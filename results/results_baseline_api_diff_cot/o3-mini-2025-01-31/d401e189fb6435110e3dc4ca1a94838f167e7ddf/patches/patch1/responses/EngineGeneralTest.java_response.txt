package com.feedzai.commons.sql.abstraction.engine.impl.abs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Delete;
import com.feedzai.commons.sql.abstraction.dml.Expression;
import com.feedzai.commons.sql.abstraction.dml.Insert;
import com.feedzai.commons.sql.abstraction.dml.K;
import com.feedzai.commons.sql.abstraction.dml.Query;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.dml.With;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineUniqueConstraintViolationException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.MappedEntity;
import com.feedzai.commons.sql.abstraction.engine.TestUtil;
import com.feedzai.commons.sql.abstraction.engine.testconfigs.H2Configuration;
import com.feedzai.commons.sql.abstraction.dml.ResultColumn;
import com.feedzai.commons.sql.abstraction.dml.ResultIterator;
import com.feedzai.commons.sql.abstraction.dml.SqlBuilder;
import com.feedzai.commons.sql.abstraction.entry.EntityEntry;
import com.feedzai.commons.sql.abstraction.util.SqlUtils;
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
 * Tests for the SQL abstraction engine.
 * @author ...
 */
@RunWith(Parameterized.class)
public class EngineGeneralTest {

    private DatabaseEngine engine;
    private Properties properties;

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws Exception {
        return TestUtil.loadTestConfigurations();
    }

    @Parameterized.Parameter
    public H2Configuration configuration;

    @BeforeClass
    public static void initStatic() {
        // Instead of using the removed Logger interface, we use LoggerContext to set the level.
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.TRACE);
    }

    @Before
    public void init() throws Exception {
        properties = new Properties();
        properties.setProperty("jdbcUrl", configuration.getJdbcUrl());
        properties.setProperty("username", configuration.getUsername());
        properties.setProperty("password", configuration.getPassword());
        properties.setProperty("engine", configuration.getEngine());
        properties.setProperty("schemaPolicy", configuration.getSchemaPolicy());
        engine = DatabaseFactory.getConnection(properties);
    }

    @After
    public void cleanup() throws Exception {
        engine.close();
    }

    @Test
    public void createEntityTest() throws DatabaseEngineException {
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

    @Test
    public void createEntityWithTwoColumnsBeingPKTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .build();
        engine.addEntity(entity);
    }

    @Test(expected = DatabaseEngineException.class)
    public void createEntityAlreadyExistsTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .build();
        engine.addEntity(entity);

        try {
            engine.addEntity(entity);
        } catch (final DatabaseEngineException e) {
            if (!"Entity 'TEST' is already defined".equals(e.getMessage())) {
                throw e;
            }
            throw e;
        }
    }

    @Test
    public void createUniqueIndexTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .addIndex(true, "COL4")
            .build();
        engine.addEntity(entity);
    }

    @Test
    public void createIndexWithTwoColumnsTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .addIndex("COL4", "COL3")
            .build();
        engine.addEntity(entity);
    }

    @Test
    public void createTwoIndexesTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .addIndex("COL4")
            .addIndex("COL3")
            .build();
        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithTheSameNameButLowerCasedTest() throws DatabaseEngineException {
        final DbEntity entity1 = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .build();
        engine.addEntity(entity1);

        final DbEntity entity2 = DbEntity.builder()
            .name("test")
            .addColumn("COL1", DbColumnType.INT)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1", "COL3")
            .build();
        engine.addEntity(entity2);
    }

    @Test
    public void createEntityWithSequencesTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT, true)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .pkFields("COL1")
            .build();
        engine.addEntity(entity);
    }

    @Test
    public void createEntityWithIndexesTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
            .name("TEST")
            .addColumn("COL1", DbColumnType.INT, true)
            .addColumn("COL2", DbColumnType.BOOLEAN)
            .addColumn("COL3", DbColumnType.DOUBLE)
            .addColumn("COL4", DbColumnType.LONG)
            .addColumn("COL5", DbColumnType.STRING)
            .addIndex("COL4")
            .pkFields("COL1")
            .build();
        engine.addEntity(entity);
    }

    @Test
    public void insertWithControlledTransactionTest() throws Exception {
        create5ColumnsEntity();

        final EntityEntry entry = EntityEntry.builder()
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

        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );

        TestUtil.assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void insertWithAutoCommitTest() throws Exception {
        create5ColumnsEntity();

        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();

        engine.persist("TEST", entry);

        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );

        TestUtil.assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void insertWithControlledTransactionUsingSequenceTest() throws Exception {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, true)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);

        final EntityEntry entry = EntityEntry.builder()
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
        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );

        TestUtil.assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 1, (int) query.get(0).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", query.get(0).get("COL5").toString());
    }

    @Test
    public void queryWithIteratorWithDataTest() throws Exception {
        create5ColumnsEntity();
        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 1)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);
        final ResultIterator it = engine.iterator(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        final Map<String, ResultColumn> res = it.next();
        TestUtil.assertNotNull("result is not null", res);
        TestUtil.assertTrue("COL1 exists", res.containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 1, (int) res.get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", res.containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", res.get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", res.containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, res.get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", res.containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) res.get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", res.containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", res.get("COL5").toString());
        TestUtil.assertNull("no more data to consume?", it.next());
        TestUtil.assertTrue("result set is closed?", it.isClosed());
        TestUtil.assertNull("next on a closed result set must return null", it.next());
        it.close();
    }

    @Test
    public void queryWithIteratorWithNoDataTest() throws Exception {
        create5ColumnsEntity();
        final ResultIterator it = engine.iterator(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        TestUtil.assertNull("result is null", it.next());
        TestUtil.assertNull("no more data to consume?", it.next());
        TestUtil.assertTrue("result set is closed?", it.isClosed());
        TestUtil.assertNull("next on a closed result set must return null", it.next());
        it.close();
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
        try (final ResultIterator it = engine.iterator(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")))) {
            resultIterator = it;
            TestUtil.assertFalse("Result iterator should not be closed before exiting try-with-resources block",
                    resultIterator.isClosed());
        }
        TestUtil.assertTrue("Result iterator should be closed after exiting try-with-resources block",
                resultIterator.isClosed());
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
            if (engine.isTransactionActive())
                engine.rollback();
        }
        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL1").asc())
        );
        // 1st row
        TestUtil.assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", query.get(0).get("COL5").toString());
        // 2nd row
        TestUtil.assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        TestUtil.assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "OLA", query.get(1).get("COL5").toString());
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
        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL1").asc())
        );
        // 1st row
        TestUtil.assertTrue("COL1 exists", query.get(0).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 2, (int) query.get(0).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(0).containsKey("COL2"));
        TestUtil.assertFalse("COL2 ok?", query.get(0).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(0).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 2D, query.get(0).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(0).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 3L, (long) query.get(0).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(0).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "ADEUS", query.get(0).get("COL5").toString());
        // 2nd row
        TestUtil.assertTrue("COL1 exists", query.get(1).containsKey("COL1"));
        TestUtil.assertEquals("COL1 ok?", 3, (int) query.get(1).get("COL1").toInt());
        TestUtil.assertTrue("COL2 exists", query.get(1).containsKey("COL2"));
        TestUtil.assertTrue("COL2 ok?", query.get(1).get("COL2").toBoolean());
        TestUtil.assertTrue("COL3 exists", query.get(1).containsKey("COL3"));
        TestUtil.assertEquals("COL3 ok?", 3D, query.get(1).get("COL3").toDouble(), 0);
        TestUtil.assertTrue("COL4 exists", query.get(1).containsKey("COL4"));
        TestUtil.assertEquals("COL4 ok?", 4L, (long) query.get(1).get("COL4").toLong());
        TestUtil.assertTrue("COL5 exists", query.get(1).containsKey("COL5"));
        TestUtil.assertEquals("COL5 ok?", "OLA", query.get(1).get("COL5").toString());
    }

    @Test
    public void batchInsertRollback() throws DatabaseEngineException {
        final CountDownLatch latch = new CountDownLatch(1);
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .build();

        new MockUp<AbstractDatabaseEngine>() {
            @Mock
            public synchronized void flush(final Invocation invocation) throws DatabaseEngineException {
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
            TestUtil.fail("Was expecting the flush operation to fail");
        } catch (final DatabaseEngineException e) {
            expectedException = e;
        } finally {
            if (engine.isTransactionActive())
                engine.rollback();
        }
        TestUtil.assertNotNull("DB returned exception when flushing", expectedException);
        latch.countDown();
        engine.beginTransaction();
        engine.flush();
        engine.commit();
        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL1").asc())
        );
        TestUtil.assertEquals("There are no rows on table TEST", 0, query.size());
    }

    @Test
    public void blobTest() throws DatabaseEngineException {
        final double[] original = new double[]{5, 6, 7};
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BLOB)
                .build();

        engine.addEntity(entity);
        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", original)
                .build();

        engine.persist("TEST", entry);
        final List<Map<String, ResultColumn>> query = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        int i = 0;
        for (double d : original) {
            TestUtil.assertEquals("arrays are equal?", d, query.get(0).get("COL2").<double[]>toBlob()[i++], 0D);
        }
    }

    @Test
    public void limitNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        final EntityEntry.Builder entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS");
        for (int i = 0; i < 10; i++) {
            entry.set("COL1", i);
            engine.persist("TEST", entry.build());
        }
        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).limit(5)
        );

        TestUtil.assertEquals("number of rows ok?", 5, query.size());
    }

    @Test
    public void limitAndOffsetNumberOfRowsTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        final EntityEntry.Builder entry = EntityEntry.builder()
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
        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).limit(limit).offset(offset)
        );
        TestUtil.assertEquals("number of rows ok?", limit, query.size());
        for (int i = offset, j = 0; i < offset + limit; i++, j++) {
            TestUtil.assertEquals("Check correct row", i, query.get(j).get("COL1").toInt().intValue());
        }
    }

    @Test
    public void limitOffsetAndOrderNumberOfRowsTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .addColumn("COL6", DbColumnType.INT)
                .build();

        engine.addEntity(entity);
        final EntityEntry.Builder entry = EntityEntry.builder()
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
        final List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).limit(limit).offset(offset).orderby(SqlBuilder.column("COL6").asc())
        );
        TestUtil.assertEquals("number of rows ok?", limit, query.size());
        for (int i = offset, j = 0; i < offset + limit; i++, j++) {
            TestUtil.assertEquals("Check correct row col1", 19 - i, query.get(j).get("COL1").toInt().intValue());
            TestUtil.assertEquals("Check correct row col6", i + 1, query.get(j).get("COL6").toInt().intValue());
        }
    }

    // ... (Other test methods remain unchanged) ...

    @Test
    public void testPersistOverrideAutoIncrement3() throws Exception {
        final DbEntity entity = DbEntity.builder()
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

        final List<Map<String, ResultColumn>> query = engine.query(
                "SELECT * FROM " + SqlUtils.quotize("MYTEST", engine.escapeCharacter())
        );
        for (Map<String, ResultColumn> resultRow : query) {
            TestUtil.assertTrue(resultRow.get("COL2").toString().endsWith(resultRow.get("COL1").toString()));
        }
        engine.close();
    }

    @Test
    public void testTruncateTable() throws Exception {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL1", 5).build());
        final Truncate truncate = new Truncate(SqlBuilder.table("TEST"));
        engine.executeUpdate(truncate);
        final List<Map<String, ResultColumn>> test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        TestUtil.assertTrue("Test truncate query empty?", test.isEmpty());
    }

    @Test
    public void testRenameTables() throws Exception {
        final String oldName = "TBL_OLD";
        final String newName = "TBL_NEW";

        dropSilently(oldName, newName);

        final DbEntity entity = DbEntity.builder()
                .name(oldName)
                .addColumn("timestamp", DbColumnType.INT)
                .build();
        engine.addEntity(entity);
        engine.persist(oldName, EntityEntry.builder().set("timestamp", 20).build());

        final Rename rename = new Rename(SqlBuilder.table(oldName), SqlBuilder.table(newName));
        engine.executeUpdate(rename);

        final Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("timestamp", DbColumnType.INT);
        TestUtil.assertEquals("Metamap ok?", metaMap, engine.getMetadata(newName));

        final List<Map<String, ResultColumn>> resultSet = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table(newName))
        );
        TestUtil.assertEquals("Count ok?", 1, resultSet.size());

        TestUtil.assertEquals("Content ok?", 20, (int) resultSet.get(0).get("timestamp").toInt());

        dropSilently(newName);
    }

    private void dropSilently(String... tables) {
        for (String table : tables) {
            try {
                engine.dropEntity(DbEntity.builder().name(table).build());
            } catch (Throwable e) {
                // Ignore exceptions silently.
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
            SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), K.k("%teste%")))
        );
        TestUtil.assertEquals(3, query.size());
        query = engine.query(
            SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.like(SqlBuilder.udf("lower", SqlBuilder.column("COL5")), K.k("%tt%")))
        );
        TestUtil.assertEquals(1, query.size());
    }

    @Test
    public void createSequenceOnLongColumnTest() throws Exception {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG, true)
                .addColumn("COL5", DbColumnType.STRING)
                .build();
        engine.addEntity(entity);
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        final List<Map<String, ResultColumn>> test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        TestUtil.assertEquals("col1 ok?", 1, (int) test.get(0).get("COL1").toInt());
        TestUtil.assertTrue("col2 ok?", test.get(0).get("COL2").toBoolean());
        TestUtil.assertEquals("col4 ok?", 1L, (long) test.get(0).get("COL4").toLong());
    }

    @Test
    public void insertWithNoAutoIncAndThatResumeTheAutoIncTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG, true)
                .addColumn("COL5", DbColumnType.STRING)
                .build();
        engine.addEntity(entity);
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        List<Map<String, ResultColumn>> test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 1L, (long) test.get(0).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 2).build(), false);
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 2L, (long) test.get(1).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 3L, (long) test.get(2).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 4).build(), false);
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 4L, (long) test.get(3).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 5L, (long) test.get(4).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 6).build(), false);
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 6L, (long) test.get(5).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).set("COL4", 7).build(), false);
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 7L, (long) test.get(6).get("COL4").toLong());

        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL2", true).build());
        test = engine.query(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")).orderby(SqlBuilder.column("COL4"))
        );
        TestUtil.assertEquals("col4 ok?", 8L, (long) test.get(7).get("COL4").toLong());
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
                .addFk(DbColumn.fkBuilder()
                        .addColumn("COL1")
                        .referencedTable("USER")
                        .addReferencedColumn("COL1")
                        .build(),
                       DbColumn.fkBuilder()
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
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        engine.persist("TEST", EntityEntry.builder().set("COL1", 1).set("COL5", "teste").build());
        List<Map<String, ResultColumn>> query = engine.query(
            SqlBuilder.select(SqlBuilder.all())
                .from(SqlBuilder.table("TEST"))
                .where(SqlBuilder.eq(SqlBuilder.column("COL1"), K.k(1)))
                .andWhere(SqlBuilder.eq(SqlBuilder.column("COL5"), K.k("teste")))
        );
        TestUtil.assertEquals("Resultset must have only one result", 1, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be teste", "teste", query.get(0).get("COL5").toString());
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
                .where(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL1"), K.k(1)), SqlBuilder.eq(SqlBuilder.column("COL1"), K.k(4))))
                .andWhere(SqlBuilder.or(SqlBuilder.eq(SqlBuilder.column("COL5"), K.k("teste")), SqlBuilder.eq(SqlBuilder.column("COL5"), K.k("TESTE"))))
        );
        TestUtil.assertEquals("Resultset must have only one result", 1, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be teste", "teste", query.get(0).get("COL5").toString());
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
        TestUtil.assertEquals("Resultset must have only 2 results", 2, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be TESTE,teste", "TESTE,teste", query.get(0).get("agg").toString());
        TestUtil.assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be TeStE,tesTte", "TeStE,tesTte", query.get(1).get("agg").toString());
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
        TestUtil.assertEquals("Resultset must have only 2 results", 2, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be TESTE;teste", "TESTE;teste", query.get(0).get("agg").toString());
        TestUtil.assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be TeStE;tesTte", "TeStE;tesTte", query.get(1).get("agg").toString());
    }

    @Test
    public void testStringAggDistinct() throws DatabaseEngineException {
        Assume.assumeTrue("This test is only valid for engines that support StringAggDistinct",
                engine.isStringAggDistinctCapable());
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
        TestUtil.assertEquals("Resultset must have only 2 results", 2, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be teste", "teste", query.get(0).get("agg").toString());
        TestUtil.assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be TeStE,tesTte", "TeStE,tesTte", query.get(1).get("agg").toString());
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
        TestUtil.assertEquals("Resultset must have only 2 results", 2, query.size());
        TestUtil.assertEquals("COL1 must be 1", 1, query.get(0).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be 1,1", "1,1", query.get(0).get("agg").toString());
        TestUtil.assertEquals("COL1 must be 2", 2, query.get(1).get("COL1").toInt().intValue());
        TestUtil.assertEquals("COL5 must be 2,2", "2,2", query.get(1).get("agg").toString());
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithOneColumnTest() throws Exception {
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
        engine.executeUpdate(SqlBuilder.dropPK(SqlBuilder.table("TEST")));
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void dropPrimaryKeyWithTwoColumnsTest() throws Exception {
        final DbEntity entity = DbEntity.builder()
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
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);
        engine.executeUpdate(new AlterColumn(SqlBuilder.table("TEST"),
                new DbColumn.Builder().name("COL1").type(DbColumnType.INT).addConstraint(DbColumnConstraint.NOT_NULL).build()));
    }

    @Test
    @Category(SkipTestCockroachDB.class)
    public void alterColumnToDifferentTypeTest() throws DatabaseEngineException {
        final DbEntity entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT)
                .addColumn("COL2", DbColumnType.BOOLEAN)
                .addColumn("COL3", DbColumnType.DOUBLE)
                .addColumn("COL4", DbColumnType.LONG)
                .addColumn("COL5", DbColumnType.STRING)
                .build();

        engine.addEntity(entity);
        engine.executeUpdate(new AlterColumn(SqlBuilder.table("TEST"),
                SqlBuilder.dbColumn().name("COL1").type(DbColumnType.STRING).build()));
    }

    @Test
    public void createTableWithDefaultsTest() throws DatabaseEngineException, DatabaseFactoryException {
        final DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, new K(1))
                .addColumn("COL2", DbColumnType.BOOLEAN, new K(false))
                .addColumn("COL3", DbColumnType.DOUBLE, new K(2.2d))
                .addColumn("COL4", DbColumnType.LONG, new K(3L))
                .pkFields("COL1");

        engine.addEntity(entity.build());
        final String ec = engine.escapeCharacter();
        engine.executeUpdate("INSERT INTO " + SqlUtils.quotize("TEST", ec) + " (" + SqlUtils.quotize("COL1", ec) + ") VALUES (10)");

        List<Map<String, ResultColumn>> test = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        TestUtil.assertEquals("Check size of records", 1, test.size());
        final Map<String, ResultColumn> record = test.get(0);
        TestUtil.assertEquals("Check COL1", 10, record.get("COL1").toInt().intValue());
        TestUtil.assertEquals("Check COL2", false, record.get("COL2").toBoolean());
        TestUtil.assertEquals("Check COL3", 2.2d, record.get("COL3").toDouble(), 0);
        TestUtil.assertEquals("Check COL4", 3L, record.get("COL4").toLong().longValue());

        final DbEntity entity1 = entity
                .addColumn("COL5", DbColumnType.STRING, new K("mantorras"), DbColumnConstraint.NOT_NULL)
                .addColumn("COL6", DbColumnType.BOOLEAN, new K(true), DbColumnConstraint.NOT_NULL)
                .addColumn("COL7", DbColumnType.INT, new K(7), DbColumnConstraint.NOT_NULL)
                .build();

        final Properties propertiesCreate = new Properties();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            propertiesCreate.setProperty(prop.getKey().toString(), prop.getValue().toString());
        }
        propertiesCreate.setProperty("schemaPolicy", "create");

        final DatabaseEngine connection2 = DatabaseFactory.getConnection(propertiesCreate);
        connection2.updateEntity(entity1);
        test = connection2.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST")));
        TestUtil.assertEquals("Check size of records", 1, test.size());
        final Map<String, ResultColumn> record2 = test.get(0);
        TestUtil.assertEquals("Check COL1", 10, record2.get("COL1").toInt().intValue());
        TestUtil.assertEquals("Check COL2", false, record2.get("COL2").toBoolean());
        TestUtil.assertEquals("Check COL3", 2.2d, record2.get("COL3").toDouble(), 1e-9);
        TestUtil.assertEquals("Check COL4", 3L, record2.get("COL4").toLong().longValue());
        TestUtil.assertEquals("Check COL5", "mantorras", record2.get("COL5").toString());
        TestUtil.assertEquals("Check COL6", true, record2.get("COL6").toBoolean());
        TestUtil.assertEquals("Check COL7", 7, record2.get("COL7").toInt().intValue());
        connection2.close();
    }

    @Test
    public void defaultValueOnBooleanColumnsTest() throws DatabaseEngineException {
        final DbEntity.Builder entity = DbEntity.builder()
                .name("TEST")
                .addColumn("COL1", DbColumnType.INT, new K(1))
                .addColumn("COL2", DbColumnType.BOOLEAN, new K(false), DbColumnConstraint.NOT_NULL)
                .addColumn("COL3", DbColumnType.DOUBLE, new K(2.2d))
                .addColumn("COL4", DbColumnType.LONG, new K(3L))
                .pkFields("COL1");

        engine.addEntity(entity.build());
        engine.persist("TEST", EntityEntry.builder().build());
        final Map<String, ResultColumn> row = engine.query(SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))).get(0);
        TestUtil.assertEquals("", 1, row.get("COL1").toInt().intValue());
        TestUtil.assertFalse("", row.get("COL2").toBoolean());
        TestUtil.assertEquals("", 2.2d, row.get("COL3").toDouble(), 0D);
        TestUtil.assertEquals("", 3L, row.get("COL4").toLong().longValue());
    }

    @Test
    public void upperTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "ola").build());
        TestUtil.assertEquals("text is uppercase", "OLA", engine.query(
                SqlBuilder.select(SqlBuilder.upper(SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))
        ).get(0).get("RES").toString());
    }

    @Test
    public void lowerTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "OLA").build());
        TestUtil.assertEquals("text is lowercase", "ola", engine.query(
                SqlBuilder.select(SqlBuilder.lower(SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))
        ).get(0).get("RES").toString());
    }

    @Test
    public void internalFunctionTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", "OLA").build());
        TestUtil.assertEquals("text is uppercase", "ola", engine.query(
                SqlBuilder.select(SqlBuilder.f("LOWER", SqlBuilder.column("COL5")).alias("RES")).from(SqlBuilder.table("TEST"))
        ).get(0).get("RES").toString());
    }

    @Test
    public void entityEntryHashcodeTest() {
        final Map<String, Object> map = new HashMap<>();
        map.put("id1", "val1");
        map.put("id2", "val2");
        map.put("id3", "val3");
        map.put("id4", "val4");
        final EntityEntry entry = EntityEntry.builder()
                .set(map)
                .build();
        TestUtil.assertEquals("entry's hashCode() matches map's hashCode()", map.hashCode(), entry.hashCode());
    }

    @Test
    public void tryWithResourcesClosesEngine() throws Exception {
        final AtomicReference<java.sql.Connection> connReference = new AtomicReference<>();
        try (final DatabaseEngine tryEngine = this.engine) {
            connReference.set(tryEngine.getConnection());
            TestUtil.assertFalse("close() method should not be called within the try-with-resources block, for an existing DatabaseEngine",
                    connReference.get().isClosed());
        }
        TestUtil.assertTrue("close() method should be called after exiting try-with-resources block, for an existing DatabaseEngine",
                connReference.get().isClosed());
        try (final DatabaseEngine tryEngine = DatabaseFactory.getConnection(properties)) {
            connReference.set(tryEngine.getConnection());
            TestUtil.assertFalse("close() method should not be called within the try-with-resources block, for a DatabaseEngine created in the block",
                    connReference.get().isClosed());
        }
        TestUtil.assertTrue("close() method should be called after exiting try-with-resources block, for a DatabaseEngine created in the block",
                connReference.get().isClosed());
    }

    @Test
    public void closingAnEngineUsingTheCreateDropPolicyShouldDropAllEntities()
            throws DatabaseEngineException, DatabaseFactoryException {
        properties.setProperty("schemaPolicy", "create-drop");
        engine = DatabaseFactory.getConnection(properties);
        engine.addEntity(TestUtil.buildEntity("ENTITY-1"));
        engine.addEntity(TestUtil.buildEntity("ENTITY-2"));
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
        final ResultIterator resultIterator = engine.iterator(
                SqlBuilder.select(SqlBuilder.all()).from(SqlBuilder.table("TEST"))
        );
        TestUtil.assertEquals("The current row count should be 0 if the iteration hasn't started", 0, resultIterator.getCurrentRowCount());
        resultIterator.next();
        TestUtil.assertEquals("The current row count is equal to 1", 1, resultIterator.getCurrentRowCount());
        for (int i = 0; i < 3; i++) {
            resultIterator.nextResult();
        }
        TestUtil.assertEquals("The current row count is equal to 4", 4, resultIterator.getCurrentRowCount());
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();
        engine.persist("TEST", EntityEntry.builder().set("COL5", TestUtil.TestEnum.TEST_ENUM_VAL).build());
        engine.persist("TEST", EntityEntry.builder().set("COL5", "something else").build());
        final List<Map<String, ResultColumn>> results = engine.query(
                SqlBuilder.select(SqlBuilder.all())
                        .from(SqlBuilder.table("TEST"))
                        .where(SqlBuilder.eq(SqlBuilder.column("COL5"), K.k(TestUtil.TestEnum.TEST_ENUM_VAL)))
        );
        TestUtil.assertEquals("One (and only one) result expected.", 1, results.size());
        TestUtil.assertEquals("An enum value should be persisted as its string representation",
                TestUtil.TestEnum.TEST_ENUM_VAL.name(), results.get(0).get("COL5").toString());
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();
        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.persist("TEST", entry);
        TestUtil.assertThatCode(() -> engine.persist("TEST", entry))
            .as("Is unique constraint violation exception")
            .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
            .as("Encapsulated exception is SQLException")
            .hasCauseInstanceOf(SQLException.class)
            .hasMessage("Something went wrong persisting the entity [unique_constraint_violation]");
    }

    @Test
    public void batchInsertDuplicateDBError() throws DatabaseEngineException {
        create5ColumnsEntityWithPrimaryKey();
        final EntityEntry entry = EntityEntry.builder()
                .set("COL1", 2)
                .set("COL2", false)
                .set("COL3", 2D)
                .set("COL4", 3L)
                .set("COL5", "ADEUS")
                .build();
        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);
        TestUtil.assertThatCode(() -> engine.flush())
            .as("Is unique constraint violation exception")
            .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
            .as("Encapsulated exception is SQLException")
            .hasCauseInstanceOf(SQLException.class)
            .hasMessage("Something went wrong while flushing [unique_constraint_violation]");
    }
}