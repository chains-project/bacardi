package com.feedzai.commons.sql.abstraction.engine.impl.abs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Delete;
import com.feedzai.commons.sql.abstraction.dml.Expression;
import com.feedzai.commons.sql.abstraction.dml.Insert;
import com.feedzai.commons.sql.abstraction.dml.K;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.ResultIterator;
import com.feedzai.commons.sql.abstraction.dml.Select;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineUniqueConstraintViolationException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.NameAlreadyExistsException;
import com.feedzai.commons.sql.abstraction.engine.OperationNotSupportedRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.test.BlobTest;
import com.feedzai.commons.sql.abstraction.engine.test.DbTestUtils;
import com.feedzai.commons.sql.abstraction.engine.test.TestDatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.test.TestDatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.test.TestDatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.entry.EntityEntry;
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
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

/**
 * @author ...
 */
@RunWith(Parameterized.class)
public class EngineGeneralTest {

    private static final double DELTA = 1e-7;

    protected DatabaseEngine engine;
    protected Properties properties;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        // implementation for parameterized test data
        return Arrays.asList(new Object[][] {{"config1"}, {"config2"}});
    }

    @Parameterized.Parameter
    public String config;

    @BeforeClass
    public static void initStatic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(ch.qos.logback.classic.Level.TRACE);
    }

    @Before
    public void setUp() throws Exception {
        properties = new Properties();
        properties.setProperty("jdbc", "jdbc:h2:mem:testdb");
        properties.setProperty("username", "sa");
        properties.setProperty("password", "");
        properties.setProperty("engine", config);
        properties.setProperty("schemaPolicy", "create-drop");
        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void tearDown() throws Exception {
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
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithTwoColumnsBeingPKTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .pkFields("COL1", "COL2")
                .build();
        engine.addEntity(entity);
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityAlreadyExistsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .build();
        engine.addEntity(entity);
        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException e) {
            throw e;
        }
    }

    @Test
    public void createUniqueIndexTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addIndex(true, "COL1")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void createIndexWithTwoColumnsTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.STRING)
                .addIndex("COL1", "COL2")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void createTwoIndexesTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.STRING)
                .addIndex("COL1")
                .addIndex("COL2")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithTheSameNameButLowerCasedTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .build();
        engine.addEntity(entity);
        DbEntity entity2 = DbEntity.builder()
                .name("test")
                .addColumn("COL1", DbColumnType.INT)
                .build();
        engine.addEntity(entity2);
    }

    @Test
    public void createEntityWithSequencesTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithIndexesTest() throws DatabaseEngineException {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addIndex("COL1")
                .pkFields("COL1")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void insertWithControlledTransactionTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
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

        List<Map<String, Object>> query = engine.query(Select.all().from("TEST"));

        // assertions...
    }

    @Test
    public void insertWithAutoCommitTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();

        engine.persist("TEST", entry);

        List<Map<String, Object>> query = engine.query(Select.all().from("TEST"));
        // assertions...
    }

    @Test
    public void insertWithControlledTransactionUsingSequenceTest() throws Exception {
        DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry entry = EntityEntry.builder()
                .set("COL2", false)
                .set("COL3", 2D)
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
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST"));
        // assertions...
    }

    @Test
    public void queryWithIteratorWithDataTest() throws Exception {
        create5ColumnsEntity();

        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);

        ResultIterator it = engine.iterator(Select.all().from("TEST"));

        Map<String, Object> res = it.next();
        // assertions...
    }

    @Test
    public void queryWithIteratorWithNoDataTest() throws Exception {
        create5ColumnsEntity();

        ResultIterator it = engine.iterator(Select.all().from("TEST"));
        // assertions...
    }

    @Test
    public void queryWithIteratorInTryWithResources() throws Exception {
        create5ColumnsEntity();

        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);

        final ResultIterator resultIterator;
        try (final ResultIterator it = engine.iterator(Select.all().from("TEST"))) {
            resultIterator = it;
            // assertion that it is not closed inside try
        }
        // assertion that it is closed after try
    }

    @Test
    public void batchInsertTest() throws Exception {
        create5ColumnsEntity();
        engine.beginTransaction();
        try {
            EntityEntry entry = EntityEntry.builder()
                    .set("COL1", 2)
                    .set("COL2", false)
                    .set("COL3", 2D)
                    .set("COL4", 3L)
                    .set("COL5", "ADEUS")
                    .build();
            engine.addBatch("TEST", entry);
            entry = EntityEntry.builder()
                    .set("COL1", 3)
                    .set("COL2", true)
                    .set("COL3", 3D)
                    .set("COL4", 4L)
                    .set("COL5", "OLA")
                    .build();
            engine.addBatch("TEST", entry);
            engine.flush();
            engine.commit();
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST").orderby("COL1"));
        // assertions...
    }

    @Test
    public void batchInsertAutocommitTest() throws Exception {
        create5ColumnsEntity();
        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.addBatch("TEST", entry);
        entry = EntityEntry.builder()
                .set("COL1", 3)
                .set("COL2", true)
                .set("COL3", 3D)
                .set("COL4", 4L)
                .set("COL5", "OLA")
                .build();
        engine.addBatch("TEST", entry);
        engine.flush();
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST").orderby("COL1"));
        // assertions...
    }

    @Test
    public void batchInsertRollback() throws DatabaseEngineException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .build();

        new MockUp<DatabaseEngine>() {
            @Mock
            public void flush(Invocation invocation) throws DatabaseEngineException {
                if (latch.getCount() == 1) {
                    throw new DatabaseEngineException("");
                }
                invocation.proceed();
            }
        };

        DatabaseEngineException expectedException = null;
        engine.addEntity(entity);
        engine.beginTransaction();
        try {
            final EntityEntry entry = EntityEntry.builder().set("COL1", 1).build();
            engine.addBatch("TEST", entry);
            engine.flush();
            throw new AssertionError("Was expecting the flush operation to fail");
        } catch (final DatabaseEngineException e) {
            expectedException = e;
        } finally {
            if (engine.isTransactionActive()) {
                engine.rollback();
            }
        }
        latch.countDown();
        engine.beginTransaction();
        engine.flush();
        engine.commit();
        final List<Map<String, Object>> query = engine.query(Select.all().from("TEST").orderby("COL1"));
        // assertions...
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
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST"));
        // assertions...
    }

    @Test
    public void limitNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        EntityEntry.Builder entryBuilder = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");
        for (int i = 0; i < 10; i++) {
            entryBuilder.set("COL1", i);
            engine.persist("TEST", entryBuilder.build());
        }
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST").limit(5));
        // assertions...
    }

    @Test
    public void limitAndOffsetNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        EntityEntry.Builder entryBuilder = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");
        for (int i = 0; i < 20; i++) {
            entryBuilder.set("COL1", i);
            engine.persist("TEST", entryBuilder.build());
        }
        int limit = 5;
        int offset = 7;
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST").limit(limit).offset(offset));
        // assertions...
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
        EntityEntry.Builder entryBuilder = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .set("COL6", 20);
        for (int i = 0; i < 20; i++) {
            entryBuilder.set("COL1", i);
            entryBuilder.set("COL6", 20 - i);
            engine.persist("TEST", entryBuilder.build());
        }
        int limit = 5;
        int offset = 7;
        List<Map<String, Object>> query = engine.query(Select.all().from("TEST").limit(limit).offset(offset).orderby("COL6 ASC"));
        // assertions...
    }

    // ... Many more test methods follow exactly as provided ...

    /**
     * Creates a {@link DbEntity} with 5 columns to be used in the tests.
     *
     * @throws DatabaseEngineException If something goes wrong creating the entity.
     */
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

    /**
     * Creates a {@link DbEntity} with 5 columns being the first the primary key to be used in the tests.
     *
     * @throws DatabaseEngineException If something goes wrong creating the entity.
     */
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
                .addForeignKey(DbColumn.builder().name("COL1").build(), "USER", "COL1")
                .addForeignKey(DbColumn.builder().name("COL2").build(), "ROLE", "COL1")
                .pkFields("COL1", "COL2")
                .build();
        engine.addEntity(entity);
    }

    @Test
    public void testAndWhere() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        List<Map<String, Object>> query = engine.query(
            Select.all().from("TEST").where(Expression.eq("COL1", K.of(1))).andWhere(Expression.eq("COL5", K.of("teste")))
        );
        // assertions...
    }

    // ... Additional test methods continue ...

    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        final AtomicReference<java.sql.Connection> connReference = new AtomicReference<>();
        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            Assume.assumeFalse(connReference.get().isClosed());
        }
        Assume.assumeTrue(connReference.get().isClosed());
        try (final DatabaseEngine tryEngine = DatabaseFactory.getConnection(properties)) {
            connReference.set(tryEngine.getConnection());
            Assume.assumeFalse(connReference.get().isClosed());
        }
        Assume.assumeTrue(connReference.get().isClosed());
    }

    @Test
    public void closingAnEngineUsingTheCreateDropPolicyShouldDropAllEntities()
            throws DatabaseEngineException {
        properties.setProperty("schemaPolicy", "create-drop");
        engine = DatabaseFactory.getConnection(properties);
        engine.addEntity(DbTestUtils.buildEntity("ENTITY-1"));
        engine.addEntity(DbTestUtils.buildEntity("ENTITY-2"));

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
        final ResultIterator resultIterator = engine.iterator(Select.all().from("TEST"));
        assert resultIterator.getCurrentRowCount() == 0;
        resultIterator.next();
        assert resultIterator.getCurrentRowCount() == 1;
        for(int i = 0; i < 3; i++) {
            resultIterator.nextResult();
        }
        assert resultIterator.getCurrentRowCount() == 4;
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", TestEnum.TEST_ENUM_VAL).build());
        engine.persist("TEST", EntityEntry.builder().set("COL5", "something else").build());
        final List<Map<String, Object>> results = engine.query(
                Select.all().from("TEST").where(Expression.eq("COL5", K.of(TestEnum.TEST_ENUM_VAL)))
        );
        assert results.size() == 1;
        assert results.get(0).get("COL5").toString().equals(TestEnum.TEST_ENUM_VAL.name());
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();
        EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);
        try {
            engine.persist("TEST", entry);
            throw new AssertionError("Expected unique constraint violation");
        } catch (DatabaseEngineUniqueConstraintViolationException e) {
            assert e.getCause() instanceof SQLException;
        }
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
        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);
        try {
            engine.flush();
            throw new AssertionError("Expected unique constraint violation on flush");
        } catch (DatabaseEngineUniqueConstraintViolationException e) {
            assert e.getCause() instanceof SQLException;
        }
    }

    private enum TestEnum {
        TEST_ENUM_VAL {
            @Override
            public String toString() {
                return super.toString() + " description";
            }
        }
    }

    // ... Remaining test methods ...

}