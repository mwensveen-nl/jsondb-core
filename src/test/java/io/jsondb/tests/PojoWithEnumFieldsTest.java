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
import io.jsondb.JsonDBTemplate;
import io.jsondb.tests.model.PojoWithEnumFields;
import io.jsondb.tests.model.PojoWithEnumFields.Status;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Farooq Khan
 * @version 1.0 06-Oct-2016
 */
public class PojoWithEnumFieldsTest {
    private static final String POJOWITHENUMFIELDS_JSON = "pojowithenumfields.json";
    @TempDir
    private File dbFilesFolder;
    private JsonDBTemplate jsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(new File("src/test/resources/dbfiles/pojowithenumfields.json"), new File(dbFilesFolder, POJOWITHENUMFIELDS_JSON));
        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model");
    }

    @Test
    public void testFind() {
        PojoWithEnumFields clazz = jsonDBTemplate.findById("0001", PojoWithEnumFields.class);

        assertNotNull(clazz);

        assertEquals(clazz.getStatus(), Status.CREATED);
    }

    @Test
    public void testInsert() {
        List<PojoWithEnumFields> clazzs = jsonDBTemplate.getCollection(PojoWithEnumFields.class);
        int size = clazzs.size();

        PojoWithEnumFields clazz = new PojoWithEnumFields();
        clazz.setId("0010");
        clazz.setStatus(Status.UPDATED);
        jsonDBTemplate.insert(clazz);

        clazzs = jsonDBTemplate.getCollection(PojoWithEnumFields.class);
        assertNotNull(clazzs);
        assertEquals(clazzs.size(), size + 1);
    }
}
