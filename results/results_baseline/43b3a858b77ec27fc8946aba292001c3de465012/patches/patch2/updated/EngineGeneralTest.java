/*
 * Copyright 2014 Feedzai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.feedzai.commons.sql.abstraction.engine.impl.abs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Expression;
import com.feedzai.commons.sql.abstraction.dml.K;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.dml.With;
import com.feedzai.commons.sql.abstraction.dml.dialect.Dialect;
import com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder;
import com.feedzai.commons.sql.abstraction.dml.result.ResultColumn;
import com.feedzai.commons.sql.abstraction.dml.result.ResultIterator;
import com.feedzai.commons.sql.abstraction.engine.AbstractDatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.ConnectionResetException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineRuntimeException;
import com.feedzai.commons.sql.abstraction.exceptions.DatabaseEngineUniqueConstraintViolationException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.MappedEntity;
import com.feedzai.commons.sql.abstraction.engine.NameAlreadyExistsException;
import com.feedzai.commons.sql.abstraction.engine.OperationNotSupportedRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB;
import com.feedzai.commons.sql.abstraction.engine.testconfig.BlobTest;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseConfiguration;
import com.feedzai.commons.sql.abstraction.engine.testconfig.DatabaseTestUtil;
import com.feedzai.commons.sql.abstraction.entry.EntityEntry;
import com.google.common.collect.ImmutableSet;
import java.sql.SQLException;
import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Verifications;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint.NOT_NULL;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.BLOB;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.BOOLEAN;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.CLOB;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.DOUBLE;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.INT;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.LONG;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.STRING;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.L;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.all;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.avg;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.between;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.caseWhen;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.cast;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.ceiling;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.coalesce;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.column;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.concat;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.count;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.createView;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.dbColumn;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.dbEntity;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.dbFk;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.delete;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.div;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.dropPK;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.entry;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.eq;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.f;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.floor;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.in;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.k;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.like;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.lit;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.lower;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.max;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.min;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.mod;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.neq;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.notBetween;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.notIn;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.or;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.select;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.stddev;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.stringAgg;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.sum;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.table;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.udf;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.union;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.update;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.upper;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.values;
import static com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder.with;
import static com.feedzai.commons.sql.abstraction.engine.EngineTestUtils.buildEntity;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.ENGINE;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.JDBC;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.PASSWORD;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.SCHEMA_POLICY;
import static com.feedzai.commons.sql.abstraction.engine.configuration.PdbProperties.USERNAME;
import static com.feedzai.commons.sql.abstraction.util.StringUtils.quotize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * @author Rui Vilao (rui.vilao@feedzai.com)
 * @since 2.0.0
 */
@RunWith(Parameterized.class)
public class EngineGeneralTest {


    private static final double DELTA = 1e-7;

    protected DatabaseEngine engine;
    protected Properties properties;

    @Parameterized.Parameters
    public static Collection<DatabaseConfiguration> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations();
    }

    @Parameterized.Parameter
    public DatabaseConfiguration config;

    @BeforeClass
    public static void initStatic() {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.TRACE);
    }

    @Before
    public void init() throws DatabaseFactoryException {
        properties = new Properties() {
            {
                setProperty(JDBC, config.jdbc);
                setProperty(USERNAME, config.username);
                setProperty(PASSWORD, config.password);
                setProperty(ENGINE, config.engine);
                setProperty(SCHEMA_POLICY, "drop-create");
            }
        };

        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void cleanup() {
        engine.close();
    }

    @Test
    public void createEntityTest() throws DatabaseEngineException {

        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithTwoColumnsBeingPKTest() throws DatabaseEngineException {

        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .build();

        engine.addEntity(entity);
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityAlreadyExistsTest() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .build();

        engine.addEntity(entity);

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException e) {
            assertEquals("", "Entity 'TEST' is already defined", e.getMessage());
            throw e;
        }
    }

    @Test
    public void createUniqueIndexTest() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .addIndex(true, "COL4")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createIndexWithTwoColumnsTest() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .addIndex("COL4", "COL3")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createTwoIndexesTest() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .addIndex("COL4")
                .addIndex("COL3")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithTheSameNameButLowerCasedTest() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .build();

        engine.addEntity(entity);

        DbEntity entity2 = dbEntity()
                .name("test")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1", "COL3")
                .build();

        engine.addEntity(entity2);

    }

    @Test
    public void createEntityWithSequencesTest() throws DatabaseEngineException {

        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithIndexesTest() throws DatabaseEngineException {

        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .addIndex("COL4")
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void insertWithControlledTransactionTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS").build();

        engine.beginTransaction();

        try {

            engine.persist("TEST", entry);
            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }

        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")));

        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void insertWithAutoCommitTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")));

        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void insertWithControlledTransactionUsingSequenceTest() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry entry = entry().set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                .build();

        engine.beginTransaction();

        try {

            engine.persist("TEST", entry);
            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")));

        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 1, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void queryWithIteratorWithDataTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = entry().set("COL1", 1).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);

        ResultIterator it = engine.iterator(select(all()).from(table("TEST")));

        Map<String, ResultColumn> res;
        res = it.next();
        assertNotNull("result is not null", res);
        assertTrue("COL1 exists", res.containsKey("COL1"));
        assertEquals("COL1 ok?", 1, (int) res.get("COL1").toInt());

        assertTrue("COL2 exists", res.containsKey("COL2"));
        assertFalse("COL2 ok?", res.get("COL2").toBoolean());

        assertTrue("COL3 exists", res.containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, res.get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", res.containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) res.get("COL4").toLong());

        assertTrue("COL5 exists", res.containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", res.get("COL5").toString());

        assertNull("no more data to consume?", it.next());

        assertTrue("result set is closed?", it.isClosed());
        assertNull("next on a closed result set must return null", it.next());

        // calling close on a closed result set has no effect.
        it.close();
    }

    @Test
    public void queryWithIteratorWithNoDataTest() throws Exception {
        create5ColumnsEntity();

        ResultIterator it = engine.iterator(select(all()).from(table("TEST")));

        assertNull("result is null", it.next());

        assertNull("no more data to consume?", it.next());

        assertTrue("result set is closed?", it.isClosed());
        assertNull("next on a closed result set must return null", it.next());

        // calling close on a closed result set has no effect.
        it.close();
    }

    /**
     * Tests that an iterator created in a try-with-resources' resource specification header is automatically closed
     * once the block is exited from.
     *
     * @throws Exception If an unexpected error occurs.
     *
     * @since 2.1.12
     */
    @Test
    public void queryWithIteratorInTryWithResources() throws Exception {
        create5ColumnsEntity();

        final EntityEntry entry = entry()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);

        final ResultIterator resultIterator;
        try (final ResultIterator it = engine.iterator(select(all()).from(table("TEST")))) {

            resultIterator = it;

            assertFalse(
                    "Result iterator should not be closed before exiting try-with-resources block",
                    resultIterator.isClosed()
            );
        }

        assertTrue(
                "Result iterator should be closed after exiting try-with-resources block",
                resultIterator.isClosed()
        );
    }

    @Test
    public void batchInsertTest() throws Exception {
        create5ColumnsEntity();

        engine.beginTransaction();

        try {
            EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                    .build();

            engine.addBatch("TEST", entry);

            entry = entry().set("COL1", 3).set("COL2", true).set("COL3", 3D).set("COL4", 4L).set("COL5", "OLA")
                    .build();

            engine.addBatch("TEST", entry);

            engine.flush();

            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }

        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")).orderby(column("COL1").asc()));

        // 1st
        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());

        // 2nd

        assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        assertEquals("COL5  ok?", "OLA", query.get(1).get("COL5").toString());
    }

    @Test
    public void batchInsertAutocommitTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                .build();

        engine.addBatch("TEST", entry);

        entry = entry().set("COL1", 3).set("COL2", true).set("COL3", 3D).set("COL4", 4L).set("COL5", "OLA")
                .build();

        engine.addBatch("TEST", entry);

        // autocommit set to true.
        engine.flush();


        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")).orderby(column("COL1").asc()));

        // 1st
        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());

        // 2nd

        assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        assertEquals("COL5  ok?", "OLA", query.get(1).get("COL5").toString());
    }

    /**
     * Tests that an iterator created in a try-with-resources' resource specification header is automatically closed
     * once the block is exited from.
     *
     * @throws Exception If an unexpected error occurs.
     *
     * @since 2.1.12
     */
    @Test
    public void queryWithIteratorInTryWithResources() throws Exception {
        create5ColumnsEntity();

        final EntityEntry entry = entry()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);

        final ResultIterator resultIterator;
        try (final ResultIterator it = engine.iterator(select(all()).from(table("TEST")))) {

            resultIterator = it;

            assertFalse(
                    "Result iterator should not be closed before exiting try-with-resources block",
                    resultIterator.isClosed()
            );
        }

        assertTrue(
                "Result iterator should be closed after exiting try-with-resources block",
                resultIterator.isClosed()
        );
    }

    @Test
    public void batchInsertTest() throws Exception {
        create5ColumnsEntity();

        engine.beginTransaction();

        try {
            EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                    .build();

            engine.addBatch("TEST", entry);

            entry = entry().set("COL1", 3).set("COL2", true).set("COL3", 3D).set("COL4", 4L).set("COL5", "OLA")
                    .build();

            engine.addBatch("TEST", entry);

            engine.flush();

            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }

        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")).orderby(column("COL1").asc()));

        // 1st
        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());

        // 2nd

        assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        assertEquals("COL5  ok?", "OLA", query.get(1).get("COL5").toString());
    }

    @Test
    public void batchInsertAutocommitTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = entry().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS")
                .build();

        engine.addBatch("TEST", entry);

        entry = entry().set("COL1", 3).set("COL2", true).set("COL3", 3D).set("COL4", 4L).set("COL5", "OLA")
                .build();

        engine.addBatch("TEST", entry);

        // autocommit set to true.
        engine.flush();


        List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")).orderby(column("COL1").asc()));

        // 1st
        assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        assertEquals("COL5  ok?", "ADEUS", query.get(0).get("COL5").toString());

        // 2nd

        assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());

        assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());

        assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);

        assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());

        assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        assertEquals("COL5  ok?", "OLA", query.get(1).get("COL5").toString());
    }

    @Test
    public void insertWithNoAutoIncAndThatResumeTheAutoIncTest() throws DatabaseEngineException {
        DbEntity entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT)
                        .addColumn("COL2", BOOLEAN)
                        .addColumn("COL3", DOUBLE)
                        .addColumn("COL4", LONG, true)
                        .addColumn("COL5", STRING)
                        .build();
        engine.addEntity(entity);
        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true)
                .build());
        List<Map<String, ResultColumn>> test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 1L, (long) test.get(0).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true).set("COL4", 2)
                .build(), false);
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 2L, (long) test.get(1).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true).build());
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 3L, (long) test.get(2).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true).set("COL4", 4)
                .build(), false);
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 4L, (long) test.get(3).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true)
                .build());
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 5L, (long) test.get(4).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true).set("COL4", 6)
                .build(), false);
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 6L, (long) test.get(5).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true).set("COL4", 7)
                .build(), false);
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 7L, (long) test.get(6).get("COL4").toLong());

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true)
                .build());
        test = engine.query(select(all()).from(table("TEST")).orderby(column("COL4")));
        assertEquals("col4 ok?", 8L, (long) test.get(7).get("COL4").toLong());
    }

    /**
     * Creates a {@link DbEntity} with 5 columns to be used in the tests.
     *
     * @throws DatabaseEngineException If something goes wrong creating the entity.
     */
    private void create5ColumnsEntity() throws DatabaseEngineException {
        final DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .build();

        engine.addEntity(entity);
    }

    /**
     * Creates a {@link DbEntity} with 5 columns being the first the primary key to be used in the tests.
     *
     * @throws DatabaseEngineException If something goes wrong creating the entity.
     */
    private void create5ColumnsEntityWithPrimaryKey() throws DatabaseEngineException {
        final DbEntity entity = dbEntity().name("TEST")
                                          .addColumn("COL1", INT)
                                          .addColumn("COL2", BOOLEAN)
                                          .addColumn("COL3", DOUBLE)
                                          .addColumn("COL4", LONG)
                                          .addColumn("COL5", STRING)
                                          .pkFields("COL1")
                                          .build();

        engine.addEntity(entity);
    }

    protected void userRolePermissionSchema() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("USER")
                .addColumn("COL1", INT, true)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);

        entity = dbEntity()
                .name("ROLE")
                .addColumn("COL1", INT, true)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);

        entity = dbEntity()
                .name("USER_ROLE")
                .addColumn("COL1", INT)
                .addColumn("COL2", INT)
                .addFk(dbFk()
                                .addColumn("COL1")
                                .referencedTable("USER")
                                .addReferencedColumn("COL1")
                                .build(),
                        dbFk()
                                .addColumn("COL2")
                                .referencedTable("ROLE")
                                .addReferencedColumn("COL1")
                                .build()
                )
                .pkFields("COL1", "COL2")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void testAndWhere() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 3).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 4).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table("TEST")).where(eq(column("COL1"), k(1))).andWhere(eq(column("COL5"), k("teste"))));

        assertEquals("Resultset must have only one result", 1, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be teste", "teste", query.get(0).get("COL5").toString());
    }

    @Test
    public void testAndWhereMultiple() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 3).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 4).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(all())
                        .from(table("TEST"))
                        .where(
                                or(
                                        eq(column("COL1"), k(1)),
                                        eq(column("COL1"), k(4))
                                )
                        )
                        .andWhere(
                                or(
                                        eq(column("COL5"), k("teste")),
                                        eq(column("COL5"), k("TESTE"))
                                )
                        )
        );

        assertEquals("Resultset must have only one result", 1, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be teste", "teste", query.get(0).get("COL5").toString());
    }

    @Test
    public void testAndWhereMultipleCheckAndEnclosed() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 3).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 4).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(all())
                        .from(table("TEST"))
                        .where(
                                or(
                                        eq(column("COL1"), k(1)),
                                        eq(column("COL1"), k(4))
                                )
                        )
                        .andWhere(
                                or(
                                        eq(column("COL5"), k("teste")),
                                        eq(column("COL5"), k("tesTte"))
                                )
                        )
        );

        assertEquals("Resultset must have only one result", 2, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be teste", "teste", query.get(0).get("COL5").toString());
        assertEquals("COL1 must be 1", 4, query.get(1).get("COL1").toInt().intValue());
        assertEquals("COL5 must be teste", "tesTte", query.get(1).get("COL5").toString());
    }

    @Test
    public void testStringAgg() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(column("COL1"), stringAgg(column("COL5")).alias("agg"))
                        .from(table("TEST"))
                        .groupby(column("COL1"))
                        .orderby(column("COL1").asc())
        );

        assertEquals("Resultset must have only 2 results", 2, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be TESTE,teste", "TESTE,teste", query.get(0).get("agg").toString());
        assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        assertEquals("COL5 must be TeStE,tesTte", "TeStE,tesTte", query.get(1).get("agg").toString());
    }

    @Test
    public void testStringAggDelimiter() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(column("COL1"), stringAgg(column("COL5")).delimiter(';').alias("agg"))
                        .from(table("TEST"))
                        .groupby(column("COL1"))
                        .orderby(column("COL1").asc())
        );

        assertEquals("Resultset must have only 2 results", 2, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be TESTE;teste", "TESTE;teste", query.get(0).get("agg").toString());
        assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        assertEquals("COL5 must be TeStE;tesTte", "TeStE;tesTte", query.get(1).get("agg").toString());
    }

    @Test
    public void testStringAggDistinct() throws DatabaseEngineException {
        assumeTrue("This test is only valid for engines that support StringAggDistinct",
                this.engine.isStringAggDistinctCapable());

        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(column("COL1"), stringAgg(column("COL5")).distinct().alias("agg"))
                        .from(table("TEST"))
                        .groupby(column("COL1"))
                        .orderby(column("COL1").asc())
        );

        assertEquals("Resultset must have only 2 results", 2, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be teste", "teste", query.get(0).get("agg").toString());
        assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        assertEquals("COL5 must be TeStE,tesTte", "TeStE,tesTte", query.get(1).get("agg").toString());
    }

    @Test
    public void testStringAggNotStrings() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 1).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 2).set("COL5", "tesTte")
                .build());

        final List<Map<String, ResultColumn>> query = engine.query(
                select(column("COL1"), stringAgg(column("COL1")).alias("agg"))
                        .from(table("TEST"))
                        .groupby(column("COL1"))
                        .orderby(column("COL1").asc())
        );

        assertEquals("Resultset must have only 2 results", 2, query.size());
        assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        assertEquals("COL5 must be 1,1", "1,1", query.get(0).get("agg").toString());
        assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        assertEquals("COL5 must be 2,2", "2,2", query.get(1).get("agg").toString());
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithOneColumnTest() throws Exception {
        DbEntity entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT)
                        .addColumn("COL2", BOOLEAN)
                        .addColumn("COL3", DOUBLE)
                        .addColumn("COL4", LONG)
                        .addColumn("COL5", STRING)
                        .pkFields("COL1")
                        .build();
        engine.addEntity(entity);
        engine.executeUpdate(dropPK(table("TEST")));
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithTwoColumnsTest() throws Exception {
        DbEntity entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT)
                        .addColumn("COL2", BOOLEAN)
                        .addColumn("COL3", DOUBLE)
                        .addColumn("COL4", LONG)
                        .addColumn("COL5", STRING)
                        .pkFields("COL1", "COL4")
                        .build();
        engine.addEntity(entity);
        engine.executeUpdate(dropPK(table("TEST")));
    }

    @Test
    public void alterColumnWithConstraintTest() throws DatabaseEngineException {
        DbEntity entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT)
                        .addColumn("COL2", BOOLEAN)
                        .addColumn("COL3", DOUBLE)
                        .addColumn("COL4", LONG)
                        .addColumn("COL5", STRING)
                        .build();

        engine.addEntity(entity);

        engine.executeUpdate(new AlterColumn(table("TEST"), new DbColumn.Builder().name("COL1").type(DbColumnType.INT).addConstraint(DbColumnConstraint
                .NOT_NULL)
                .build()));
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void alterColumnToDifferentTypeTest() throws DatabaseEngineException {
        DbEntity entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT)
                        .addColumn("COL2", BOOLEAN)
                        .addColumn("COL3", DOUBLE)
                        .addColumn("COL4", LONG)
                        .addColumn("COL5", STRING)
                        .build();

        engine.addEntity(entity);

        engine.executeUpdate(new AlterColumn(table("TEST"), dbColumn().name("COL1").type(DbColumnType.STRING)
                .build()));
    }

    @Test
    public void createTableWithDefaultsTest() throws DatabaseEngineException, DatabaseFactoryException {
        DbEntity.Builder entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT, new K(1))
                        .addColumn("COL2", BOOLEAN, new K(false))
                        .addColumn("COL3", DOUBLE, new K(2.2d))
                        .addColumn("COL4", LONG, new K(3L))
                        .pkFields("COL1");

        engine.addEntity(entity.build());

        final String ec = engine.escapeCharacter();
        engine.executeUpdate("INSERT INTO " + quotize("TEST", ec) + " (" + quotize("COL1", ec) + ") VALUES (10)");

        List<Map<String, ResultColumn>> test = engine.query(select(all()).from(table("TEST")));
        assertEquals("Check size of records", 1, test.size());
        Map<String, ResultColumn> record = test.get(0);
        assertEquals("Check COL1", 10, record.get("COL1").toInt().intValue());
        assertEquals("Check COL2", false, record.get("COL2").toBoolean());
        assertEquals("Check COL3", 2.2d, record.get("COL3").toDouble(), 0);
        assertEquals("Check COL4", 3L, record.get("COL4").toLong().longValue());


        final DbEntity entity1 = entity
                .addColumn("COL5", STRING, new K("mantorras"), NOT_NULL)
                .addColumn("COL6", BOOLEAN, new K(true), NOT_NULL)
                .addColumn("COL7", INT, new K(7), NOT_NULL)
                .build();

        final Properties propertiesCreate = new Properties();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            propertiesCreate.setProperty(prop.getKey().toString(), prop.getValue().toString());
        }
        propertiesCreate.setProperty(SCHEMA_POLICY, "create");

        final DatabaseEngine connection2 = DatabaseFactory.getConnection(propertiesCreate);
        connection2.updateEntity(entity1);

        test = connection2.query(select(all()).from(table("TEST")));
        assertEquals("Check size of records", 1, test.size());
        record = test.get(0);
        assertEquals("Check COL1", 10, record.get("COL1").toInt().intValue());
        assertEquals("Check COL2", false, record.get("COL2").toBoolean());
        assertEquals("Check COL3", 2.2d, record.get("COL3").toDouble(), 1e-9);
        assertEquals("Check COL4", 3L, record.get("COL4").toLong().longValue());
        assertEquals("Check COL5", "mantorras", record.get("COL5").toString());
        assertEquals("Check COL6", true, record.get("COL6").toBoolean());
        assertEquals("Check COL7", 7, record.get("COL7").toInt().intValue());
        connection2.close();
    }

    @Test
    public void defaultValueOnBooleanColumnsTest() throws DatabaseEngineException {
        DbEntity.Builder entity =
                dbEntity()
                        .name("TEST")
                        .addColumn("COL1", INT, new K(1))
                        .addColumn("COL2", BOOLEAN, new K(false), NOT_NULL)
                        .addColumn("COL3", DOUBLE, new K(2.2d))
                        .addColumn("COL4", LONG, new K(3L))
                        .pkFields("COL1");

        engine.addEntity(entity.build());

        engine.persist("TEST", entry().build());
        Map<String, ResultColumn> row = engine.query(select(all()).from(table("TEST"))).get(0);

        assertEquals("", 1, row.get("COL1").toInt().intValue());
        assertFalse("", row.get("COL2").toBoolean());
        assertEquals("", 2.2d, row.get("COL3").toDouble(), 0D);
        assertEquals("", 3L, row.get("COL4").toLong().longValue());
    }

    @Test
    public void upperTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", entry().set("COL5", "ola").build());
        assertEquals("text is uppercase", "OLA", engine.query(select(upper(column("COL5")).alias("RES")).from(table("TEST"))).get(0).get("RES").toString());
    }

    @Test
    public void lowerTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", entry().set("COL5", "OLA").build());
        assertEquals("text is lowercase", "ola", engine.query(select(lower(column("COL5")).alias("RES")).from(table("TEST"))).get(0).get("RES").toString());
    }

    @Test
    public void internalFunctionTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", entry().set("COL5", "OLA").build());
        assertEquals("text is uppercase", "ola", engine.query(select(f("LOWER", column("COL5")).alias("RES")).from(table("TEST"))).get(0).get("RES")
                .toString());
    }

    @Test
    public void entityEntryHashcodeTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("id1", "val1");
        map.put("id2", "val2");
        map.put("id3", "val3");
        map.put("id4", "val4");

        EntityEntry entry = entry()
                .set(map)
                .build();

        assertEquals("entry's hashCode() matches map's hashCode()", map.hashCode(), entry.hashCode());
    }

    /**
     * Tests that creating a {@link DatabaseEngine} using try-with-resources will close the engine
     * (and thus the underlying connection to the database) once the block is exited from.
     *
     * @throws Exception if something goes wrong while checking if the connection of the engine is closed.
     * @since 2.1.12
     */
    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        final AtomicReference<Connection> connReference = new AtomicReference<>();

        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            assertFalse("close() method should not be called within the try-with-resources block, for an existing DatabaseEngine",
                    connReference.get().isClosed());
        }

        assertTrue("close() method should be called after exiting try-with-resources block, for an existing DatabaseEngine",
                connReference.get().isClosed());

        try (final DatabaseEngine tryEngine = DatabaseFactory.getConnection(properties)) {
            connReference.set(tryEngine.getConnection());
            assertFalse("close() method should not be called within the try-with-resources block, for a DatabaseEngine created in the block",
                    connReference.get().isClosed());
        }

        assertTrue("close() method should be called after exiting try-with-resources block, for a DatabaseEngine created in the block",
                connReference.get().isClosed());

    }

    /**
     * Test that closing a database engine a 'create-drop' policy with multiple entities closes all insert statements
     * associated with each entity, regardless of the schema policy used.
     *
     * Each entity is associated with 3 prepared statements. This test ensures that 3 PSs per entity are closed.
     *
     * @throws DatabaseEngineException  If something goes wrong while adding an entity to the engine.
     * @throws DatabaseFactoryException If the database engine class specified in the properties does not exist.
     * @since 2.1.13
     */
    @Test
    public void closingAnEngineUsingTheCreateDropPolicyShouldDropAllEntities()
            throws DatabaseEngineException, DatabaseFactoryException {

        // Force the schema policy to be 'create-drop'
        properties.setProperty(SCHEMA_POLICY, "create-drop");
        engine = DatabaseFactory.getConnection(properties);

        engine.addEntity(buildEntity("ENTITY-1"));
        engine.addEntity(buildEntity("ENTITY-2"));

        // Force invocation counting to start here
        new Expectations(engine) {};

        engine.close();

        new Verifications() {{
            engine.dropEntity((DbEntity) any); times = 2;
        }};

    }

    /**
     * Assesses whether the current row count is incremented if the .next()/.nextResult()
     * methods are called in the iterator.
     *
     * @throws DatabaseEngineException If a database access error happens.
     */
    @Test
    public void doesRowCountIncrementTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        // Create 4 entries
        for (int i = 0; i < 4; i++) {
            engine.persist("TEST", entry().set("COL1", i).build());
        }

        final ResultIterator resultIterator = engine.iterator(select(all()).from(table("TEST")));

        assertEquals("The current row count should be 0 if the iteration hasn't started", 0, resultIterator.getCurrentRowCount());

        // If the .next() method is called once then the current row count should be updated to 1
        resultIterator.next();

        assertEquals("The current row count is equal to 1", 1,resultIterator.getCurrentRowCount());

        // If for the same iterator the .nextResult() method is called 3 additional
        // times then the current row count should be updated to 4
        for(int i = 0; i < 3; i++) {
            resultIterator.nextResult();
        }

        assertEquals("The current row count is equal to 4", 4, resultIterator.getCurrentRowCount());
    }

    /**
     * Tests that a {@link com.feedzai.commons.sql.abstraction.dml.K constant expression} with an enum value behaves
     * as if the enum is a string (obtained from {@link Enum#name()}, both when persisting an entry and when using
     * the enum value for filtering in a WHERE clause.
     *
     * @throws DatabaseEngineException If something goes wrong creating the test entity or persisting entries.
     */
    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        // should fail here if enum is not supported, or it will just put garbage, which will be detected later
        engine.persist("TEST", entry().set("COL5", TestEnum.TEST_ENUM_VAL).build());

        engine.persist("TEST", entry().set("COL5", "something else").build());

        final List<Map<String, ResultColumn>> results = engine.query(
                select(all())
                        .from(table("TEST"))
                        .where(eq(column("COL5"), k(TestEnum.TEST_ENUM_VAL)))
        );

        assertThat(results)
                .as("One (and only one) result expected.")
                .hasSize(1)
                .element(0)
                .as("An enum value should be persisted as its string representation")
                .extracting(element -> element.get("COL5").toString())
                .isEqualTo(TestEnum.TEST_ENUM_VAL.name());
    }

    /**
     * Tests that when inserting duplicated entries in a table the right exception is returned.
     *
     * The steps performed on this test are:
     * <ol>
     *     <li>Add duplicated entries in a transaction and fail to persist</li>
     *     <li>Ensure the exception is a {@link DatabaseEngineUniqueConstraintViolationException}</li>
     * </ol>
     *
     * @throws DatabaseEngineException If there is a problem on {@link DatabaseEngine} operations.
     */
    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = entry().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        // Add the same entry twice (repeated value for COL1, id)
        engine.persist("TEST", entry);
        assertThatCode(() -> engine.persist("TEST", entry))
                .as("Is unique constraint violation exception")
                .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
                .as("Encapsulated exception is SQLException")
                .hasCauseInstanceOf(SQLException.class)
                .hasMessage("Something went wrong persisting the entity [unique_constraint_violation]");
    }

    /**
     * Tests that on a duplicated batch entry situation the right exception is returned.
     *
     * The steps performed on this test are:
     * <ol>
     *     <li>Add duplicated batch entries to transaction and fail to flush</li>
     *     <li>Ensure the exception is a {@link DatabaseEngineUniqueConstraintViolationException}</li>
     * </ol>
     *
     * @throws DatabaseEngineException If there is a problem on {@link DatabaseEngine} operations.
     */
    @Test
    public void batchInsertDuplicateDBError() throws DatabaseEngineException {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = entry().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        // Add the same entry twice (repeated value for COL1, id)
        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);

        // Flush the duplicated entries and check the exception
        assertThatCode(() -> engine.flush())
                .as("Is unique constraint violation exception")
                .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
                .as("Encapsulated exception is SQLException")
                .hasCauseInstanceOf(SQLException.class)
                .hasMessage("Something went wrong while flushing [unique_constraint_violation]");
    }

    /**
     * An enum for tests.
     */
    private enum TestEnum {
        TEST_ENUM_VAL;

        @Override
        public String toString() {
            return super.toString() + " description";
        }
    }
}