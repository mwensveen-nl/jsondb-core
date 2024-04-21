/*
 * Copyright (c) 2016 Farooq Khan
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jsondb.tests;

import com.google.common.io.Files;
import io.jsondb.InvalidJsonDbApiUsageException;
import io.jsondb.JsonDBTemplate;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import io.jsondb.tests.model.Instance;
import io.jsondb.tests.model.PojoWithList;
import io.jsondb.tests.model.Site;
import io.jsondb.tests.model.Volume;
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @version 1.0 08-Oct-2016
 */
public class UpsertTests {
    private static final String POJOWITHLIST_JSON = "pojowithlist.json";
    private static final String VOLUMES_JSON = "volumes.json";
    private static final String INSTANCES_JSON = "instances.json";
    @TempDir
    private File dbFilesFolder;
    private JsonDBTemplate jsonDBTemplate = null;
    private ICipher cipher;

    @BeforeEach
    public void setUp() throws Exception {
        cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);
    }

    /**
     * Test to upsert a new object into a known collection type which has no data.
     */
    @Test
    public void testUpsert_NewObjectCollectionWithNoData() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(new Site()));
        assertEquals("Collection by name 'sites' not found. Create collection first", exception.getMessage());
    }

    /**
     * Test to upsert a new object into a known collection type which has some data.
     */
    @Test
    public void testUpsert_NewObjectCollectionWithSomeData() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        Instance instance = new Instance();
        instance.setId("07");
        instance.setHostname("ec2-54-191-07");
        instance.setPrivateKey("PrivateRyanSaved");
        instance.setPublicKey("TomHanks");
        jsonDBTemplate.upsert(instance);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size + 1, instances.size());
    }

    private class SomeClass {
        // intentionately empty
    }

    /**
     * Test to upsert a new object of unknown collection type.
     */
    @Test
    public void testUpsert_NewObjectUnknownCollection() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(new SomeClass()));
        assertEquals("Entity 'SomeClass' is not annotated with annotation @Document", exception.getMessage());
    }

    /**
     * Test to upsert a null object.
     */
    @Test
    public void testUpsert_Null_1() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(null));
        assertEquals("Null Object cannot be upserted into DB", exception.getMessage());
    }

    /**
     * Test to upsert a null object.
     */
    @Test
    public void testUpsert_Null_2() {
        Object nullObject = null;
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(nullObject, "instances"));
        assertEquals("Null Object cannot be upserted into DB", exception.getMessage());
    }

    /**
     * Test to upsert a null object.
     */
    @Test
    public void testUpsert_Null_3() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(null, "instances"));
        assertEquals("Null Object batch cannot be upserted into DB", exception.getMessage());
    }

    /**
     * Test to upsert a Collection object.
     */
    @Test
    public void testUpsert_CollectionObject() {
        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(new HashSet<>()));
        assertEquals("Collection object cannot be inserted, removed, updated or upserted as a single object", exception.getMessage());
    }

    /**
     * Test to upsert a new object with a ID that already exists..
     */
    @Test
    public void testUpsert_ObjectWithExistingId() {
        Instance instance1 = jsonDBTemplate.findById("03", Instance.class);
        assertNotNull(instance1);
        assertEquals("03", instance1.getId());
        assertEquals("ec2-54-191-04", instance1.getHostname());
        assertEquals("b87eb02f5dd7e5232d7b0fc30a5015e4", instance1.getPrivateKey());
        assertEquals("d3aa045f71bf4d1dffd2c5f485a4bc1d", instance1.getPublicKey());

        Instance instance2 = new Instance();
        instance2.setId("03");
        instance2.setHostname("ec2-54-191-04_Updated");
        instance2.setPrivateKey("SavingPrivateRyan");
        instance2.setPublicKey("VeryPublic");
        jsonDBTemplate.upsert(instance2);

        Instance instance3 = jsonDBTemplate.findById("03", Instance.class);
        assertNotNull(instance3);
        assertEquals("03", instance3.getId());
        assertEquals("ec2-54-191-04_Updated", instance3.getHostname());
        assertEquals("SavingPrivateRyan", instance3.getPrivateKey());
        assertEquals("VeryPublic", instance3.getPublicKey());
    }

    /**
     * Test to upsert a new object into a collection and verify the actual file output.
     */
    @Test
    public void testUpsert_InsertAndVerify() {
        List<Volume> vols = jsonDBTemplate.getCollection(Volume.class);
        int size = vols.size();

        Volume vol = new Volume();
        vol.setId("000001");
        vol.setName("c:");
        vol.setSize(102400000000L);
        vol.setFlash(true);
        jsonDBTemplate.upsert(vol);

        vols = jsonDBTemplate.getCollection(Volume.class);
        assertNotNull(vols);
        assertEquals(size + 1, vols.size());

        String[] expectedLinesAtEnd = { "{\"id\":\"000001\",\"name\":\"c:\",\"size\":102400000000,\"flash\":true}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, VOLUMES_JSON), expectedLinesAtEnd);
    }

    /**
     * Test to upsert a collection of objects.
     */
    @Test
    public void testUpsert_CollectionOfObjects() {
        List<Volume> vols = jsonDBTemplate.getCollection(Volume.class);
        int size = vols.size();

        List<Volume> newList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Volume vol = new Volume();
            int id = 2 + i;
            vol.setId(String.format("%06d", id));
            vol.setName("c:");
            vol.setSize(10240000L * i);
            vol.setFlash(i % 2 == 0);
            newList.add(vol);
        }
        jsonDBTemplate.upsert(newList, Volume.class);

        vols = jsonDBTemplate.getCollection(Volume.class);
        assertNotNull(vols);
        assertEquals(size + 5, vols.size());

        String[] expectedLinesAtEnd = {
                "{\"schemaVersion\":\"1.0\"}",
                "{\"id\":\"000002\",\"name\":\"c:\",\"size\":0,\"flash\":true}",
                "{\"id\":\"000003\",\"name\":\"c:\",\"size\":10240000,\"flash\":false}",
                "{\"id\":\"000004\",\"name\":\"c:\",\"size\":20480000,\"flash\":true}",
                "{\"id\":\"000005\",\"name\":\"c:\",\"size\":30720000,\"flash\":false}",
                "{\"id\":\"000006\",\"name\":\"c:\",\"size\":40960000,\"flash\":true}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, VOLUMES_JSON), expectedLinesAtEnd);
    }

    /**
     * Test to upsert a collection of objects with one of them already present in DB.
     */
    @Test
    public void testUpsert_CollectionOfObjectsWithOnePresent() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        List<Instance> newList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Instance c = new Instance();
            int id = 21 + i;
            c.setId(String.format("%02d", id));
            newList.add(c);
        }
        Instance instance = new Instance();
        instance.setId("03");
        newList.add(instance);

        jsonDBTemplate.upsert(newList, Instance.class);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size + 5, instances.size());
    }

    /**
     * Test to upsert a collection of objects with duplicate.
     */
    @Test
    public void testUpsert_CollectionOfObjectsWithDuplicate() {
        List<Instance> newList = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Instance c = new Instance();
            c.setId("000028");
            newList.add(c);
        }

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(newList, Instance.class));
        assertEquals("Duplicate object with id: 000028 within the passed in parameter", exception.getMessage());
    }

    /**
     * Test to upsert a collection of objects of unknown type into DB.
     */
    @Test
    public void testUpsert_ObjectsOfUnknownType() {
        List<Site> newList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Site c = new Site();
            int id = 0 + i;
            c.setId(String.format("%06d", id));
            newList.add(c);
        }

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> jsonDBTemplate.upsert(newList, Site.class));
        ;
        assertEquals("Collection by name 'sites' not found. Create collection first", exception.getMessage());
    }

    /**
     * Test to upsert objects without a ID set.
     */
    @Test
    public void testInsert_ObjectWithoutId() {
        List<Instance> instances = jsonDBTemplate.getCollection(Instance.class);
        int size = instances.size();

        Instance instance = new Instance();
        instance.setHostname("VplexServer");
        instance.setPrivateKey("PrivateNetwork");
        instance.setPublicKey("PublicNetwork");
        jsonDBTemplate.upsert(instance);

        instances = jsonDBTemplate.getCollection(Instance.class);
        assertNotNull(instances);
        assertEquals(size + 1, instances.size());
    }

    @Test
    public void testUpsert_ObjectWithList() {
        jsonDBTemplate.createCollection(PojoWithList.class);
        PojoWithList bean = new PojoWithList();
        bean.setId("000002");
        List<String> stuff = new ArrayList<>(Arrays.asList("A", "B"));
        bean.setStuff(stuff);

        jsonDBTemplate.upsert(bean);

        String[] expectedLinesAtEnd = {
                "{\"schemaVersion\":\"1.0\"}",
                "{\"id\":\"000002\",\"stuff\":[\"A\",\"B\"]}" };

        TestUtils.checkLastLines(new File(dbFilesFolder, POJOWITHLIST_JSON), expectedLinesAtEnd);
    }
}
