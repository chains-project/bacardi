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
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Expression;
import com.feedzai.commons.sql.abstraction.dml.F;
import com.feedzai.commons.sql.abstraction.dml.K;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.dml.With;
import com.feedzai.commons.sql.abstraction.dml.dialect.SqlBuilder;
import com.feedzai.commons.sql.abstraction.dml.result.ResultColumn;
import com.feedzai.commons.sql.abstraction.dml.result.ResultIterator;
import com.feedzai.commons.sql.abstraction.engine.AbstractDatabaseEngine;
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
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
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
import org.junit.After;
import org.junit.Assume;
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
    protected DatabaseConfiguration configuration;

    @Parameterized.Parameters
    public static Collection<DatabaseConfiguration> data() throws Exception {
        return DatabaseTestUtil.loadConfigurations();
    }

    @BeforeClass
    public static void initStatic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.TRACE);
    }

    @Before
    public void setUp() throws DatabaseFactoryException {
        properties = new Properties();
        properties.putAll(System.getProperties());
        if (configuration != null) {
            properties.putAll(configuration.getProperties());
        }
        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void tearDown() {
        if (engine != null) {
            engine.close();
        }
    }

    @Test
    public void createEntityTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                                  .name("TEST")
                                  .addColumn("COL1", DbColumnType.INT)
                                  .addColumn("COL2", DbColumnType.BOOLEAN)
                                  .addColumn("COL3", DbColumnType.DOUBLE)
                                  .addColumn("COL4", DbColumnType.LONG)
                                  .addColumn("COL5", DbColumnType.STRING)
                                  .build();

        engine.addEntity(entity);
    }

    @Test
    public void insertWithControlledTransactionTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 2)
                                       .set("COL2", false)
                                       .set("COL3", 2.0)
                                       .set("COL4", 3L)
                                       .set("COL5", "ADEUS")
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
    }

    @Test
    public void persistWitoutTransactionTest() throws Exception {
        create5ColumnsEntity();
        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 1)
                                       .set("COL2", true)
                                       .set("COL3", 1.0)
                                       .set("COL4", 1L)
                                       .set("COL5", "TEST")
                                       .build();
        engine.persist("TEST", entry);
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityAlreadyExistsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                                  .name("TEST")
                                  .addColumn("COL1", DbColumnType.INT)
                                  .build();

        engine.addEntity(entity);
        // Attempt to add again to trigger exception.
        engine.addEntity(entity);
    }

    @Test
    public void updateEntityTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 1)
                                       .build();

        engine.persist("TEST", entry);
        engine.executeUpdate(Update.builder()
                                   .table("TEST")
                                   .set(SqlBuilder.eq(SqlBuilder.column("COL1"), K.k(2)))
                                   .build());
    }

    @Test
    public void queryTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        List<Map<String, ResultColumn>> result = engine.query(SqlBuilder.select(SqlBuilder.all())
                                                                          .from(SqlBuilder.table("TEST")));
        // Assert result is not null.
        if (result == null) {
            throw new AssertionError("Result is null");
        }
    }
    
    @Test
    public void batchInsertTest() throws Exception {
        create5ColumnsEntity();
        engine.beginTransaction();
        try {
            EntityEntry entry1 = EntityEntry.builder()
                                            .set("COL1", 3)
                                            .build();
            engine.addBatch("TEST", entry1);
            EntityEntry entry2 = EntityEntry.builder()
                                            .set("COL1", 4)
                                            .build();
            engine.addBatch("TEST", entry2);
            engine.flush();
            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
    }

    @Test
    public void deleteTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());
        engine.executeUpdate(SqlBuilder.delete(SqlBuilder.table("TEST")));
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

        Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("COL1", DbColumnType.INT);
        metaMap.put("COL2", DbColumnType.BOOLEAN);
        metaMap.put("COL3", DbColumnType.DOUBLE);
        metaMap.put("COL4", DbColumnType.LONG);
        metaMap.put("COL5", DbColumnType.STRING);
        metaMap.put("COL6", DbColumnType.BLOB);

        if (!metaMap.equals(engine.getMetadata("TEST"))) {
            throw new AssertionError("Metadata does not match");
        }
    }

    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        AtomicReference<Connection> connReference = new AtomicReference<>();

        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            if (connReference.get().isClosed()) {
                throw new AssertionError("Connection should not be closed within try-with-resources");
            }
        }
        if (!connReference.get().isClosed()) {
            throw new AssertionError("Connection should be closed after try-with-resources block");
        }
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();

        // Persist using enum value.
        engine.persist("TEST", EntityEntry.builder().set("COL5", TestEnum.TEST_ENUM_VAL).build());
        engine.persist("TEST", EntityEntry.builder().set("COL5", "something else").build());

        List<Map<String, ResultColumn>> results = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                          .from(SqlBuilder.table("TEST"))
                          .where(SqlBuilder.eq(SqlBuilder.column("COL5"), K.k(TestEnum.TEST_ENUM_VAL)))
        );

        if (results.size() != 1 || !results.get(0).get("COL5").toString().equals(TestEnum.TEST_ENUM_VAL.name())) {
            throw new AssertionError("Enum value not persisted as its string representation");
        }
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = EntityEntry.builder()
                                       .set("COL1", 2)
                                       .set("COL2", false)
                                       .set("COL3", 2.0)
                                       .set("COL4", 3L)
                                       .set("COL5", "ADEUS")
                                       .build();

        engine.persist("TEST", entry);
        try {
            engine.persist("TEST", entry);
            throw new AssertionError("Expected DatabaseEngineException not thrown");
        } catch (DatabaseEngineException e) {
            if (!e.getMessage().contains("unique_constraint_violation")) {
                throw new AssertionError("Unexpected exception message", e);
            }
        }
    }

    private void create5ColumnsEntity() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
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

    // The rest of the test methods remain unchanged.
}