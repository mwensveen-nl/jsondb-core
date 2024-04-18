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

import io.jsondb.JsonDBException;
import io.jsondb.crypto.CryptoUtil;
import io.jsondb.crypto.Default1Cipher;
import io.jsondb.crypto.ICipher;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class Default1CipherTests {

    @Test
    public void testKeyValidityAesGcm() throws UnsupportedEncodingException, GeneralSecurityException {
        Default1Cipher default1Cipher = new Default1Cipher("badkey", "UTF-8");
        assertNotNull(default1Cipher);
    }

    @Test
    public void testKeyAesGcm() throws UnsupportedEncodingException, GeneralSecurityException {
        String base64EncodedKey = CryptoUtil.generate128BitKey("MyPassword", "ksdfkja923u4anf");
        base64EncodedKey = CryptoUtil.generate128BitKey("MyPassword", "ksdfkja923u4anf");
        assertEquals("jCt039xT0eUwkIqAWACw/w==", base64EncodedKey);
        ICipher cipher = new Default1Cipher(base64EncodedKey);

        String encryptedText = cipher.encrypt("Hallo, Wie gehts");
        // assertEquals("dy4tf8d4HNkbfI9dPY4DPldga+RA+lTknLSZwajanyTjuGkqQGf5MfUDYhSgWMF1", encryptedText);
        System.out.println(encryptedText);
        String decryptedText = cipher.decrypt(encryptedText);
        assertEquals("Hallo, Wie gehts", decryptedText);

        decryptedText = cipher.decrypt("dy4tf8d4HNkbfI9dPY4DPldga+RA+lTknLSZwajanyTjuGkqQGf5MfUDYhSgWMF1");
        assertEquals("Hallo, Wie gehts", decryptedText);
    }

    @Test
    public void testKeyValidityDefault1() throws UnsupportedEncodingException, GeneralSecurityException {
        Default1Cipher cipher = new Default1Cipher("badkey", StandardCharsets.UTF_8);
        JsonDBException exception = assertThrows(JsonDBException.class, () -> cipher.encrypt("Hallo, Wie gehts"));
        assertTrue(exception.getCause() instanceof InvalidKeyException);
    }

    @Test
    public void testKeyDefault1() throws UnsupportedEncodingException, GeneralSecurityException {
        String base64EncodedKey = CryptoUtil.generate128BitKey("MyPassword", "ksdfkja923u4anf");
        ICipher cipher = new Default1Cipher(base64EncodedKey);

        String msg = "Hallo, gru\u00DF";

        String encryptedText = cipher.encrypt(msg);
        String decryptedText = cipher.decrypt(encryptedText);
        assertEquals(msg, decryptedText);
        assertTrue(encryptedText.length() > 40, "ciphertext too short");
    }
}
