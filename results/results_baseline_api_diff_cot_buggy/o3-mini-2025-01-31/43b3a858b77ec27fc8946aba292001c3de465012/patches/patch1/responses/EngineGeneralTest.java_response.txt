package com.feedzai.commons.sql.abstraction.engine.impl.abs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.feedzai.commons.sql.abstraction.ddl.AlterColumn;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint;
import com.feedzai.commons.sql.abstraction.ddl.DbColumnType;
import com.feedzai.commons.sql.abstraction.ddl.DbEntity;
import com.feedzai.commons.sql.abstraction.ddl.DbFk;
import com.feedzai.commons.sql.abstraction.ddl.Rename;
import com.feedzai.commons.sql.abstraction.dml.Delete;
import com.feedzai.commons.sql.abstraction.dml.Select;
import com.feedzai.commons.sql.abstraction.dml.Truncate;
import com.feedzai.commons.sql.abstraction.dml.Update;
import com.feedzai.commons.sql.abstraction.dml.Values;
import com.feedzai.commons.sql.abstraction.engine.ConnectionResetException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngine;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineRuntimeException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseEngineUniqueConstraintViolationException;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactory;
import com.feedzai.commons.sql.abstraction.engine.DatabaseFactoryException;
import com.feedzai.commons.sql.abstraction.engine.NameAlreadyExistsException;
import com.feedzai.commons.sql.abstraction.engine.metadata.MappedEntity;
import com.feedzai.commons.sql.abstraction.entry.EntityEntry;
import com.feedzai.commons.sql.abstraction.util.StringUtils;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnConstraint.NOT_NULL;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.BLOB;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.CLOB;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.DOUBLE;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.INT;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.LONG;
import static com.feedzai.commons.sql.abstraction.ddl.DbColumnType.STRING;
import static com.feedzai.commons.sql.abstraction.dml.DmlBuilder.delete;
import static com.feedzai.commons.sql.abstraction.dml.DmlBuilder.dropPK;
import static com.feedzai.commons.sql.abstraction.dml.DmlBuilder.insert;
import static com.feedzai.commons.sql.abstraction.dml.DmlBuilder.select;
import static com.feedzai.commons.sql.abstraction.dml.DmlBuilder.update;
import static com.feedzai.commons.sql.abstraction.entry.EntityEntryBuilder.entry;
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

@RunWith(Parameterized.class)
public class EngineGeneralTest {

    @BeforeClass
    public static void initStatic() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.TRACE);
    }

    // ... (all other test methods and code remain unchanged)

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

    @Test
    public void testBlob() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", BLOB)
                .build();

        engine.addEntity(entity);

        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", new BlobTest(1, "name"))
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(new BlobTest(1, "name"), result.get(0).get("COL2").<BlobTest>toBlob());

        BlobTest updBlob = new BlobTest(2, "cenas");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(updBlob);

        Update upd = update(table("TEST")).set(eq(column("COL2"), lit("?"))).where(eq(column("COL1"), k("CENINHAS")));

        engine.createPreparedStatement("testBlob", upd);

        engine.setParameters("testBlob", bos.toByteArray());

        engine.executePSUpdate("testBlob");

        result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(updBlob, result.get(0).get("COL2").<BlobTest>toBlob());
    }

    @Test
    public void testBlobSettingWithIndexTest() throws Exception {
        DbEntity entity = dbEntity().name("TEST").addColumn("COL1", STRING).addColumn("COL2", BLOB)
                .build();
        engine.addEntity(entity);
        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", new BlobTest(1, "name"))
                .build();
        engine.persist("TEST", entry);
        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(new BlobTest(1, "name"), result.get(0).get("COL2").<BlobTest>toBlob());

        BlobTest updBlob = new BlobTest(2, "cenas");

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(updBlob);

        Update upd = update(table("TEST")).set(eq(column("COL2"), lit("?"))).where(eq(column("COL1"), k("CENINHAS")));
        engine.createPreparedStatement("testBlob", upd);
        engine.setParameter("testBlob", 1, bos.toByteArray());
        engine.executePSUpdate("testBlob");
        result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(updBlob, result.get(0).get("COL2").<BlobTest>toBlob());
    }

    @Test
    public void testBlobByteArray() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", BLOB)
                .build();

        engine.addEntity(entity);

        // 10 mb
        byte[] bb = new byte[1024 * 1024 * 10];
        byte[] bb2 = new byte[1024 * 1024 * 10];
        for (int i = 0; i < bb.length; i++) {
            bb[i] = (byte) (Math.random() * 128);
            bb2[i] = (byte) (Math.random() * 64);
        }

        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", bb)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertArrayEquals(bb, result.get(0).get("COL2").toBlob());

        Update upd = update(table("TEST")).set(eq(column("COL2"), lit("?"))).where(eq(column("COL1"), k("CENINHAS")));

        engine.createPreparedStatement("upd", upd);

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(bb2);

        engine.setParameters("upd", bos.toByteArray());

        engine.executePSUpdate("upd");

        result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertArrayEquals(bb2, result.get(0).get("COL2").toBlob());

    }

    @Test
    public void testBlobString() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", BLOB)
                .build();

        engine.addEntity(entity);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4000; i++) {
            sb.append("a");
        }

        String bigString = sb.toString();
        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", bigString)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(bigString, result.get(0).get("COL2").<String>toBlob());
    }

    @Test
    public void testBlobJSON() throws DatabaseEngineException {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", BLOB)
                .build();

        engine.addEntity(entity);

        String bigString = "[{\"type\":\"placeholder\",\"conf\":{},\"row\":0,\"height\":280,\"width\":12}]";
        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", bigString)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(bigString, result.get(0).get("COL2").<String>toBlob());
    }

    @Test
    public void addDropColumnWithDropCreateTest() throws DatabaseEngineException {
        DbEntity.Builder entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", BOOLEAN)
                .addColumn("USER", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1");
        engine.addEntity(entity.build());
        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(BOOLEAN, test.get("COL2"));
        assertEquals(DOUBLE, test.get("USER"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        EntityEntry entry = entry().set("COL1", 1).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c")
                .build();
        engine.persist("TEST", entry);

        entity.removeColumn("USER");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        entry = entry().set("COL1", 2).set("COL2", true).set("COL3", 2d).set("COL4", 1L).set("COL5", "c")
                .build();
        engine.persist("TEST", entry);

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        entity.addColumn("COL6", BLOB).addColumn("COL7", DOUBLE);
        engine.updateEntity(entity.build());

        entry = entry().set("COL1", 3).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").set("COL6", new BlobTest(1, "")).set("COL7", 2d)
                .build();
        engine.persist("TEST", entry);

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));
        assertEquals(BLOB, test.get("COL6"));
        assertEquals(DOUBLE, test.get("COL7"));
    }

    @Test
    public void addDropColumnTest() throws Exception {
        DbEntity.Builder entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", BOOLEAN)
                .addColumn("USER", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1");
        engine.addEntity(entity.build());
        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(BOOLEAN, test.get("COL2"));
        assertEquals(DOUBLE, test.get("USER"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        final DatabaseEngine engine2 = this.engine.duplicate(new Properties() {
            {
                setProperty(SCHEMA_POLICY, "create");
            }
        }, true);

        EntityEntry entry = entry().set("COL1", 1).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c")
                .build();
        engine2.persist("TEST", entry);

        entity.removeColumn("USER");
        entity.removeColumn("COL2");
        engine2.updateEntity(entity.build());

        System.out.println("> " + engine2.getMetadata("TEST"));
        entry = entry().set("COL1", 2).set("COL2", true).set("COL3", 2d).set("COL4", 1L).set("COL5", "c")
                .build();
        engine2.persist("TEST", entry);

        test = engine2.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        entity.addColumn("COL6", BLOB).addColumn("COL7", DOUBLE);
        engine2.updateEntity(entity.build());

        entry = entry().set("COL1", 3).set("COL2", true).set("USER", 2d).set("COL4", 1L).set("COL5", "c").set("COL6", new BlobTest(1, "")).set("COL7", 2d)
                .build();
        engine2.persist("TEST", entry);

        test = engine2.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));
        assertEquals(BLOB, test.get("COL6"));
        assertEquals(DOUBLE, test.get("COL7"));
    }

    @Test
    public void updateEntityNoneSchemaPolicyCreatesInMemoryPreparedStmtsTest() throws DatabaseEngineException, DatabaseFactoryException {
        dropSilently("TEST");
        engine.removeEntity("TEST");

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

        properties.setProperty(SCHEMA_POLICY, "none");
        DatabaseEngine schemaNoneEngine = DatabaseFactory.getConnection(properties);

        EntityEntry entry = entry()
                .set("COL1", 1)
                .set("COL2", true)
                .set("COL3", 1d)
                .set("COL4", 1L)
                .set("COL5", "1")
                .build();

        try {
            schemaNoneEngine.persist(entity.getName(), entry);
            fail("Should throw an exception if trying to persist an entity before calling addEntity/updateEntity a first time");
        } catch (final DatabaseEngineException e) {
            assertTrue("Should fail because the entity is still unknown to this DatabaseEngine instance",
                e.getMessage().contains("Unknown entity"));
        }

        schemaNoneEngine.updateEntity(entity);

        assertTrue("DatabaseEngine should be aware of the entity even with a NONE schema policy.", schemaNoneEngine.containsEntity(entity.getName()));

        schemaNoneEngine.persist(entity.getName(), entry);
        List<Map<String, ResultColumn>> result = schemaNoneEngine.query(select(all()).from(table("TEST")));

        assertEquals("There should be only one entry in the table.", 1, result.size());

        Map<String, ResultColumn> resultEntry = result.get(0);

        assertEquals("COL1 was successfully inserted", 1, resultEntry.get("COL1").toInt().intValue());
        assertEquals("COL2 was successfully inserted", true, resultEntry.get("COL2").toBoolean());
        assertEquals("COL3 was successfully inserted", 1.0, resultEntry.get("COL3").toDouble(), 0);
        assertEquals("COL4 was successfully inserted", 1L, resultEntry.get("COL4").toLong().longValue());
        assertEquals("COL5 was successfully inserted", "1", resultEntry.get("COL5").toString());
    }

    @Test
    public void updateEntityNoneSchemaPolicyDoesntExecuteDDL() throws DatabaseFactoryException {
        dropSilently("TEST");

        properties.setProperty(SCHEMA_POLICY, "none");
        DatabaseEngine schemaNoneEngine = DatabaseFactory.getConnection(properties);

        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1")
                .build();

        try {
            schemaNoneEngine.updateEntity(entity);
            schemaNoneEngine.query(select(all()).from(table(entity.getName())));
            fail("Should have failed because updateEntity with schema policy NONE doesn't execute DDL");
        } catch (final DatabaseEngineException e) {
            // Expected exception.
        }
    }

    @Test
    public void addDropColumnNonExistentDropCreateTest() throws DatabaseEngineException {
        dropSilently("TEST");
        engine.removeEntity("TEST");

        DbEntity.Builder entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1");
        engine.updateEntity(entity.build());

        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(BOOLEAN, test.get("COL2"));
        assertEquals(DOUBLE, test.get("COL3"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.removeColumn("COL3");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.addColumn("COL6", BLOB).addColumn("COL7", DOUBLE, DbColumnConstraint.NOT_NULL);
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));
        assertEquals(BLOB, test.get("COL6"));
        assertEquals(DOUBLE, test.get("COL7"));
    }

    @Test
    public void addDropColumnNonExistentTest() throws Exception {
        dropSilently("TEST");
        engine.removeEntity("TEST");

        DatabaseEngine engine = this.engine.duplicate(new Properties() {
            {
                setProperty(SCHEMA_POLICY, "create");
            }
        }, true);

        DbEntity.Builder entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", INT)
                .addColumn("COL2", BOOLEAN)
                .addColumn("COL3", DOUBLE)
                .addColumn("COL4", LONG)
                .addColumn("COL5", STRING)
                .pkFields("COL1");
        engine.updateEntity(entity.build());

        Map<String, DbColumnType> test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(BOOLEAN, test.get("COL2"));
        assertEquals(DOUBLE, test.get("COL3"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.removeColumn("COL3");
        entity.removeColumn("COL2");
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));

        dropSilently("TEST");
        engine.removeEntity("TEST");

        entity.addColumn("COL6", BLOB).addColumn("COL7", DOUBLE, DbColumnConstraint.NOT_NULL);
        engine.updateEntity(entity.build());

        test = engine.getMetadata("TEST");
        assertEquals(INT, test.get("COL1"));
        assertEquals(LONG, test.get("COL4"));
        assertEquals(STRING, test.get("COL5"));
        assertEquals(BLOB, test.get("COL6"));
        assertEquals(DOUBLE, test.get("COL7"));
    }

    @Test
    public void testInsertNullCLOB() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", CLOB)
                .build();
        engine.addEntity(entity);

        EntityEntry entry = entry().set("COL1", "CENINHAS")
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        System.out.println(result.get(0).get("COL2"));
        assertNull(result.get(0).get("COL2").toString());
    }

    @Test
    public void testCLOB() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", CLOB)
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

        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", initialClob)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));

        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(initialClob, result.get(0).get("COL2").toString());

        Update upd = update(table("TEST")).set(eq(column("COL2"), lit("?"))).where(eq(column("COL1"), k("CENINHAS")));

        engine.createPreparedStatement("upd", upd);

        engine.setParameters("upd", updateClob);

        engine.executePSUpdate("upd");

        result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(updateClob, result.get(0).get("COL2").toString());
    }

    @Test
    public void testCLOBEncoding() throws Exception {
        DbEntity entity = dbEntity()
                .name("TEST")
                .addColumn("COL1", STRING)
                .addColumn("COL2", CLOB)
                .build();

        engine.addEntity(entity);

        String initialClob = "áãç";
        String updateClob = "áãç_áãç";

        EntityEntry entry = entry().set("COL1", "CENINHAS").set("COL2", initialClob)
                .build();

        engine.persist("TEST", entry);

        List<Map<String, ResultColumn>> result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(initialClob, result.get(0).get("COL2").toString());

        Update upd = update(table("TEST")).set(eq(column("COL2"), lit("?"))).where(eq(column("COL1"), k("CENINHAS")));

        engine.createPreparedStatement("upd", upd);

        engine.setParameters("upd", updateClob);

        engine.executePSUpdate("upd");

        result = engine.query(select(all()).from(table("TEST")));
        assertEquals("CENINHAS", result.get(0).get("COL1").toString());
        assertEquals(updateClob, result.get(0).get("COL2").toString());
    }

    @Test
    public void testPersistOverrideAutoIncrement() throws Exception {
        DbEntity entity = dbEntity()
                .name("MYTEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry ent = entry().set("COL2", "CENAS1")
                .build();
        engine.persist("MYTEST", ent);
        ent = entry().set("COL2", "CENAS2")
                .build();
        engine.persist("MYTEST", ent);

        ent = entry().set("COL2", "CENAS3").set("COL1", 3)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = entry().set("COL2", "CENAS5").set("COL1", 5)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = entry().set("COL2", "CENAS6")
                .build();
        engine.persist("MYTEST", ent);

        ent = entry().set("COL2", "CENAS7")
                .build();
        engine.persist("MYTEST", ent);

        final List<Map<String, ResultColumn>> query = engine.query("SELECT * FROM " + quotize("MYTEST", engine.escapeCharacter()));
        for (Map<String, ResultColumn> stringResultColumnMap : query) {
            assertTrue(stringResultColumnMap.get("COL2").toString().endsWith(stringResultColumnMap.get("COL1").toString()));
        }
        engine.close();
    }

    @Test
    public void testPersistOverrideAutoIncrement2() throws Exception {
        String APP_ID = "APP_ID";
        DbColumn APP_ID_COLUMN = new DbColumn.Builder().name(APP_ID).type(INT).build();
        String STM_TABLE = "FDZ_APP_STREAM";
        String STM_ID = "STM_ID";
        String STM_NAME = "STM_NAME";
        DbEntity STREAM = dbEntity().name(STM_TABLE)
                .addColumn(APP_ID_COLUMN)
                .addColumn(STM_ID, INT, true)
                .addColumn(STM_NAME, STRING, NOT_NULL)
                .pkFields(STM_ID, APP_ID)
                .build();

        engine.addEntity(STREAM);

        EntityEntry ent = entry().set(APP_ID, 1).set(STM_ID, 1).set(STM_NAME, "NAME1")
                .build();
        engine.persist(STM_TABLE, ent);

        ent = entry().set(APP_ID, 2).set(STM_ID, 1).set(STM_NAME, "NAME1")
                .build();
        engine.persist(STM_TABLE, ent, false);

        ent = entry().set(APP_ID, 2).set(STM_ID, 2).set(STM_NAME, "NAME2")
                .build();
        engine.persist(STM_TABLE, ent);

        ent = entry().set(APP_ID, 1).set(STM_ID, 10).set(STM_NAME, "NAME10")
                .build();
        engine.persist(STM_TABLE, ent, false);

        ent = entry().set(APP_ID, 1).set(STM_ID, 2).set(STM_NAME, "NAME11")
                .build();
        engine.persist(STM_TABLE, ent);

        ent = entry().set(APP_ID, 2).set(STM_ID, 11).set(STM_NAME, "NAME11")
                .build();
        engine.persist(STM_TABLE, ent, false);

        final List<Map<String, ResultColumn>> query = engine.query(select(all()).from(table(STM_TABLE)));
        for (Map<String, ResultColumn> stringResultColumnMap : query) {
            System.out.println(stringResultColumnMap);
            assertTrue("Assert Stream Name with id", stringResultColumnMap.get(STM_NAME).toString().endsWith(stringResultColumnMap.get(STM_ID).toString()));
        }
    }

    @Test
    public void testPersistOverrideAutoIncrement3() throws Exception {
        DbEntity entity = dbEntity()
                .name("MYTEST")
                .addColumn("COL1", INT, true)
                .addColumn("COL2", STRING)
                .build();

        engine.addEntity(entity);

        EntityEntry ent = entry().set("COL2", "CENAS1").set("COL1", 1)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = entry().set("COL2", "CENAS2")
                .build();
        engine.persist("MYTEST", ent);

        ent = entry().set("COL2", "CENAS5").set("COL1", 5)
                .build();
        engine.persist("MYTEST", ent, false);

        ent = entry().set("COL2", "CENAS6")
                .build();
        engine.persist("MYTEST", ent);

        final List<Map<String, ResultColumn>> query = engine.query("SELECT * FROM " + quotize("MYTEST", engine.escapeCharacter()));
        for (Map<String, ResultColumn> stringResultColumnMap : query) {
            System.out.println(stringResultColumnMap);
            assertTrue(stringResultColumnMap.get("COL2").toString().endsWith(stringResultColumnMap.get("COL1").toString()));
        }
        engine.close();
    }

    @Test
    public void testTruncateTable() throws Exception {
        create5ColumnsEntity();

        engine.persist("TEST", entry().set("COL1", 5)
                .build());

        Truncate truncate = new Truncate(table("TEST"));

        engine.executeUpdate(truncate);

        final List<Map<String, ResultColumn>> test = engine.query(select(all()).from(table("TEST")));
        assertTrue("Test truncate query empty?", test.isEmpty());
    }

    @Test
    public void testRenameTables() throws Exception {
        String oldName = "TBL_OLD";
        String newName = "TBL_NEW";

        dropSilently(oldName, newName);

        DbEntity entity = dbEntity()
                .name(oldName)
                .addColumn("timestamp", INT)
                .build();
        engine.addEntity(entity);
        engine.persist(oldName, entry().set("timestamp", 20)
                .build());

        Rename rename = new Rename(table(oldName), table(newName));
        engine.executeUpdate(rename);

        final Map<String, DbColumnType> metaMap = new LinkedHashMap<>();
        metaMap.put("timestamp", INT);
        assertEquals("Metamap ok?", metaMap, engine.getMetadata(newName));

        List<Map<String, ResultColumn>> resultSet = engine.query(select(all()).from(table(newName)));
        assertEquals("Count ok?", 1, resultSet.size());

        assertEquals("Content ok?", 20, (int) resultSet.get(0).get("timestamp").toInt());

        dropSilently(newName);
    }

    private void dropSilently(String... tables) {
        for (String table : tables) {
            try {
                engine.dropEntity(dbEntity().name(table).build());
            } catch (final Throwable e) {
                // ignore
            }
        }
    }

    @Test
    public void testLikeWithTransformation() throws Exception {
        create5ColumnsEntity();
        engine.persist("TEST", entry().set("COL1", 5).set("COL5", "teste")
                .build());
        engine.persist("TEST", entry().set("COL1", 5).set("COL5", "TESTE")
                .build());
        engine.persist("TEST", entry().set("COL1", 5).set("COL5", "TeStE")
                .build());
        engine.persist("TEST", entry().set("COL1", 5).set("COL5", "tesTte")
                .build());

        List<Map<String, ResultColumn>> query = engine.query(
            select(all()).from(table("TEST")).where(like(udf("lower", column("COL5")), k("%teste%")))
        );
        assertEquals(3, query.size());
        query = engine.query(select(all()).from(table("TEST")).where(like(udf("lower", column("COL5")), k("%tt%"))));
        assertEquals(1, query.size());
    }

    @Test
    public void createSequenceOnLongColumnTest() throws Exception {
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
        List<Map<String, ResultColumn>> test = engine.query(select(all()).from(table("TEST")));
        assertEquals("col1 ok?", 1, (int) test.get(0).get("COL1").toInt());
        assertTrue("col2 ok?", test.get(0).get("COL2").toBoolean());
        assertEquals("col4 ok?", 1L, (long) test.get(0).get("COL4").toLong());
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

        engine.persist("TEST", entry().set("COL1", 1).set("COL2", true)
                .build());
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

    @Test
    public void closingAnEngineUsingTheCreateDropPolicyShouldDropAllEntities()
            throws DatabaseEngineException, DatabaseFactoryException {

        properties.setProperty(SCHEMA_POLICY, "create-drop");
        engine = DatabaseFactory.getConnection(properties);

        engine.addEntity(buildEntity("ENTITY-1"));
        engine.addEntity(buildEntity("ENTITY-2"));

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
            engine.persist("TEST", entry().set("COL1", i).build());
        }

        final ResultIterator resultIterator = engine.iterator(select(all()).from(table("TEST")));

        assertEquals("The current row count should be 0 if the iteration hasn't started", 0, resultIterator.getCurrentRowCount());

        resultIterator.next();

        assertEquals("The current row count is equal to 1", 1, resultIterator.getCurrentRowCount());

        for (int i = 0; i < 3; i++) {
            resultIterator.nextResult();
        }

        assertEquals("The current row count is equal to 4", 4, resultIterator.getCurrentRowCount());
    }

    @Test
    public void kEnumTest() throws DatabaseEngineException {
        create5ColumnsEntity();

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
                .extracting(element -> element.get("COL5").toString())
                .as("An enum value should be persisted as its string representation")
                .isEqualTo(TestEnum.TEST_ENUM_VAL.name());
    }

    @Test
    public void insertDuplicateDBError() throws Exception {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = entry().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        engine.persist("TEST", entry);
        assertThatCode(() -> engine.persist("TEST", entry))
                .as("Is unique constraint violation exception")
                .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
                .as("Encapsulated exception is SQLException")
                .hasCauseInstanceOf(SQLException.class)
                .hasMessage("Something went wrong persisting the entity [unique_constraint_violation]");
    }

    @Test
    public void batchInsertDuplicateDBError() throws DatabaseEngineException {
        create5ColumnsEntityWithPrimaryKey();

        EntityEntry entry = entry().set("COL1", 2)
                                   .set("COL2", false)
                                   .set("COL3", 2D)
                                   .set("COL4", 3L)
                                   .set("COL5", "ADEUS")
                                   .build();

        engine.addBatch("TEST", entry);
        engine.addBatch("TEST", entry);

        assertThatCode(() -> engine.flush())
                .as("Is unique constraint violation exception")
                .isInstanceOf(DatabaseEngineUniqueConstraintViolationException.class)
                .as("Encapsulated exception is SQLException")
                .hasCauseInstanceOf(SQLException.class)
                .hasMessage("Something went wrong while flushing [unique_constraint_violation]");
    }

    private enum TestEnum {
        TEST_ENUM_VAL;

        @Override
        public String toString() {
            return super.toString() + " description";
        }
    }
}
