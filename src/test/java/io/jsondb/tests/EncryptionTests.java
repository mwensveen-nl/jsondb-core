/*
 * Copyright (c) 2016 - 2018 Farooq Khan
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
import io.jsondb.tests.util.TestUtils;
import java.io.File;
import java.security.GeneralSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the encryption functionality
 * 
 * @version 1.0 22-Oct-2016
 */
@SuppressWarnings("deprecation")
public class EncryptionTests {
    private static final String INSTANCES_JSON = "instances.json";
    @TempDir
    private File dbFilesFolder;

    @TempDir
    private File dbFilesFolder2;

    private JsonDBTemplate jsonDBTemplate = null;
    private JsonDBTemplate unencryptedjsonDBTemplate = null;

    @BeforeEach
    public void setUp() throws Exception {
        Files.copy(new File("src/test/resources/dbfiles/instances.json"), new File(dbFilesFolder, INSTANCES_JSON));
        ICipher cipher = new Default1Cipher("1r8+24pibarAWgS85/Heeg==");

        jsonDBTemplate = new JsonDBTemplate(dbFilesFolder.getAbsolutePath(), "io.jsondb.tests.model", cipher);

        unencryptedjsonDBTemplate = new JsonDBTemplate(dbFilesFolder2.getAbsolutePath(), "io.jsondb.tests.model");
    }

    @Test
    public void encryptionTest() {
        Instance instance = new Instance();
        instance.setId("11");
        instance.setHostname("ec2-54-191-11");
        instance.setPrivateKey("b87eb02f5dd7e5232d7b0fc30a5015e4");
        instance.setPublicKey("d3aa045f71bf4d1dffd2c5f485a4bc1d");
        jsonDBTemplate.insert(instance);

        String lastLine = TestUtils.lastLine(new File(dbFilesFolder, INSTANCES_JSON));
        assertTrue(lastLine.startsWith("{\"id\":\"11\",\"hostname\":\"ec2-54-191-11\",\"privateKey\":"));
        assertTrue(lastLine.endsWith(",\"publicKey\":\"d3aa045f71bf4d1dffd2c5f485a4bc1d\"}"));
        assertFalse(lastLine.contains("b87eb02f5dd7e5232d7b0fc30a5015e4"));

        Instance i = jsonDBTemplate.findById("11", "instances");
        assertEquals("b87eb02f5dd7e5232d7b0fc30a5015e4", i.getPrivateKey());
    }

    @Test
    public void changeEncryptionTest() {
        ICipher newCipher = null;
        try {
            newCipher = new Default1Cipher("jCt039xT0eUwkIqAWACw/w==");
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        jsonDBTemplate.changeEncryption(newCipher);

        String lastLine = TestUtils.lastLine(new File(dbFilesFolder, INSTANCES_JSON));
        assertTrue(lastLine.startsWith("{\"id\":\"06\",\"hostname\":\"ec2-54-191-06\",\"privateKey\":"));
        assertTrue(lastLine.endsWith(",\"publicKey\":\"\"}"));
        assertFalse(lastLine.contains("vr90J53rB/gXDb7XfALayqYXcVxHUT4eU+HqsTcpCI2rEmeeqwsHXEnpZxF4rzRCfDZs7NzSODRkPGgOHWmslQ=="));

        Instance i = jsonDBTemplate.findById("01", "instances");
        assertEquals("b87eb02f5dd7e5232d7b0fc30a5015e4", i.getPrivateKey());
    }

    @Test
    public void changeEncryptionTest2() throws GeneralSecurityException {
        ICipher newCipher = new Default1Cipher("jCt039xT0eUwkIqAWACw/w==");

        InvalidJsonDbApiUsageException exception = assertThrows(InvalidJsonDbApiUsageException.class, () -> unencryptedjsonDBTemplate.changeEncryption(newCipher));
        assertEquals("DB is not encrypted, nothing to change for EncryptionKey", exception.getMessage());
    }
}
