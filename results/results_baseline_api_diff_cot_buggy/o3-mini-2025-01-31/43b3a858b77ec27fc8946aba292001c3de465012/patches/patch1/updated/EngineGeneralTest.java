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
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.DbFk;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.dml.With;
import com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder;
import com.feedzai.commons.sql.abstraction.dml.result.ResultColumn;
import com.feedzai.commons.sql.abstraction.dml.result.ResultIterator;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.MappedEntity;
import com.feedzai.commons.sql.abstraction.engine.NameAlreadyExistsException;
import com.feedzai.commons.sql.abstraction.engine.OperationNotSupportedRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.test.BlobTest;
import com.feedzai.commons.sql.abstraction.engine.test.DatabaseTestConfig;
import com.feedzai.commons.sql.abstraction.engine.test.DatabaseTestUtil;
import com.feedzai.commons.sql.abstraction.entry.EntityEntry;
import com.feedzai.commons.sql.abstraction.entry.EntityEntryBuilder;
import com.feedzai.commons.sql.abstraction.exceptions.DatabaseEngineUniqueConstraintViolationException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
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

/**
 * @author Feedzai
 */
@RunWith(Parameterized.class)
public class EngineGeneralTest {

    protected DatabaseEngine engine;
    protected Properties properties;

    @Parameterized.Parameters
    public static Collection<DatabaseTestConfig> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations();
    }

    @Parameterized.Parameter
    public DatabaseTestConfig config;

    @BeforeClass
    public static void initStatic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.TRACE);
    }

    @Before
    public void init() throws DatabaseFactoryException {
        properties = new Properties();
        properties.put("JDBC", config.jdbcUrl);
        properties.put("USERNAME", config.username);
        properties.put("PASSWORD", config.password);
        properties.put("ENGINE", config.engineClassName);
        properties.put("SCHEMA_POLICY", config.schemaPolicy);
        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void cleanup() {
        engine.close();
    }

    @Test
    public void createEntityTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_CREATE_ENTITY")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("NAME", DbColumnType.STRING)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
    }

    @Test
    public void persistEntityTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_PERSIST_ENTITY")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("VALUE", DbColumnType.DOUBLE)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        final EntityEntry entry = EntityEntry.builder().set("VALUE", 3.14).build();
        engine.persist("TEST_PERSIST_ENTITY", entry);
    }

    @Test(expected = DatabaseEngineException.class)
    public void duplicateEntityTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_DUPLICATE")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("DESC", DbColumnType.STRING)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        // This should throw an exception since the entity is already added
        engine.addEntity(p);
    }

    @Test
    public void updateEntityTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_UPDATE")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("STATUS", DbColumnType.BOOLEAN)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        final EntityEntry entry = EntityEntry.builder().set("STATUS", false).build();
        engine.persist("TEST_UPDATE", entry);
        engine.executeUpdate(
            Update.builder()
                  .table(SqlBuilder.table("TEST_UPDATE"))
                  .set(SqlBuilder.eq(SqlBuilder.column("STATUS"), SqlBuilder.k(true)))
                  .build()
        );
    }

    @Test
    public void deleteEntityTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_DELETE")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("AMOUNT", DbColumnType.DOUBLE)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        final EntityEntry entry = EntityEntry.builder().set("AMOUNT", 100.0).build();
        engine.persist("TEST_DELETE", entry);
        engine.executeUpdate(SqlBuilder.delete(SqlBuilder.table("TEST_DELETE")));
    }

    @Test
    public void queryTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_QUERY")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("DESCRIPTION", DbColumnType.STRING)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        engine.persist("TEST_QUERY", EntityEntry.builder().set("DESCRIPTION", "Hello World").build());
        List<Map<String, ResultColumn>> results = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                          .from(SqlBuilder.table("TEST_QUERY")));
        if (results.isEmpty()) {
            throw new DatabaseEngineRuntimeException("Query returned no results");
        }
    }

    @Test
    public void limitTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_LIMIT")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("COUNT", DbColumnType.INT)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        for (int i = 0; i < 10; i++) {
            engine.persist("TEST_LIMIT", EntityEntry.builder().set("COUNT", i).build());
        }
        List<Map<String, ResultColumn>> results = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                          .from(SqlBuilder.table("TEST_LIMIT"))
                                                                          .limit(5));
        if (results.size() != 5) {
            throw new DatabaseEngineRuntimeException("Expected 5 results but got " + results.size());
        }
    }

    @Test
    public void batchInsertTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_BATCH")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("NAME", DbColumnType.STRING)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        engine.beginTransaction();
        try {
            engine.addBatch("TEST_BATCH", EntityEntry.builder().set("NAME", "batch1").build());
            engine.addBatch("TEST_BATCH", EntityEntry.builder().set("NAME", "batch2").build());
            engine.flush();
            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
    }

    @Test
    public void getMetadataTest() throws DatabaseEngineException {
        final DbEntity p = DbEntity.builder().name("TEST_METADATA")
                                   .addColumn("ID", DbColumnType.INT, true)
                                   .addColumn("DATA", DbColumnType.STRING)
                                   .pkFields("ID")
                                   .build();
        engine.addEntity(p);
        Map<String, DbColumnType> meta = engine.getMetadata("TEST_METADATA");
        if (!meta.containsKey("ID") || !meta.containsKey("DATA")) {
            throw new DatabaseEngineRuntimeException("Metadata does not contain expected columns");
        }
    }

    @Test(expected = DatabaseEngineUniqueConstraintViolationException.class)
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 2)
                                       .set("COL2", false)
                                       .set("COL3", 2D)
                                       .set("COL4", 3L)
                                       .set("COL5", "ADEUS")
                                       .build();

        // Add the same entry twice (repeated value for COL1, id)
        engine.persist("TEST", entry);
        engine.persist("TEST", entry);
    }

    @Test
    public void batchInsertDuplicateDBError() throws DatabaseEngineException {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 2)
                                       .set("COL2", false)
                                       .set("COL3", 2D)
                                       .set("COL4", 3L)
                                       .set("COL5", "ADEUS")
                                       .build();

        // Add the same entry twice (repeated value for COL1, id)
        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);

        try {
            engine.flush();
        } catch (DatabaseEngineException e) {
            if (!(e.getCause() instanceof SQLException)) {
                throw e;
            }
            throw e;
        }
    }

    @Test
    public void blobTest() throws DatabaseEngineException {
        final double[] original = new double[]{5, 6, 7};
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);
        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", original)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));

        int i = 0;
        for (double d : original) {
            if (d != query.get(0).get("COL2").<double[]>toBlob()[i++]) {
                throw new DatabaseEngineRuntimeException("Blob arrays not equal");
            }
        }
    }

    @Test
    public void limitAndOffsetNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }

        int limit = 5;
        int offset = 7;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .limit(limit)
                                                                        .offset(offset));
        if (query.size() != limit) {
            throw new DatabaseEngineRuntimeException("Row number not matching expected limit");
        }
        for (int i = offset, j = 0; i < offset + limit; i++, j++) {
            if (i != query.get(j).get("COL1").toInt().intValue()) {
                throw new DatabaseEngineRuntimeException("Row does not match expected value");
            }
        }
    }

    @Test
    public void limitOffsetAndOrderNumberOfRowsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 20);

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            entry.set("COL6", 20 - i);
            engine.persist("TEST", entry.build());
        }

        int limit = 5;
        int offset = 7;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .limit(limit)
                                                                        .offset(offset)
                                                                        .orderby(SqlBuilder.column("COL6").asc()));
        if (query.size() != limit) {
            throw new DatabaseEngineRuntimeException("Row number not matching expected limit");
        }
        for (int i = offset, j = 0; i < offset + limit; i++, j++) {
            if ((query.get(j).get("COL1").toInt().intValue() != 19 - i) || 
                (query.get(j).get("COL6").toInt().intValue() != i + 1)) {
                throw new DatabaseEngineRuntimeException("Ordered row does not match expected values");
            }
        }
    }

    @Test
    public void offsetLessThanZero() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 20);

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            entry.set("COL6", 20 - i);
            engine.persist("TEST", entry.build());
        }

        int limit = 5;
        int offset = -1;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .limit(limit)
                                                                        .offset(offset)
                                                                        .orderby(SqlBuilder.column("COL6").asc()));
        if (query.size() != limit) {
            throw new DatabaseEngineRuntimeException("Row number not matching expected limit");
        }
        for (int i = 0, j = 0; i < 5; i++, j++) {
            if ((query.get(j).get("COL1").toInt().intValue() != 19 - i) ||
                (query.get(j).get("COL6").toInt().intValue() != i + 1)) {
                throw new DatabaseEngineRuntimeException("Row values not matching expected when offset is negative");
            }
        }
    }

    @Test
    public void offsetBiggerThanSize() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 20);

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            entry.set("COL6", 20 - i);
            engine.persist("TEST", entry.build());
        }

        int limit = 5;
        int offset = 20;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .limit(limit)
                                                                        .offset(offset));
        if (query.size() != 0) {
            throw new DatabaseEngineRuntimeException("Expected no results when offset is equal to table size");
        }
    }

    @Test
    public void limitZeroOrNegative() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 20);

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            entry.set("COL6", 20 - i);
            engine.persist("TEST", entry.build());
        }

        int limit = 0;
        int offset = 1;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .limit(limit)
                                                                        .offset(offset));
        if (query.size() != 19) {
            throw new DatabaseEngineRuntimeException("When limit is zero, expected table length minus offset");
        }

        limit = -1;
        query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                       .from(SqlBuilder.table("TEST"))
                                       .limit(limit)
                                       .offset(offset));
        if (query.size() != 19) {
            throw new DatabaseEngineRuntimeException("When limit is negative, expected table length minus offset");
        }
    }

    @Test
    public void offsetOnlyNumberOfRowsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 2);

        for (int i = 0; i < 20; i++) {
            entry.set("COL1", i);
            entry.set("COL6", 20 - i);
            engine.persist("TEST", entry.build());
        }

        int offset = 7;
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                        .from(SqlBuilder.table("TEST"))
                                                                        .offset(offset));
        if (query.size() != 20 - offset) {
            throw new DatabaseEngineRuntimeException("Offset query returned unexpected number of rows");
        }
        for (int i = offset, j = 0; i < 20; i++, j++) {
            if (i != query.get(j).get("COL1").toInt().intValue()) {
                throw new DatabaseEngineRuntimeException("Offset row check failed");
            }
        }
    }

    @Test
    public void stddevTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.stddev(SqlBuilder.column("COL1")).alias("STDDEV"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (Math.abs(query.get(0).get("STDDEV").toDouble() - 3.0276503540974917D) > 0.0001D) {
            throw new DatabaseEngineRuntimeException("STDDEV value not within expected range");
        }
    }

    @Test
    public void sumTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.sum(SqlBuilder.column("COL1")).alias("SUM"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (query.get(0).get("SUM").toInt() != 45) {
            throw new DatabaseEngineRuntimeException("SUM value incorrect");
        }
    }

    @Test
    public void countTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.count(SqlBuilder.column("COL1")).alias("COUNT"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (query.get(0).get("COUNT").toInt() != 10) {
            throw new DatabaseEngineRuntimeException("COUNT value incorrect");
        }
    }

    @Test
    public void avgTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.avg(SqlBuilder.column("COL1")).alias("AVG"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (Math.abs(query.get(0).get("AVG").toDouble() - 4.5D) > 0.0) {
            throw new DatabaseEngineRuntimeException("AVG value incorrect");
        }
    }

    @Test
    public void maxTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.max(SqlBuilder.column("COL1")).alias("MAX"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (query.get(0).get("MAX").toInt() != 9) {
            throw new DatabaseEngineRuntimeException("MAX value incorrect");
        }
    }

    @Test
    public void minTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.min(SqlBuilder.column("COL1")).alias("MIN"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (query.get(0).get("MIN").toInt() != 0) {
            throw new DatabaseEngineRuntimeException("MIN value incorrect");
        }
    }

    @Test
    public void floorTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2.5D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }

        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.floor(SqlBuilder.column("COL3")).alias("FLOOR"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (Math.abs(query.get(0).get("FLOOR").toDouble() - 2.0) > 1e-7) {
            throw new DatabaseEngineRuntimeException("FLOOR value incorrect");
        }
    }

    @Test
    public void ceilingTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2.5D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");

        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }

        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.ceiling(SqlBuilder.column("COL3")).alias("CEILING"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (Math.abs(query.get(0).get("CEILING").toDouble() - 3.0) > 1e-7) {
            throw new DatabaseEngineRuntimeException("CEILING value incorrect");
        }
    }

    @Test
    public void twoIntegerDivisionMustReturnADoubleTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry.Builder ee = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", 2);

        engine.persist("TEST", ee.build());

        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.div(SqlBuilder.column("COL1"), SqlBuilder.column("COL2")).alias("DIV"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (Math.abs(query.get(0).get("DIV").toDouble() - 0.5D) > 0.0) {
            throw new DatabaseEngineRuntimeException("Division result incorrect");
        }
    }

    @Test
    public void selectWithoutFromTest() throws DatabaseEngineException {
        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.k(1).alias("constant")));

        if (query.get(0).get("constant").toInt() != 1) {
            throw new DatabaseEngineRuntimeException("Select without from returned incorrect constant");
        }
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityWithNullNameTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name(null)
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException de) {
            if (!"You have to define the entity name".equals(de.getMessage())) {
                throw de;
            }
            throw de;
        }
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityWithNoNameTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException de) {
            if (!"You have to define the entity name".equals(de.getMessage())) {
                throw de;
            }
            throw de;
        }
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityWithNameThatExceedsTheMaximumAllowedTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("0123456789012345678901234567891")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException de) {
            if (!("Entity '0123456789012345678901234567891' exceeds the maximum number of characters (30)".equals(de.getMessage()))) {
                throw de;
            }
            throw de;
        }
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityWithColumnThatDoesNotHaveNameTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("entname")
                .addColumn("", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException de) {
            if (!("Column in entity 'entname' must have a name".equals(de.getMessage()))) {
                throw de;
            }
            throw de;
        }
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityWithMoreThanOneAutoIncColumn() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("entname")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.INT, true)
                .build();

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException de) {
            if (!("You can only define one auto incremented column".equals(de.getMessage()))) {
                throw de;
            }
            throw de;
        }
    }

    @Test
    public void getGeneratedKeysFromAutoIncTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry ee = EntityEntry.builder()
                .set("COL2", 2)
                .build();

        Long persist = engine.persist("TEST", ee);

        if (!new Long(1).equals(persist)) {
            throw new DatabaseEngineRuntimeException("Generated key not as expected");
        }
    }

    @Test
    public void getGeneratedKeysFromAutoInc2Test() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        EntityEntry ee = EntityEntry.builder()
                .set("COL2", 2)
                .build();

        Long persist = engine.persist("TEST", ee);

        if (!new Long(1).equals(persist)) {
            throw new DatabaseEngineRuntimeException("Generated key not as expected");
        }

        ee = EntityEntry.builder()
                .set("COL2", 2)
                .build();

        persist = engine.persist("TEST", ee);

        if (!new Long(2).equals(persist)) {
            throw new DatabaseEngineRuntimeException("Generated key not as expected");
        }
    }

    @Test
    public void getGeneratedKeysFromAutoIncWithTransactionTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        engine.beginTransaction();

        try {
            EntityEntry ee = EntityEntry.builder()
                    .set("COL2", 2)
                    .build();

            Long persist = engine.persist("TEST", ee);

            if (!new Long(1).equals(persist)) {
                throw new DatabaseEngineRuntimeException("Generated key not as expected");
            }

            ee = EntityEntry.builder()
                    .set("COL2", 2)
                    .build();

            persist = engine.persist("TEST", ee);

            if (!new Long(2).equals(persist)) {
                throw new DatabaseEngineRuntimeException("Generated key not as expected");
            }

            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
    }

    @Test
    public void getGeneratedKeysWithNoAutoIncTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.STRING)
            .addColumn("COL2", DbColumnType.STRING)
            .pkFields("COL1", "COL2")
            .build();

        engine.addEntity(entity);

        final EntityEntry ee = EntityEntry.builder()
                .set("COL1", "VAL1")
                .set("COL2", "VAL2")
                .build();

        if (engine.persist("TEST", ee) != null) {
            throw new DatabaseEngineRuntimeException("Expected null generated key for non-autoIncrement entity");
        }
    }

    @Test
    public void addMultipleAutoIncColumnsTest() {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT, true)
            .addColumn("COL2", DbColumnType.INT, true)
            .build();

        try {
            engine.addEntity(entity);
            throw new DatabaseEngineRuntimeException("Expected exception on multiple auto-increment columns");
        } catch (DatabaseEngineException ex) {
            // Expected exception
        }
    }

    @Test
    public void abortTransactionTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .build();

        engine.addEntity(entity);

        engine.beginTransaction();
        try {
            EntityEntry ee = EntityEntry.builder()
                    .set("COL1", 1)
                    .set("COL2", 2)
                    .build();

            engine.persist("TEST", ee);

            throw new Exception();
        } catch (final Exception e) {
            // ignore
        } finally {
            if (!engine.isTransactionActive()) {
                throw new DatabaseEngineRuntimeException("Transaction should be active");
            }
            engine.rollback();

            if (engine.isTransactionActive()) {
                throw new DatabaseEngineRuntimeException("Transaction should have been rolled back");
            }

            if (engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))).size() != 0) {
                throw new DatabaseEngineRuntimeException("Table should be empty after rollback");
            }
        }
    }

    @Test
    public void createEntityDropItAndCreateItAgainTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("USER")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);
        DbEntity removeEntity = engine.removeEntity("USER");

        if (removeEntity == null) {
            throw new DatabaseEngineRuntimeException("Entity removal failed");
        }

        engine.addEntity(entity);
    }

    @Test
    public void dropEntityThatDoesNotExistTest() {
        DbEntity removeEntity = engine.removeEntity("TABLETHATDOESNOTEXIST");

        if (removeEntity != null) {
            throw new DatabaseEngineRuntimeException("Expected null when removing non-existent entity");
        }
    }

    @Test
    public void joinsTest() throws DatabaseEngineException {
        userRolePermissionSchema();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.table("USER").alias("a").innerJoin(SqlBuilder.table("USER_ROLE").alias("b"), SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1")))
                        )
        );

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.table("USER").alias("a")
                                        .innerJoin(SqlBuilder.table("USER_ROLE").alias("b"), SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1")))
                                        .innerJoin(SqlBuilder.table("ROLE").alias("c"), SqlBuilder.eq(SqlBuilder.column("b", "COL2"), SqlBuilder.column("c", "COL1")))
                        )
        );

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.table("USER").alias("a").rightOuterJoin(SqlBuilder.table("USER_ROLE").alias("b"), SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1")))
                        )
        );

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.table("USER").alias("a").leftOuterJoin(SqlBuilder.table("USER_ROLE").alias("b"), SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1")))
                        )
        );
    }

    @Test
    public void joinATableWithQueryTest() throws DatabaseEngineException {
        userRolePermissionSchema();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.table("USER").alias("a")
                                        .innerJoin(
                                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                                        .from(SqlBuilder.table("USER")).alias("b")
                                                , SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1"))
                                        )
                        )
        );
    }

    @Test
    public void joinAQueryWithATableTest() throws DatabaseEngineException {
        userRolePermissionSchema();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                        .from(SqlBuilder.table("USER")).alias("b")
                                        .innerJoin(
                                                SqlBuilder.table("USER").alias("a")
                                                , SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1"))
                                        )
                        )
        );
    }

    @Test
    public void joinTwoQueriesTest() throws DatabaseEngineException {
        userRolePermissionSchema();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                        .from(SqlBuilder.table("USER")).alias("a")
                                        .innerJoin(
                                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                                        .from(SqlBuilder.table("USER")).alias("b")
                                                , SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1"))
                                        )
                        )
        );
    }

    @Test
    public void joinThreeQueriesTest() throws DatabaseEngineException {
        userRolePermissionSchema();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(
                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                        .from(SqlBuilder.table("USER")).alias("a")
                                        .innerJoin(
                                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                                        .from(SqlBuilder.table("USER")).alias("b")
                                                , SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("b", "COL1"))
                                        )
                                        .rightOuterJoin(
                                                SqlBuilder.select(SqlBuilder.column("COL1"))
                                                        .from(SqlBuilder.table("USER")).alias("c")
                                                , SqlBuilder.eq(SqlBuilder.column("a", "COL1"), SqlBuilder.column("c", "COL1"))
                                        )
                        )
        );
    }

    @Test
    @Category(com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB.class)
    public void createAndDropViewTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.executeUpdate(
                SqlBuilder.createView("VN").as(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")))
        );

        engine.dropView("VN");
    }

    @Test
    @Category(com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB.class)
    public void createOrReplaceViewTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.executeUpdate(
                SqlBuilder.createView("VN").as(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))).replace()
        );

        engine.dropView("VN");
    }

    @Test
    public void distinctTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all()).distinct()
                        .from(SqlBuilder.table("TEST"))
        );
    }

    @Test
    public void distinctAndLimitTogetherTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all()).distinct()
                        .from(SqlBuilder.table("TEST")).limit(2)
        );
    }

    @Test
    public void notEqualTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.neq(SqlBuilder.column("COL1"), SqlBuilder.k(1)))
        );
    }

    @Test
    public void inTest() throws DatabaseEngineException {
        runInClauseTest(SqlBuilder.in(SqlBuilder.column("COL1"), SqlBuilder.L(SqlBuilder.k(1))));
    }

    @Test
    public void inSelectTest() throws DatabaseEngineException {
        runInClauseTest(SqlBuilder.in(
                SqlBuilder.column("COL1"),
                SqlBuilder.select(SqlBuilder.column("COL1")).from(SqlBuilder.table("TEST")).where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)))
        ));
    }

    @Test
    public void inManyValuesTest() throws DatabaseEngineException {
        final List<SqlBuilder.Expression> numExprs = IntStream.rangeClosed(-19998, 1)
                .mapToObj(SqlBuilder::k)
                .collect(Collectors.toList());

        runInClauseTest(SqlBuilder.in(SqlBuilder.column("COL1"), SqlBuilder.L(numExprs)));
    }

    @Test
    public void notInTest() throws DatabaseEngineException {
        runInClauseTest(SqlBuilder.notIn(SqlBuilder.column("COL1"), SqlBuilder.L(SqlBuilder.k(2))));
    }

    @Test
    public void notInSelectTest() throws DatabaseEngineException {
        runInClauseTest(SqlBuilder.notIn(
                SqlBuilder.column("COL1"),
                SqlBuilder.select(SqlBuilder.column("COL1")).from(SqlBuilder.table("TEST")).where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(2)))
        ));
    }

    @Test
    public void notInManyValuesTest() throws DatabaseEngineException {
        final List<SqlBuilder.Expression> numExprs = IntStream.rangeClosed(2, 20001)
                .mapToObj(SqlBuilder::k)
                .collect(Collectors.toList());

        runInClauseTest(SqlBuilder.notIn(SqlBuilder.column("COL1"), SqlBuilder.L(numExprs)));
    }

    private void runInClauseTest(final SqlBuilder.Expression whereInExpression) throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "s1").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "s2").build());

        final List<Map<String, ResultColumn>> results = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(whereInExpression)
        );

        if (results.size() != 1 || results.get(0).get("COL1").toInt() != 1) {
            throw new DatabaseEngineRuntimeException("IN clause did not return expected result");
        }
    }

    @Test
    public void booleanTrueComparisonTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry entry1 = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", true)
                .set("COL3", 1)
                .set("COL4", 1)
                .set("COL5", "val 1")
                .build();
        engine.persist("TEST", entry1, false);

        EntityEntry entry2 = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 1)
                .set("COL4", 1)
                .set("COL5", "val 1")
                .build();
        engine.persist("TEST", entry2, false);

        List<Map<String, ResultColumn>> rows = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.k(true)))
        );

        if (rows.size() != 1) {
            throw new DatabaseEngineRuntimeException("Boolean true comparison failed");
        }
    }

    @Test
    public void booleanFalseComparisonTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry entry1 = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", true)
                .set("COL3", 1)
                .set("COL4", 1)
                .set("COL5", "val 1")
                .build();
        engine.persist("TEST", entry1, false);

        EntityEntry entry2 = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 1)
                .set("COL4", 1)
                .set("COL5", "val 1")
                .build();
        engine.persist("TEST", entry2, false);

        List<Map<String, ResultColumn>> rows = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.k(false)))
        );

        if (rows.size() != 1) {
            throw new DatabaseEngineRuntimeException("Boolean false comparison failed");
        }
    }

    @Test
    public void coalesceTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.coalesce(SqlBuilder.column("COL2"), SqlBuilder.k(false)), SqlBuilder.k(false)))
        );
    }

    @Test
    public void multipleCoalesceTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.coalesce(SqlBuilder.column("COL2"), SqlBuilder.k(false), SqlBuilder.k(true)), SqlBuilder.k(false)))
        );
    }

    @Test
    public void betweenTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.between(SqlBuilder.column("COL1"), SqlBuilder.k(1), SqlBuilder.k(2)))
        );
    }

    @Test
    public void testCast() throws DatabaseEngineException {

        final Query query = SqlBuilder.select(
                SqlBuilder.cast(SqlBuilder.k("22"), DbColumnType.INT).alias("int"),
                SqlBuilder.cast(SqlBuilder.k(22), DbColumnType.STRING).alias("string"),
                SqlBuilder.cast(SqlBuilder.k("1"), DbColumnType.BOOLEAN).alias("bool"),
                SqlBuilder.cast(SqlBuilder.k("22"), DbColumnType.DOUBLE).alias("double"),
                SqlBuilder.cast(SqlBuilder.k(22), DbColumnType.LONG).alias("long")
        );

        final Map<String, ResultColumn> result = engine.query(query).get(0);

        if (!new Integer(22).equals(result.get("int").toInt())) {
            throw new DatabaseEngineRuntimeException("Cast to int failed");
        }
        if (!"22".equals(result.get("string").toString())) {
            throw new DatabaseEngineRuntimeException("Cast to string failed");
        }
        if (!Boolean.TRUE.equals(result.get("bool").toBoolean())) {
            throw new DatabaseEngineRuntimeException("Cast to boolean failed");
        }
        if (!new Double(22).equals(result.get("double").toDouble())) {
            throw new DatabaseEngineRuntimeException("Cast to double failed");
        }
        if (!new Long(22).equals(result.get("long").toLong())) {
            throw new DatabaseEngineRuntimeException("Cast to long failed");
        }
    }

    @Test
    public void testCastColumns() throws DatabaseEngineException {

        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL_INT", DbColumnType.INT)
                .addColumn("COL_STRING", DbColumnType.STRING)
                .addColumn("COL_CAST_INT", DbColumnType.INT)
                .addColumn("COL_CAST_STRING", DbColumnType.STRING)
                .pkFields("COL_INT")
                .build();

        engine.addEntity(entity);

        EntityEntry entry = EntityEntry.builder()
                .set("COL_INT", 123)
                .set("COL_STRING", "321")
                .build();

        engine.persist("TEST", entry);

        final Update update = Update.builder()
                .table(SqlBuilder.table("TEST"))
                .set(SqlBuilder.eq(SqlBuilder.column("COL_CAST_INT"), SqlBuilder.cast(SqlBuilder.k("3211"), DbColumnType.INT)),
                     SqlBuilder.eq(SqlBuilder.column("COL_CAST_STRING"), SqlBuilder.cast(SqlBuilder.k(1233), DbColumnType.STRING)))
                .where(SqlBuilder.eq(SqlBuilder.column("COL_INT"), SqlBuilder.k(123)))
                .build();

        engine.executeUpdate(update);

        Query query =
                SqlBuilder.select(
                        SqlBuilder.cast(SqlBuilder.column("COL_INT"), DbColumnType.STRING).alias("COL_INT_string"),
                        SqlBuilder.cast(SqlBuilder.column("COL_STRING"), DbColumnType.INT).alias("COL_STRING_int"),
                        SqlBuilder.column("COL_CAST_INT"),
                        SqlBuilder.column("COL_CAST_STRING")
                ).from(SqlBuilder.table("TEST"));

        Map<String, ResultColumn> result = engine.query(query).get(0);

        if (!"123".equals(result.get("COL_INT_string").toString())) {
            throw new DatabaseEngineRuntimeException("Cast of COL_INT to string failed");
        }
        if (!new Integer(321).equals(result.get("COL_STRING_int").toInt())) {
            throw new DatabaseEngineRuntimeException("Cast of COL_STRING to int failed");
        }
        if (!new Integer(3211).equals(result.get("COL_CAST_INT").toInt())) {
            throw new DatabaseEngineRuntimeException("COL_CAST_INT value incorrect");
        }
        if (!"1233".equals(result.get("COL_CAST_STRING").toString())) {
            throw new DatabaseEngineRuntimeException("COL_CAST_STRING value incorrect");
        }

        entry = EntityEntry.builder()
                .set("COL_INT", 1000)
                .set("COL_STRING", "321000")
                .build();

        engine.persist("TEST", entry);

        query = SqlBuilder.select(SqlBuilder.column("COL_INT"))
                .from(SqlBuilder.table("TEST"))
                .orderby(SqlBuilder.column("COL_INT"));
        String firstResult = engine.query(query).get(0).get("COL_INT").toString();
        if (!"123".equals(firstResult)) {
            throw new DatabaseEngineRuntimeException("Numeric sort failed");
        }

        query = SqlBuilder.select(SqlBuilder.column("COL_INT"), SqlBuilder.cast(SqlBuilder.column("COL_INT"), DbColumnType.STRING).alias("COL_INT_string"))
                .from(SqlBuilder.table("TEST"))
                .orderby(SqlBuilder.column("COL_INT_string"));
        firstResult = engine.query(query).get(0).get("COL_INT").toString();
        if (!"1000".equals(firstResult)) {
            throw new DatabaseEngineRuntimeException("String sort failed");
        }
    }

    @Test(expected = OperationNotSupportedRuntimeException.class)
    public void testCastUnsupported() throws DatabaseEngineException {
        engine.query(SqlBuilder.select(SqlBuilder.cast(SqlBuilder.k("22"), DbColumnType.BLOB)));
    }

    @Test
    public void testWith() throws DatabaseEngineException {
        if (engine.getDialect() == SqlBuilder.Dialect.MYSQL) {
            return;
        }

        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "manuel").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "ana").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "rita").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "rui").build());

        final With with = With.builder("friends", SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")))
                .then(
                        SqlBuilder.select(SqlBuilder.column("COL5").alias("name"))
                        .from(SqlBuilder.table("friends"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)))
                ).build();

        final List<Map<String, ResultColumn>> result = engine.query(with);

        if (!"manuel".equals(result.get(0).get("name").toString())) {
            throw new DatabaseEngineRuntimeException("WITH query returned unexpected result");
        }
    }

    @Test
    public void testWithAll() throws DatabaseEngineException {
        if (engine.getDialect() == SqlBuilder.Dialect.MYSQL) {
            return;
        }

        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "manuel").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "ana").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "rita").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "rui").build());

        final With with =
                With.builder("friends", SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")))
                .then(
                        SqlBuilder.select(SqlBuilder.column("COL5").alias("name"))
                        .from(SqlBuilder.table("friends"))
                        .orderby(SqlBuilder.column("COL5"))
                ).build();

        final List<Map<String, ResultColumn>> result = engine.query(with);

        if (!"ana".equals(result.get(0).get("name").toString()) ||
            !"manuel".equals(result.get(1).get("name").toString()) ||
            !"rita".equals(result.get(2).get("name").toString()) ||
            !"rui".equals(result.get(3).get("name").toString())) {
            throw new DatabaseEngineRuntimeException("WITH all query returned unexpected order");
        }
    }

    @Test
    public void testWithMultiple() throws DatabaseEngineException {
        if (engine.getDialect() == SqlBuilder.Dialect.MYSQL) {
            return;
        }

        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "manuel").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "ana").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "rita").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "rui").build());

        final With with =
                With.builder("friendsA",
                        SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)), SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(2)))))
                .andWith("friendsB",
                        SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(3)), SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(4)))))
                .then(
                        SqlBuilder.union(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("friendsA")),
                                         SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("friendsB")))
                ).build();

        final List<Map<String, ResultColumn>> result = engine.query(with);

        final List<String> resultSorted = result.stream()
                .map(row -> row.get("COL5").toString())
                .sorted()
                .collect(Collectors.toList());

        if (!"ana".equals(resultSorted.get(0)) ||
            !"manuel".equals(resultSorted.get(1)) ||
            !"rita".equals(resultSorted.get(2)) ||
            !"rui".equals(resultSorted.get(3))) {
            throw new DatabaseEngineRuntimeException("Union query returned unexpected results");
        }
    }

    @Test
    public void testCaseWhen() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "teste").build());

        List<Map<String, ResultColumn>> result = engine.query(
                SqlBuilder.select(SqlBuilder.caseWhen().when(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste")), SqlBuilder.k("LOL")).alias("case"))
                        .from(SqlBuilder.table("TEST")));

        if (!"LOL".equals(result.get(0).get("case").toString()) || 
            !"LOL".equals(result.get(3).get("case").toString())) {
            throw new DatabaseEngineRuntimeException("CASE WHEN query returned unexpected result");
        }
    }

    @Test
    public void testCaseWhenElse() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "teste").build());

        List<Map<String, ResultColumn>> result = engine.query(
                SqlBuilder.select(SqlBuilder.caseWhen().when(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste")), SqlBuilder.k("LOL"))
                                .otherwise(SqlBuilder.k("ROFL")).alias("case"))
                        .from(SqlBuilder.table("TEST"))
        );

        if (!"LOL".equals(result.get(0).get("case").toString()) ||
            !"ROFL".equals(result.get(1).get("case").toString()) ||
            !"ROFL".equals(result.get(2).get("case").toString()) ||
            !"LOL".equals(result.get(3).get("case").toString())) {
            throw new DatabaseEngineRuntimeException("CASE WHEN ELSE query returned unexpected results");
        }
    }

    @Test
    public void testCaseMultipleWhenElse() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "pomme de terre").build());

        List<Map<String, ResultColumn>> result = engine.query(
                SqlBuilder.select(SqlBuilder.caseWhen().when(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste")), SqlBuilder.k("LOL"))
                                .when(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("pomme de terre")), SqlBuilder.k("KEK"))
                                .otherwise(SqlBuilder.k("ROFL")).alias("case"))
                        .from(SqlBuilder.table("TEST"))
        );

        if (!"LOL".equals(result.get(0).get("case").toString()) ||
            !"ROFL".equals(result.get(1).get("case").toString()) ||
            !"ROFL".equals(result.get(2).get("case").toString()) ||
            !"LOL".equals(result.get(3).get("case").toString()) ||
            !"KEK".equals(result.get(4).get("case").toString())) {
            throw new DatabaseEngineRuntimeException("Multiple CASE WHEN query returned unexpected result");
        }
    }

    @Test
    public void testConcat() throws DatabaseEngineException {
        final List<Map<String, ResultColumn>> result = queryConcat(SqlBuilder.k("."));

        if (!"teste.teste".equals(result.get(0).get("concat").toString()) ||
            !"xpto.xpto".equals(result.get(1).get("concat").toString()) ||
            !"xpto.xpto".equals(result.get(2).get("concat").toString()) ||
            !"teste.teste".equals(result.get(3).get("concat").toString()) ||
            !"pomme de terre.pomme de terre".equals(result.get(4).get("concat").toString())) {
            throw new DatabaseEngineRuntimeException("Concat query returned incorrect values");
        }
    }

    @Test
    public void testConcatEmpty() throws DatabaseEngineException {
        final List<Map<String, ResultColumn>> result = queryConcat(SqlBuilder.k(""));

        if (!"testeteste".equals(result.get(0).get("concat").toString()) ||
            !"xptoxpto".equals(result.get(1).get("concat").toString()) ||
            !"xptoxpto".equals(result.get(2).get("concat").toString()) ||
            !"testeteste".equals(result.get(3).get("concat").toString()) ||
            !"pomme de terrepomme de terre".equals(result.get(4).get("concat").toString())) {
            throw new DatabaseEngineRuntimeException("Concat empty query returned incorrect values");
        }
    }

    @Test
    public void testConcatNullExpressions() throws DatabaseEngineException {
        final Query query = SqlBuilder.select(SqlBuilder.concat(SqlBuilder.k(","), SqlBuilder.k("lol"), SqlBuilder.k(null), SqlBuilder.k("rofl")).alias("concat"));
        final List<Map<String, ResultColumn>> result = engine.query(query);
        if (!"lol,rofl".equals(result.get(0).get("concat").toString())) {
            throw new DatabaseEngineRuntimeException("Concat with null expressions returned incorrect value");
        }
    }

    @Test
    public void testConcatNullDelimiter() throws DatabaseEngineException {
        final Query query = SqlBuilder.select(SqlBuilder.concat(SqlBuilder.k(null), SqlBuilder.k("lol"), SqlBuilder.k("nop"), SqlBuilder.k("rofl")).alias("concat"));
        final List<Map<String, ResultColumn>> result = engine.query(query);
        if (!"lolnoprofl".equals(result.get(0).get("concat").toString())) {
            throw new DatabaseEngineRuntimeException("Concat with null delimiter returned incorrect value");
        }
    }

    @Test
    public void testConcatColumn() throws DatabaseEngineException {
        final List<Map<String, ResultColumn>> result = queryConcat(SqlBuilder.column("COL2"));

        if (!"testetesteteste".equals(result.get(0).get("concat").toString()) ||
            !"xptoxptoxpto".equals(result.get(1).get("concat").toString()) ||
            !"xptoxptoxpto".equals(result.get(2).get("concat").toString()) ||
            !"testetesteteste".equals(result.get(3).get("concat").toString()) ||
            !"pomme de terrepomme de terrepomme de terre".equals(result.get(4).get("concat").toString())) {
            throw new DatabaseEngineRuntimeException("Concat column query returned incorrect value");
        }
    }

    private List<Map<String, ResultColumn>> queryConcat(final SqlBuilder.Expression delimiter) throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.STRING)
                .addColumn("COL3", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", "teste").set("COL3", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL2", "xpto").set("COL3", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL2", "xpto").set("COL3", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL2", "teste").set("COL3", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL2", "pomme de terre").set("COL3", "pomme de terre").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 6).set("COL2", "lol").set("COL3", null).build());

        final Query query = SqlBuilder.select(SqlBuilder.concat(delimiter, SqlBuilder.column("COL2"), SqlBuilder.column("COL3")).alias("concat"))
                .from(SqlBuilder.table("TEST"));

        return engine.query(query);
    }

    @Test
    public void testCaseToBoolean() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", false).build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL2", true).set("COL5", "xpto").build());

        final Query query = SqlBuilder.select(SqlBuilder.column("COL2"),
                SqlBuilder.caseWhen().when(SqlBuilder.column("COL5").isNotNull(), SqlBuilder.k(true)).otherwise(SqlBuilder.k(false)).alias("COL5_NOT_NULL"))
                .from(SqlBuilder.table("TEST"))
                .orderby(SqlBuilder.column("COL1").asc());

        final List<Map<String, ResultColumn>> result = engine.query(query);

        if (result.get(0).get("COL2").toBoolean() ||
            result.get(0).get("COL5_NOT_NULL").toBoolean() ||
            !result.get(1).get("COL2").toBoolean() ||
            !result.get(1).get("COL5_NOT_NULL").toBoolean()) {
            throw new DatabaseEngineRuntimeException("CASE to boolean conversion failed");
        }
    }

    @Test
    public void testUnion() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "a").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "b").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "c").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "d").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "d").build());

        final String[] letters = new String[] {"a", "b", "c", "d", "d"};
        final Collection<SqlBuilder.Expression> queries = Arrays.stream(letters)
                .map(literal -> SqlBuilder.select(SqlBuilder.column("COL5"))
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k(literal))))
                .collect(Collectors.toList());

        final SqlBuilder.Expression query = SqlBuilder.union(queries);
        final List<Map<String, ResultColumn>> result = engine.query(query);

        if (result.size() != 4) {
            throw new DatabaseEngineRuntimeException("Union query did not return distinct results as expected");
        }

        final List<String> resultSorted = result.stream()
                .map(row -> row.get("COL5").toString())
                .sorted()
                .collect(Collectors.toList());

        if (!"a".equals(resultSorted.get(0)) ||
            !"b".equals(resultSorted.get(1)) ||
            !"c".equals(resultSorted.get(2)) ||
            !"d".equals(resultSorted.get(3))) {
            throw new DatabaseEngineRuntimeException("Union query results incorrect");
        }
    }

    @Test
    public void testUnionAll() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "a").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "b").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "c").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "d").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "d").build());

        final int[] ids = new int[] {1, 2, 3, 4, 5};
        final Collection<SqlBuilder.Expression> queries = Arrays.stream(ids)
                .mapToObj(literal ->
                        SqlBuilder.select(SqlBuilder.column("COL5"))
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(literal))))
                .collect(Collectors.toList());

        final SqlBuilder.Expression query = SqlBuilder.union(queries).all();
        final List<Map<String, ResultColumn>> result = engine.query(query);

        if (result.size() != 5) {
            throw new DatabaseEngineRuntimeException("Union all did not return all records");
        }

        final List<String> resultSorted = result.stream()
                .map(row -> row.get("COL5").toString())
                .sorted()
                .collect(Collectors.toList());

        if (!"a".equals(resultSorted.get(0)) ||
            !"b".equals(resultSorted.get(1)) ||
            !"c".equals(resultSorted.get(2)) ||
            !"d".equals(resultSorted.get(3)) ||
            !"d".equals(resultSorted.get(4))) {
            throw new DatabaseEngineRuntimeException("Union all results incorrect");
        }
    }

    @Test
    public void testValues() throws DatabaseEngineException {
        final Values values =
                Values.builder("id", "name")
                    .row(SqlBuilder.k(1), SqlBuilder.k("ana"))
                    .row(SqlBuilder.k(2), SqlBuilder.k("fred"))
                    .row(SqlBuilder.k(3), SqlBuilder.k("manuel"))
                    .row(SqlBuilder.k(4), SqlBuilder.k("rita"))
                    .build();

        final List<Map<String, ResultColumn>> result = engine.query(values);

        final List<Integer> ids = result.stream()
                .map(row -> row.get("id").toInt())
                .sorted()
                .collect(Collectors.toList());

        final List<String> names = result.stream()
                .map(row -> row.get("name").toString())
                .sorted()
                .collect(Collectors.toList());

        if (!(ids.get(0) == 1 && ids.get(1) == 2 && ids.get(2) == 3 && ids.get(3) == 4)) {
            throw new DatabaseEngineRuntimeException("Values query ids incorrect");
        }
        if (!("ana".equals(names.get(0)) && "fred".equals(names.get(1)) && "manuel".equals(names.get(2)) && "rita".equals(names.get(3)))) {
            throw new DatabaseEngineRuntimeException("Values query names incorrect");
        }
    }

    @Test(expected = DatabaseEngineRuntimeException.class)
    public void testValuesNoAliases() throws DatabaseEngineException {
        final Values values =
                Values.builder()
                    .row(SqlBuilder.k(1), SqlBuilder.k("ana"))
                    .row(SqlBuilder.k(2), SqlBuilder.k("fred"))
                    .row(SqlBuilder.k(3), SqlBuilder.k("manuel"))
                    .row(SqlBuilder.k(4), SqlBuilder.k("rita"))
                    .build();
        try {
            engine.query(values);
        } catch (DatabaseEngineRuntimeException e) {
            if (!"Values requires aliases to avoid ambiguous columns names.".equals(e.getMessage())) {
                throw e;
            }
            throw e;
        }
    }

    @Test
    public void testLargeValues() throws DatabaseEngineException {
        final Values values = Values.builder("long", "uuid");

        for (int i = 0 ; i < 256 ; i++) {
            values.row(SqlBuilder.k(ThreadLocalRandom.current().nextLong()),
                    SqlBuilder.k(UUID.randomUUID().toString()));
        }

        engine.query(values);
    }

    @Test
    public void betweenWithSelectTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.between(SqlBuilder.enclose(SqlBuilder.select(SqlBuilder.column("COL1")).from(SqlBuilder.table("TEST"))), SqlBuilder.k(1), SqlBuilder.k(2)))
        );
    }

    @Test
    public void betweenEnclosedTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.enclose(SqlBuilder.between(SqlBuilder.column("COL1"), SqlBuilder.k(1), SqlBuilder.k(2))))
        );
    }

    @Test
    public void notBetweenTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.enclose(SqlBuilder.notBetween(SqlBuilder.column("COL1"), SqlBuilder.k(1), SqlBuilder.k(2))))
        );
    }

    @Test
    public void modTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.INT)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 12)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 5)
                .set("COL5", "ADEUS")
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.mod(SqlBuilder.column("COL1"), SqlBuilder.column("COL4")).alias("MODULO"))
                                                                          .from(SqlBuilder.table("TEST")));

        if (query.get(0).get("MODULO").toInt() != 2) {
            throw new DatabaseEngineRuntimeException("Modulo operation returned incorrect result");
        }
    }

    @Test
    public void subSelectTest() throws DatabaseEngineException {
        List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(
                        SqlBuilder.k(1000).alias("timestamp"),
                        SqlBuilder.column("sq_1", "one").alias("first"),
                        SqlBuilder.column("sq_1", "two").alias("second"),
                        SqlBuilder.column("sq_1", "three").alias("third"))
                        .from(
                                SqlBuilder.select(
                                        SqlBuilder.k(1).alias("one"),
                                        SqlBuilder.k(2L).alias("two"),
                                        SqlBuilder.k(3.0).alias("three")).alias("sq_1")
                        )
        );

        if (query.get(0).get("timestamp").toLong() != 1000 ||
            query.get(0).get("first").toInt() != 1 ||
            query.get(0).get("second").toLong() != 2L ||
            Math.abs(query.get(0).get("third").toDouble() - 3.0) > 0.0) {
            throw new DatabaseEngineRuntimeException("Subselect query returned unexpected result");
        }
    }

    @Test
    public void update1ColTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(
                Update.builder()
                        .table(SqlBuilder.table("TEST"))
                        .set(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)))
                        .build()
        );
    }

    @Test
    public void update2ColTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(
                Update.builder()
                        .table(SqlBuilder.table("TEST"))
                        .set(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)),
                             SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("ola")))
                        .build()
        );
    }

    @Test
    public void updateWithAliasTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(
                Update.builder()
                        .table(SqlBuilder.table("TEST").alias("T"))
                        .set(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)),
                             SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("ola")))
                        .build()
        );
    }

    @Test
    public void updateWithWhereTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(
                Update.builder()
                        .table(SqlBuilder.table("TEST").alias("T"))
                        .set(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)),
                             SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("ola")))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(5)))
                        .build()
        );
    }

    @Test
    public void updateFrom1ColTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        final DbEntity entity = DbEntity.builder()
                .name("TEST2")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "xpto").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "pomme de terre").build());

        engine.persist("TEST2", EntityEntry.builder().set("COL1", 1).set("COL2", "update1").build());
        engine.persist("TEST2", EntityEntry.builder().set("COL1", 5).set("COL2", "update2").build());

        final Update updateFrom =
                Update.builder()
                        .table(SqlBuilder.table("TEST"))
                        .from(SqlBuilder.table("TEST2"))
                        .set(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.column("TEST2", "COL2")))
                        .where(SqlBuilder.eq(SqlBuilder.column("TEST", "COL1"), SqlBuilder.column("TEST2", "COL1")))
                        .build();

        engine.executeUpdate(updateFrom);

        final Query query = SqlBuilder.select(SqlBuilder.column("COL5"))
                .from(SqlBuilder.table("TEST"))
                .orderby(SqlBuilder.column("COL1"));

        final List<Map<String, ResultColumn>> result = engine.query(query);

        if (!"update1".equals(result.get(0).get("COL5").toString()) ||
            !"xpto".equals(result.get(1).get("COL5").toString()) ||
            !"xpto".equals(result.get(2).get("COL5").toString()) ||
            !"teste".equals(result.get(3).get("COL5").toString()) ||
            !"update2".equals(result.get(4).get("COL5").toString())) {
            throw new DatabaseEngineRuntimeException("Update from query failed");
        }
    }

    @Test
    public void deleteTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(SqlBuilder.delete(SqlBuilder.table("TEST")));
    }

    @Test
    public void deleteWithWhereTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        engine.executeUpdate(
                SqlBuilder.delete(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(5)))
        );
    }

    @Test
    public void deleteCheckReturnTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 6).build());

        int rowsDeleted = engine.executeUpdate(SqlBuilder.delete(SqlBuilder.table("TEST")));

        if (rowsDeleted != 2) {
            throw new DatabaseEngineRuntimeException("Delete did not remove expected number of rows");
        }
    }

    @Test
    public void executePreparedStatementTest() throws DatabaseEngineException, NameAlreadyExistsException, com.feedzai.commons.sql.abstraction.engine.ConnectionResetException {
        create5ColumnsEntity();

        EntityEntry ee = EntityEntry.builder().set("COL1", 1).set("COL2", true).build();

        engine.persist("TEST", ee);

        String ec = engine.escapeCharacter();
        engine.createPreparedStatement("test", "SELECT * FROM " + SqlBuilder.quotize("TEST", ec) + " WHERE " + SqlBuilder.quotize("COL1", ec) + " = ?");
        engine.setParameters("test", 1);
        engine.executePS("test");
        List<Map<String, ResultColumn>> res = engine.getPSResultSet("test");

        if (res.get(0).get("COL1").toInt() != 1 || !res.get(0).get("COL2").toBoolean()) {
            throw new DatabaseEngineRuntimeException("Prepared statement did not return correct values");
        }
    }

    @Test
    public void executePreparedStatementUpdateTest() throws DatabaseEngineException, NameAlreadyExistsException, com.feedzai.commons.sql.abstraction.engine.ConnectionResetException {
        create5ColumnsEntity();

        EntityEntry ee = EntityEntry.builder().set("COL1", 1).set("COL2", true).build();

        engine.persist("TEST", ee);

        engine.createPreparedStatement("test", Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.lit("?"))).build());
        engine.setParameters("test", 2);
        engine.executePSUpdate("test");

        List<Map<String, ResultColumn>> res = engine.query("SELECT * FROM " + SqlBuilder.quotize("TEST", engine.escapeCharacter()));

        if (res.get(0).get("COL1").toInt() != 2 || !res.get(0).get("COL2").toBoolean()) {
            throw new DatabaseEngineRuntimeException("Prepared update did not execute correctly");
        }
    }

    @Test
    public void metadataTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                        .name("TEST")
                        .addColumn("COL1", DbColumnType.INT)
                        .addColumn("COL2", DbColumnType.BOOLEAN)
                        .addColumn("COL3", DbColumnType.DOUBLE)
                        .addColumn("COL4", DbColumnType.LONG)
                        .addColumn("COL5", DbColumnType.STRING)
                        .addColumn("COL6", DbColumnType.BLOB)
                        .build();

        engine.addEntity(entity);

        final Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("COL1", DbColumnType.INT);
        metaMap.put("COL2", DbColumnType.BOOLEAN);
        metaMap.put("COL3", DbColumnType.DOUBLE);
        metaMap.put("COL4", DbColumnType.LONG);
        metaMap.put("COL5", DbColumnType.STRING);
        metaMap.put("COL6", DbColumnType.BLOB);

        if (!metaMap.equals(engine.getMetadata("TEST"))) {
            throw new DatabaseEngineRuntimeException("Metadata does not match expected map");
        }
    }

    @Test
    public void getMetadataOnATableThatDoesNotExistTest() throws DatabaseEngineException {
        if (!engine.getMetadata("TableThatDoesNotExist").isEmpty()) {
            throw new DatabaseEngineRuntimeException("Expected empty metadata for a non-existent table");
        }
    }

    @Test
    public void testSqlInjection1() throws DatabaseEngineException {
        create5ColumnsEntity();

        EntityEntry entry = EntityEntry.builder().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS").build();
        engine.persist("TEST", entry);
        entry = EntityEntry.builder().set("COL1", 2).set("COL2", false).set("COL3", 2D).set("COL4", 3L).set("COL5", "ADEUS2").build();
        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("ADEUS' or 1 = 1 " + engine.commentCharacter()))));

        if (result.size() != 0) {
            throw new DatabaseEngineRuntimeException("SQL injection test failed");
        }
    }

    @Test
    public void testBlob() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);

        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", new BlobTest(1, "name")).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !new BlobTest(1, "name").equals(result.get(0).get("COL2").<BlobTest>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob test failed");
        }

        BlobTest updBlob = new BlobTest(2, "cenas");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(updBlob);

        Update upd = Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.lit("?")))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k("CENINHAS")))
                .build();

        engine.createPreparedStatement("testBlob", upd);

        engine.setParameters("testBlob", bos.toByteArray());

        engine.executePSUpdate("testBlob");

        result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !updBlob.equals(result.get(0).get("COL2").<BlobTest>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob update failed");
        }
    }

    @Test
    public void testBlobSettingWithIndexTest() throws Exception {
        DbEntity entity = DbEntity.builder().name("TEST").addColumn("COL1", DbColumnType.STRING).addColumn("COL2", DbColumnType.BLOB)
                .build();
        engine.addEntity(entity);
        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", new BlobTest(1, "name")).build();
        engine.persist("TEST", entry);
        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !new BlobTest(1, "name").equals(result.get(0).get("COL2").<BlobTest>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob setting with index test failed");
        }

        BlobTest updBlob = new BlobTest(2, "cenas");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(updBlob);

        Update upd = Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.lit("?")))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k("CENINHAS")))
                .build();
        engine.createPreparedStatement("testBlob", upd);
        engine.setParameter("testBlob", 1, bos.toByteArray());
        engine.executePSUpdate("testBlob");
        result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !updBlob.equals(result.get(0).get("COL2").<BlobTest>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob setting with index update failed");
        }
    }

    @Test
    public void testBlobByteArray() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);

        byte[] bb = new byte[1024 * 1024 * 10];
        byte[] bb2 = new byte[1024 * 1024 * 10];
        for (int i = 0; i < bb.length; i++) {
            bb[i] = (byte) (Math.random() * 128);
            bb2[i] = (byte) (Math.random() * 64);
        }

        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", bb).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !java.util.Arrays.equals(bb, result.get(0).get("COL2").toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob ByteArray test failed");
        }

        Update upd = Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.lit("?")))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k("CENINHAS")))
                .build();

        engine.createPreparedStatement("upd", upd);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(bb2);

        engine.setParameters("upd", bos.toByteArray());

        engine.executePSUpdate("upd");

        result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !java.util.Arrays.equals(bb2, result.get(0).get("COL2").toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob ByteArray update test failed");
        }
    }

    @Test
    public void testBlobString() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            sb.append("a");
        }

        String bigString = sb.toString();
        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", bigString).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !bigString.equals(result.get(0).get("COL2").<String>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob String test failed");
        }
    }

    @Test
    public void testBlobJSON() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);

        String bigString = "[{\"type\":\"placeholder\",\"conf\":{},\"row\":0,\"height\":280,\"width\":12}]";
        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", bigString).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !bigString.equals(result.get(0).get("COL2").<String>toBlob())) {
            throw new DatabaseEngineRuntimeException("Blob JSON test failed");
        }
    }

    @Test
    public void addDropColumnWithDropCreateTest() throws DatabaseEngineException {
        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("USER", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1");
        engine.addEntity(entity.build());
        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.BOOLEAN.equals(test.get("COL2")) ||
            !DbColumnType.DOUBLE.equals(test.get("USER")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Initial metadata did not match");
        }

        EntityEntry entry = EntityEntry.builder().set("COL1", 1).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").build();
        engine.persist("TEST", entry);

        entity.removeColumn("USER");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        entry = EntityEntry.builder().set("COL1", 2).set("COL2", true).set("COL3", 2d).set("COL4", 1L).set("COL5", "c").build();
        engine.persist("TEST", entry);

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Metadata after column removal did not match");
        }

        entity.addColumn("COL6", DbColumnType.BLOB).addColumn("COL7", DbColumnType.DOUBLE);
        engine.updateEntity(entity.build());

        entry = EntityEntry.builder().set("COL1", 3).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").set("COL6", new BlobTest(1, "")).set("COL7", 2d).build();
        engine.persist("TEST", entry);

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5")) ||
            !DbColumnType.BLOB.equals(test.get("COL6")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL7"))) {
            throw new DatabaseEngineRuntimeException("Metadata after adding columns did not match");
        }
    }

    @Test
    public void addDropColumnTest() throws Exception {
        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("USER", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1");
        engine.addEntity(entity.build());
        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.BOOLEAN.equals(test.get("COL2")) ||
            !DbColumnType.DOUBLE.equals(test.get("USER")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Initial metadata did not match");
        }

        final DatabaseEngine engine2 = this.engine.duplicate(new Properties() {{
            setProperty("SCHEMA_POLICY", "create");
        }}, true);

        EntityEntry entry = EntityEntry.builder().set("COL1", 1).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").build();
        engine2.persist("TEST", entry);

        entity.removeColumn("USER");
        entity.removeColumn("COL2");
        engine2.updateEntity(entity.build());

        entry = EntityEntry.builder().set("COL1", 2).set("COL2", true).set("COL3", 2d).set("COL4", 1L).set("COL5", "c").build();
        engine2.persist("TEST", entry);

        test = engine2.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Metadata post removal did not match");
        }

        entity.addColumn("COL6", DbColumnType.BLOB).addColumn("COL7", DbColumnType.DOUBLE);
        engine2.updateEntity(entity.build());

        entry = EntityEntry.builder().set("COL1", 3).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").set("COL6", new BlobTest(1, "")).set("COL7", 2d).build();
        engine2.persist("TEST", entry);

        test = engine2.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5")) ||
            !DbColumnType.BLOB.equals(test.get("COL6")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL7"))) {
            throw new DatabaseEngineRuntimeException("Metadata final check failed");
        }
        engine2.close();
    }

    @Test
    public void updateEntityNoneSchemaPolicyCreatesInMemoryPreparedStmtsTest() throws DatabaseEngineException, DatabaseFactoryException {
        dropSilently("TEST");
        engine.removeEntity("TEST");

        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);

        properties.setProperty("SCHEMA_POLICY", "none");
        DatabaseEngine schemaNoneEngine = DatabaseFactory.getConnection(properties);

        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", true)
                .set("COL3", 1d)
                .set("COL4", 1L)
                .set("COL5", "1")
                .build();

        try {
            schemaNoneEngine.persist(entity.getName(), entry);
            throw new DatabaseEngineRuntimeException("Expected exception for unknown entity with NONE schema policy");
        } catch (final DatabaseEngineException e) {
            if (!e.getMessage().contains("Unknown entity")) {
                throw new DatabaseEngineRuntimeException("Unexpected exception message: " + e.getMessage());
            }
        }

        schemaNoneEngine.updateEntity(entity);

        if (!schemaNoneEngine.containsEntity(entity.getName())) {
            throw new DatabaseEngineRuntimeException("Entity should be known after updateEntity with NONE schema policy");
        }

        schemaNoneEngine.persist(entity.getName(), entry);
        List<Map<String, ResultColumn>> result = schemaNoneEngine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));

        if (result.size() != 1) {
            throw new DatabaseEngineRuntimeException("Expected one entry in table");
        }

        Map<String, ResultColumn> resultEntry = result.get(0);

        if (resultEntry.get("COL1").toInt() != 1 ||
            !Boolean.TRUE.equals(resultEntry.get("COL2").toBoolean()) ||
            resultEntry.get("COL3").toDouble() != 1.0 ||
            resultEntry.get("COL4").toLong() != 1L ||
            !"1".equals(resultEntry.get("COL5").toString())) {
            throw new DatabaseEngineRuntimeException("Persisted values do not match expected");
        }
    }

    @Test
    public void updateEntityNoneSchemaPolicyDoesntExecuteDDL() throws DatabaseFactoryException {
        dropSilently("TEST");

        properties.setProperty("SCHEMA_POLICY", "none");
        DatabaseEngine schemaNoneEngine = DatabaseFactory.getConnection(properties);

        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();

        try {
            schemaNoneEngine.updateEntity(entity);
            schemaNoneEngine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table(entity.getName())));
            throw new DatabaseEngineRuntimeException("Expected failure due to DDL not executed with NONE schema policy");
        } catch (final DatabaseEngineException e) {
            // Expected failure
        }
    }

    @Test
    public void addDropColumnNonExistentDropCreateTest() throws DatabaseEngineException {
        dropSilently("TEST");
        engine.removeEntity("TEST");

        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1");
        engine.updateEntity(entity.build());

        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.BOOLEAN.equals(test.get("COL2")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL3")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Initial metadata did not match");
        }

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.removeColumn("COL3");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Metadata after dropping columns did not match");
        }

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.addColumn("COL6", DbColumnType.BLOB).addColumn("COL7", DbColumnType.DOUBLE, DbColumnConstraint.NOT_NULL);
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5")) ||
            !DbColumnType.BLOB.equals(test.get("COL6")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL7"))) {
            throw new DatabaseEngineRuntimeException("Final metadata did not match");
        }
    }

    @Test
    public void addDropColumnNonExistentTest() throws Exception {
        dropSilently("TEST");
        engine.removeEntity("TEST");

        DatabaseEngine engine = this.engine.duplicate(new Properties() {{
            setProperty("SCHEMA_POLICY", "create");
        }}, true);

        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1");
        engine.updateEntity(entity.build());

        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.BOOLEAN.equals(test.get("COL2")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL3")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Initial metadata did not match");
        }

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.removeColumn("COL3");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5"))) {
            throw new DatabaseEngineRuntimeException("Metadata after dropping columns did not match");
        }

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.addColumn("COL6", DbColumnType.BLOB).addColumn("COL7", DbColumnType.DOUBLE, DbColumnConstraint.NOT_NULL);
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        if (!DbColumnType.INT.equals(test.get("COL1")) ||
            !DbColumnType.LONG.equals(test.get("COL4")) ||
            !DbColumnType.STRING.equals(test.get("COL5")) ||
            !DbColumnType.BLOB.equals(test.get("COL6")) ||
            !DbColumnType.DOUBLE.equals(test.get("COL7"))) {
            throw new DatabaseEngineRuntimeException("Final metadata did not match");
        }
    }

    @Test
    public void testInsertNullCLOB() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.CLOB)
                .build();
        engine.addEntity(entity);


        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString())) {
            throw new DatabaseEngineRuntimeException("CLOB insert test failed for COL1");
        }
        if (result.get(0).get("COL2").toString() != null) {
            throw new DatabaseEngineRuntimeException("CLOB insert test failed for COL2");
        }
    }

    @Test
    public void testCLOB() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.CLOB)
                .build();

        engine.addEntity(entity);

        StringBuilder sb = new StringBuilder();
        StringBuilder sb1 = new StringBuilder();
        for (int x = 0; x < 500000; x++) {
            sb.append(x);
            sb1.append(x * 2);
        }
        String initialClob = sb.toString();
        String updateClob = sb1.toString();

        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", initialClob).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));

        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !initialClob.equals(result.get(0).get("COL2").toString())) {
            throw new DatabaseEngineRuntimeException("Initial CLOB values do not match");
        }

        Update upd = Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.lit("?")))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k("CENINHAS")))
                .build();

        engine.createPreparedStatement("upd", upd);

        engine.setParameters("upd", updateClob);

        engine.executePSUpdate("upd");

        result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !updateClob.equals(result.get(0).get("COL2").toString())) {
            throw new DatabaseEngineRuntimeException("CLOB update failed");
        }
    }

    @Test
    public void testCLOBEncoding() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.STRING)
                .addColumn("COL2", DbColumnType.CLOB)
                .build();

        engine.addEntity(entity);

        String initialClob = "áãç";
        String updateClob = "áãç_áãç";

        EntityEntry entry = EntityEntry.builder().set("COL1", "CENINHAS").set("COL2", initialClob).build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !initialClob.equals(result.get(0).get("COL2").toString())) {
            throw new DatabaseEngineRuntimeException("CLOB encoding initial value mismatch");
        }

        Update upd = Update.builder(SqlBuilder.table("TEST")).set(SqlBuilder.eq(SqlBuilder.column("COL2"), SqlBuilder.lit("?")))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k("CENINHAS")))
                .build();

        engine.createPreparedStatement("upd", upd);

        engine.setParameters("upd", updateClob);

        engine.executePSUpdate("upd");

        result = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!"CENINHAS".equals(result.get(0).get("COL1").toString()) ||
            !updateClob.equals(result.get(0).get("COL2").toString())) {
            throw new DatabaseEngineRuntimeException("CLOB encoding update value mismatch");
        }
    }

    @Test
    public void testPersistOverrideAutoIncrement() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("MYTEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.STRING)
                .build();


        engine.addEntity(entity);

        EntityEntry ent = EntityEntry.builder().set("COL2", "CENAS1").build();
        engine.persist("MYTEST", ent);
        ent = EntityEntry.builder().set("COL2", "CENAS2").build();
        engine.persist("MYTEST", ent);

        ent = EntityEntry.builder().set("COL2", "CENAS3").set("COL1", 3).build();
        engine.persist("MYTEST", ent, false);

        ent = EntityEntry.builder().set("COL2", "CENAS5").set("COL1", 5).build();
        engine.persist("MYTEST", ent, false);

        ent = EntityEntry.builder().set("COL2", "CENAS6").build();
        engine.persist("MYTEST", ent);

        ent = EntityEntry.builder().set("COL2", "CENAS7").build();
        engine.persist("MYTEST", ent);

        final List<Map<String, ResultColumn>> query = engine.query("SELECT * FROM " + SqlBuilder.quotize("MYTEST", engine.escapeCharacter()));
        for (Map<String, ResultColumn> m : query) {
            if (!m.get("COL2").toString().endsWith(m.get("COL1").toString())) {
                throw new DatabaseEngineRuntimeException("Persist override auto increment failed");
            }
        }
        engine.close();
    }

    @Test
    public void testPersistOverrideAutoIncrement2() throws Exception {
        String APP_ID = "APP_ID";
        DbColumn APP_ID_COLUMN = new DbColumn.Builder().name(APP_ID).type(DbColumnType.INT).build();
        String STM_TABLE = "FDZ_APP_STREAM";
        String STM_ID = "STM_ID";
        String STM_NAME = "STM_NAME";
        DbEntity STREAM = DbEntity.builder().name(STM_TABLE)
                .addColumn(APP_ID_COLUMN)
                .addColumn(STM_ID, DbColumnType.INT, true)
                .addColumn(STM_NAME, DbColumnType.STRING, true)
                .pkFields(STM_ID, APP_ID)
                .build();

        engine.addEntity(STREAM);

        EntityEntry ent = EntityEntry.builder().set(APP_ID, 1).set(STM_ID, 1).set(STM_NAME, "NAME1").build();
        engine.persist(STM_TABLE, ent);

        ent = EntityEntry.builder().set(APP_ID, 2).set(STM_ID, 1).set(STM_NAME, "NAME1").build();
        engine.persist(STM_TABLE, ent, false);

        ent = EntityEntry.builder().set(APP_ID, 2).set(STM_ID, 2).set(STM_NAME, "NAME2").build();
        engine.persist(STM_TABLE, ent);

        ent = EntityEntry.builder().set(APP_ID, 1).set(STM_ID, 10).set(STM_NAME, "NAME10").build();
        engine.persist(STM_TABLE, ent, false);

        ent = EntityEntry.builder().set(APP_ID, 1).set(STM_ID, 2).set(STM_NAME, "NAME11").build();
        engine.persist(STM_TABLE, ent);

        ent = EntityEntry.builder().set(APP_ID, 2).set(STM_ID, 11).set(STM_NAME, "NAME11").build();
        engine.persist(STM_TABLE, ent, false);

        final List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table(STM_TABLE)));
        for (Map<String, ResultColumn> m : query) {
            if (!m.get(STM_NAME).toString().endsWith(m.get(STM_ID).toString())) {
                throw new DatabaseEngineRuntimeException("Stream name does not match stream id");
            }
        }
    }

    @Test
    public void testPersistOverrideAutoIncrement3() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("MYTEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry ent = EntityEntry.builder().set("COL2", "CENAS1").set("COL1", 1).build();
        engine.persist("MYTEST", ent, false);

        ent = EntityEntry.builder().set("COL2", "CENAS2").build();
        engine.persist("MYTEST", ent);

        ent = EntityEntry.builder().set("COL2", "CENAS5").set("COL1", 5).build();
        engine.persist("MYTEST", ent, false);

        ent = EntityEntry.builder().set("COL2", "CENAS6").build();
        engine.persist("MYTEST", ent);

        final List<Map<String, ResultColumn>> query = engine.query("SELECT * FROM " + SqlBuilder.quotize("MYTEST", engine.escapeCharacter()));
        for (Map<String, ResultColumn> m : query) {
            if (!m.get("COL2").toString().endsWith(m.get("COL1").toString())) {
                throw new DatabaseEngineRuntimeException("Persist override auto increment failed");
            }
        }
        engine.close();
    }

    @Test
    public void testTruncateTable() throws Exception {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());

        SqlBuilder.Truncate truncate = new SqlBuilder.Truncate(SqlBuilder.table("TEST"));

        engine.executeUpdate(truncate);

        final List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (!test.isEmpty()) {
            throw new DatabaseEngineRuntimeException("Truncate did not clear table");
        }
    }

    @Test
    public void testRenameTables() throws Exception {
        String oldName = "TBL_OLD";
        String newName = "TBL_NEW";

        dropSilently(oldName, newName);

        DbEntity entity = DbEntity.builder()
                .name(oldName)
                .addColumn("timestamp", DbColumnType.INT)
                .build();
        engine.addEntity(entity);
        engine.persist(oldName, EntityEntry.builder().set("timestamp", 20).build());

        Rename rename = new Rename(SqlBuilder.table(oldName), SqlBuilder.table(newName));
        engine.executeUpdate(rename);

        final Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("timestamp", DbColumnType.INT);
        if (!metaMap.equals(engine.getMetadata(newName))) {
            throw new DatabaseEngineRuntimeException("Renamed table metadata does not match");
        }

        List<Map<String, ResultColumn>> resultSet = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table(newName)));
        if (resultSet.size() != 1 || resultSet.get(0).get("timestamp").toInt() != 20) {
            throw new DatabaseEngineRuntimeException("Renamed table data incorrect");
        }

        dropSilently(newName);
    }

    private void dropSilently(String... tables) {
        for (String table : tables) {
            try {
                engine.dropEntity(DbEntity.builder().name(table).build());
            } catch (final Throwable e) {
                // ignore
            }
        }
    }

    @Test
    public void testLikeWithTransformation() throws Exception {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).set("COL5", "tesTte").build());

        List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), SqlBuilder.k("%teste%")))
        );
        if (query.size() != 3) {
            throw new DatabaseEngineRuntimeException("LIKE with transformation returned incorrect number of rows");
        }
        query = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), SqlBuilder.k("%tt%"))));
        if (query.size() != 1) {
            throw new DatabaseEngineRuntimeException("LIKE with transformation (second check) returned incorrect number of rows");
        }
    }

    @Test
    public void createSequenceOnLongColumnTest() throws Exception {
        DbEntity entity = DbEntity.builder()
                        .name("TEST")
                        .addColumn("COL1", DbColumnType.INT)
                        .addColumn("COL2", DbColumnType.BOOLEAN)
                        .addColumn("COL3", DbColumnType.DOUBLE)
                        .addColumn("COL4", DbColumnType.LONG, true)
                        .addColumn("COL5", DbColumnType.STRING)
                        .build();
        engine.addEntity(entity);
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        if (test.get(0).get("COL1").toInt() != 1 || !test.get(0).get("COL2").toBoolean() || test.get(0).get("COL4").toLong() != 1L) {
            throw new DatabaseEngineRuntimeException("Sequence on long column test failed");
        }
    }

    @Test
    public void insertWithNoAutoIncAndThatResumeTheAutoIncTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                        .name("TEST")
                        .addColumn("COL1", DbColumnType.INT)
                        .addColumn("COL2", DbColumnType.BOOLEAN)
                        .addColumn("COL3", DbColumnType.DOUBLE)
                        .addColumn("COL4", DbColumnType.LONG, true)
                        .addColumn("COL5", DbColumnType.STRING)
                        .build();
        engine.addEntity(entity);
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(0).get("COL4").toLong() != 1L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test initial failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 2).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(1).get("COL4").toLong() != 2L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test second failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(2).get("COL4").toLong() != 3L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test third failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 4).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(3).get("COL4").toLong() != 4L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test fourth failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(4).get("COL4").toLong() != 5L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test fifth failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 6).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(5).get("COL4").toLong() != 6L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test sixth failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 7).build(), false);
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(6).get("COL4").toLong() != 7L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test seventh failed");
        }

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4")));
        if (test.get(7).get("COL4").toLong() != 8L) {
            throw new DatabaseEngineRuntimeException("AutoInc resume test eighth failed");
        }
    }

    private void create5ColumnsEntity() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);
    }

    private void create5ColumnsEntityWithPrimaryKey() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                                          .name("TEST")
                                          .addColumn("COL1", DbColumnType.INT)
                                          .addColumn("COL2", DbColumnType.BOOLEAN)
                                          .addColumn("COL3", DbColumnType.DOUBLE)
                                          .addColumn("COL4", DbColumnType.LONG)
                                          .addColumn("COL5", DbColumnType.STRING)
                                          .pkFields("COL1")
                                          .build();

        engine.addEntity(entity);
    }

    protected void userRolePermissionSchema() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("USER")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);

        entity = DbEntity.builder()
                .name("ROLE")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();

        engine.addEntity(entity);

        entity = DbEntity.builder()
                .name("USER_ROLE")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.INT)
                .addFk(DbFk.builder().addColumn("COL1").referencedTable("USER").addReferencedColumn("COL1").build(),
                       DbFk.builder().addColumn("COL2").referencedTable("ROLE").addReferencedColumn("COL1").build())
                .pkFields("COL1", "COL2")
                .build();

        engine.addEntity(entity);
    }

    @Test
    public void testAndWhere() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1))).andWhere(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste"))));

        if (query.size() != 1 ||
            query.get(0).get("COL1").toInt() != 1 ||
            !"teste".equals(query.get(0).get("COL5").toString())) {
            throw new DatabaseEngineRuntimeException("AndWhere query returned incorrect result");
        }
    }

    @Test
    public void testAndWhereMultiple() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)), SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(4))))
                        .andWhere(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste")), SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("TESTE"))))
        );

        if (query.size() != 1 ||
            query.get(0).get("COL1").toInt() != 1 ||
            !"teste".equals(query.get(0).get("COL5").toString())) {
            throw new DatabaseEngineRuntimeException("AndWhere multiple query returned incorrect result");
        }
    }

    @Test
    public void testAndWhereMultipleCheckAndEnclosed() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 3).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 4).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(1)), SqlBuilder.eq(SqlBuilder.column("COL1"), SqlBuilder.k(4))))
                        .andWhere(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("teste")), SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k("tesTte"))))
        );

        if (query.size() != 2) {
            throw new DatabaseEngineRuntimeException("AndWhere multiple enclosed query returned incorrect result count");
        }
    }

    @Test
    public void testStringAgg() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.column("COL1"), SqlBuilder.stringAgg(SqlBuilder.column("COL5")).alias("agg"))
                        .from(SqlBuilder.table("TEST"))
                        .groupby(SqlBuilder.column("COL1"))
                        .orderby(SqlBuilder.column("COL1").asc())
        );

        if (query.size() != 2) {
            throw new DatabaseEngineRuntimeException("StringAgg query returned incorrect result count");
        }
    }

    @Test
    public void testStringAggDelimiter() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.column("COL1"), SqlBuilder.stringAgg(SqlBuilder.column("COL5")).delimiter(';').alias("agg"))
                        .from(SqlBuilder.table("TEST"))
                        .groupby(SqlBuilder.column("COL1"))
                        .orderby(SqlBuilder.column("COL1").asc())
        );
    }

    @Test
    public void testStringAggDistinct() throws DatabaseEngineException {
        if (!engine.isStringAggDistinctCapable()) {
            return;
        }

        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.column("COL1"), SqlBuilder.stringAgg(SqlBuilder.column("COL5")).distinct().alias("agg"))
                        .from(SqlBuilder.table("TEST"))
                        .groupby(SqlBuilder.column("COL1"))
                        .orderby(SqlBuilder.column("COL1").asc())
        );
    }

    @Test
    public void testStringAggNotStrings() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "TESTE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "TeStE").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 2).set("COL5", "tesTte").build());

        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.column("COL1"), SqlBuilder.stringAgg(SqlBuilder.column("COL1")).alias("agg"))
                        .from(SqlBuilder.table("TEST"))
                        .groupby(SqlBuilder.column("COL1"))
                        .orderby(SqlBuilder.column("COL1").asc())
        );
    }

    @Test
    @Category(com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithOneColumnTest() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);
        engine.executeUpdate(SqlBuilder.dropPK(SqlBuilder.table("TEST")));
    }

    @Test
    @Category(com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithTwoColumnsTest() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .pkFields("COL1", "COL4")
                .build();
        engine.addEntity(entity);
        engine.executeUpdate(SqlBuilder.dropPK(SqlBuilder.table("TEST")));
    }

    @Test
    public void alterColumnWithConstraintTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        engine.executeUpdate(new AlterColumn(SqlBuilder.table("TEST"), new DbColumn.Builder().name("COL1").type(DbColumnType.INT).addConstraint(DbColumnConstraint.NOT_NULL).build()));
    }

    @Test
    @Category(com.feedzai.commons.sql.abstraction.engine.impl.cockroach.SkipTestCockroachDB.class)
    public void alterColumnToDifferentTypeTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        engine.executeUpdate(new AlterColumn(SqlBuilder.table("TEST"), SqlBuilder.dbColumn().name("COL1").type(DbColumnType.STRING).build()));
    }

    @Test
    public void createTableWithDefaultsTest() throws DatabaseEngineException, DatabaseFactoryException {
        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, SqlBuilder.k(1))
                .addColumn("COL2", DbColumnType.BOOLEAN, SqlBuilder.k(false))
                .addColumn("COL3", DbColumnType.DOUBLE, SqlBuilder.k(2.2d))
                .addColumn("COL4", DbColumnType.LONG, SqlBuilder.k(3L))
                .pkFields("COL1");

        engine.addEntity(entity.build());

        final String ec = engine.escapeCharacter();
        engine.executeUpdate("INSERT INTO " + SqlBuilder.quotize("TEST", ec) + " (" + SqlBuilder.quotize("COL1", ec) + ") VALUES (10)");

        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        Map<String, ResultColumn> record = test.get(0);
        if (record.get("COL1").toInt() != 10 ||
            record.get("COL2").toBoolean() ||
            Math.abs(record.get("COL3").toDouble() - 2.2d) > 1e-9 ||
            record.get("COL4").toLong() != 3L) {
            throw new DatabaseEngineRuntimeException("Default values not applied correctly");
        }

        final DbEntity entity1 = entity
                .addColumn("COL5", DbColumnType.STRING, SqlBuilder.k("mantorras"), DbColumnConstraint.NOT_NULL)
                .addColumn("COL6", DbColumnType.BOOLEAN, SqlBuilder.k(true), DbColumnConstraint.NOT_NULL)
                .addColumn("COL7", DbColumnType.INT, SqlBuilder.k(7), DbColumnConstraint.NOT_NULL)
                .build();

        final Properties propertiesCreate = new Properties();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            propertiesCreate.setProperty(prop.getKey().toString(), prop.getValue().toString());
        }
        propertiesCreate.setProperty("SCHEMA_POLICY", "create");

        final DatabaseEngine connection2 = DatabaseFactory.getConnection(propertiesCreate);
        connection2.updateEntity(entity1);

        test = connection2.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        record = test.get(0);
        if (record.get("COL1").toInt() != 10 ||
            record.get("COL2").toBoolean() ||
            Math.abs(record.get("COL3").toDouble() - 2.2d) > 1e-9 ||
            record.get("COL4").toLong() != 3L ||
            !"mantorras".equals(record.get("COL5").toString()) ||
            !record.get("COL6").toBoolean() ||
            record.get("COL7").toInt() != 7) {
            throw new DatabaseEngineRuntimeException("Updated table with defaults did not match expected");
        }
        connection2.close();
    }

    @Test
    public void defaultValueOnBooleanColumnsTest() throws DatabaseEngineException {
        DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, SqlBuilder.k(1))
                .addColumn("COL2", DbColumnType.BOOLEAN, SqlBuilder.k(false), DbColumnConstraint.NOT_NULL)
                .addColumn("COL3", DbColumnType.DOUBLE, SqlBuilder.k(2.2d))
                .addColumn("COL4", DbColumnType.LONG, SqlBuilder.k(3L))
                .pkFields("COL1");

        engine.addEntity(entity.build());

        engine.persist("TEST", EntityEntry.builder().build());
        Map<String, ResultColumn> row = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))).get(0);

        if (row.get("COL1").toInt() != 1 ||
            row.get("COL2").toBoolean() ||
            Math.abs(row.get("COL3").toDouble() - 2.2d) > 0.0 ||
            row.get("COL4").toLong() != 3L) {
            throw new DatabaseEngineRuntimeException("Default value on boolean columns test failed");
        }
    }

    @Test
    public void upperTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "ola").build());
        if (!"OLA".equals(engine.query(SqlBuilder.select(SqlBuilder.upper(SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))).get(0).get("RES").toString())) {
            throw new DatabaseEngineRuntimeException("Upper test failed");
        }
    }

    @Test
    public void lowerTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "OLA").build());
        if (!"ola".equals(engine.query(SqlBuilder.select(SqlBuilder.lower(SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))).get(0).get("RES").toString())) {
            throw new DatabaseEngineRuntimeException("Lower test failed");
        }
    }

    @Test
    public void internalFunctionTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "OLA").build());
        if (!"ola".equals(engine.query(SqlBuilder.select(SqlBuilder.f("LOWER", SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))).get(0).get("RES").toString())) {
            throw new DatabaseEngineRuntimeException("Internal function test failed");
        }
    }

    @Test
    public void entityEntryHashcodeTest() {
        Map<String, Object> map = new HashMap<>();
        map.put("id1", "val1");
        map.put("id2", "val2");
        map.put("id3", "val3");
        map.put("id4", "val4");

        EntityEntry entry = EntityEntry.builder().set(map).build();

        if (map.hashCode() != entry.hashCode()) {
            throw new RuntimeException("EntityEntry hashcode does not match map's hashcode");
        }
    }

    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        final AtomicReference<java.sql.Connection> connReference = new AtomicReference<>();

        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            if (connReference.get().isClosed()) {
                throw new RuntimeException("Connection should not be closed within try-with-resources block");
            }
        }

        if (!connReference.get().isClosed()) {
            throw new RuntimeException("Connection should be closed after try-with-resources block");
        }

        try (final DatabaseEngine tryEngine = DatabaseFactory.getConnection(properties)) {
            connReference.set(tryEngine.getConnection());
            if (connReference.get().isClosed()) {
                throw new RuntimeException("Connection should not be closed within try-with-resources block");
            }
        }

        if (!connReference.get().isClosed()) {
            throw new RuntimeException("Connection should be closed after try-with-resources block");
        }
    }

    @Test
    public void closingAnEngineUsingTheCreateDropPolicyShouldDropAllEntities()
            throws DatabaseEngineException, DatabaseFactoryException {

        properties.setProperty("SCHEMA_POLICY", "create-drop");
        engine = DatabaseFactory.getConnection(properties);

        engine.addEntity(EntityEntryBuilder.buildEntity("ENTITY-1"));
        engine.addEntity(EntityEntryBuilder.buildEntity("ENTITY-2"));

        new Expectations(engine) {};

        engine.close();

        new Verifications() {{
            engine.dropEntity((DbEntity) any); times = 2;
        }};
    }

    @Test
    public void doesRowCountIncrementTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        for (int i = 0; i < 4; i++) {
            engine.persist("TEST", EntityEntry.builder().set("COL1", i).build());
        }

        final ResultIterator resultIterator = engine.iterator(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));

        if (resultIterator.getCurrentRowCount() != 0) {
            throw new DatabaseEngineRuntimeException("Row count should be 0 before iteration");
        }

        resultIterator.next();

        if (resultIterator.getCurrentRowCount() != 1) {
            throw new DatabaseEngineRuntimeException("Row count should be 1 after one next()");
        }

        for(int i = 0; i < 3; i++) {
            resultIterator.nextResult();
        }

        if (resultIterator.getCurrentRowCount() != 4) {
            throw new DatabaseEngineRuntimeException("Row count should be 4 after four iterations");
        }
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        engine.persist("TEST", EntityEntry.builder().set("COL5", TestEnum.TEST_ENUM_VAL).build());

        engine.persist("TEST", EntityEntry.builder().set("COL5", "something else").build());

        final List<Map<String, ResultColumn>> results = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL5"), SqlBuilder.k(TestEnum.TEST_ENUM_VAL)))
        );

        if (results.size() != 1 || !TestEnum.TEST_ENUM_VAL.name().equals(results.get(0).get("COL5").toString())) {
            throw new DatabaseEngineRuntimeException("Enum test failed");
        }
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = EntityEntry.builder().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        engine.persist("TEST", entry);
        try {
            engine.persist("TEST", entry);
        } catch (final DatabaseEngineException e) {
            if (!(e instanceof DatabaseEngineUniqueConstraintViolationException) ||
                !(e.getCause() instanceof SQLException) ||
                !"Something went wrong persisting the entity [unique_constraint_violation]".equals(e.getMessage())) {
                throw e;
            }
            throw e;
        }
    }

    @Test
    public void batchInsertDuplicateDBError() throws DatabaseEngineException {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = EntityEntry.builder().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);

        try {
            engine.flush();
        } catch (final DatabaseEngineException e) {
            if (!(e instanceof DatabaseEngineUniqueConstraintViolationException) ||
                !(e.getCause() instanceof SQLException) ||
                !"Something went wrong while flushing [unique_constraint_violation]".equals(e.getMessage())) {
                throw e;
            }
            throw e;
        }
    }

    private enum TestEnum {
        TEST_ENUM_VAL;
        @Override
        public String toString() {
            return super.toString() + " description";
        }
    }
}