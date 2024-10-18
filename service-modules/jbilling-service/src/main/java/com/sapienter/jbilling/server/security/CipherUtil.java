package com.sapienter.jbilling.server.security;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;


/**
 * Created by faizan on 8/21/17.
 */
public class CipherUtil {
    private static final String ALGO = "AES";
    private static final String ENCRYPTION_ALGO = "AES/ECB/PKCS5Padding";
    private static final String ENCODING_FORMAT = "UTF-8";
    private static byte[] keyValue = {
            0x74, 0x68, 0x69, 0x73, 0x49, 0x73, 0x41, 0x53, 0x65, 0x63, 0x72, 0x65, 0x74, 0x4b, 0x65, 0x79
    };

    public static char[] encrypt(char[] Data) throws Exception {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        SecretKeySpec secretKey = new SecretKeySpec(keyValue, ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encVal = cipher.doFinal(charsToBytes(Data));
        byte[] encryptedBytes = Base64.getEncoder().encode(encVal);
        char[] encryptedValue = bytesToChars(encryptedBytes);
        Arrays.fill(encryptedBytes, (byte) 0); // clear sensitive data
        Arrays.fill(encVal, (byte) 0); // clear sensitive data
        return encryptedValue;
    }

    public static char[] decrypt(char[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGO);
        SecretKeySpec secretKey = new SecretKeySpec(keyValue, ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decordedValue = Base64.getDecoder().decode(charsToBytes(encryptedData));
        byte[] decValue = cipher.doFinal(decordedValue);
        char[] decryptedValue = bytesToChars(decValue);
        Arrays.fill(decordedValue, (byte) 0); // clear sensitive data
        Arrays.fill(decValue, (byte) 0); // clear sensitive data
        return decryptedValue;
    }

    public static byte[] charsToBytes(char[] chars){
        Charset charset = Charset.forName(ENCODING_FORMAT);
        ByteBuffer byteBuffer = charset.encode(CharBuffer.wrap(chars));
        byte[] bytes = Arrays.copyOf(byteBuffer.array(), chars.length);
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    public static char[] bytesToChars(byte[] bytes){
        Charset charset = Charset.forName(ENCODING_FORMAT);
        CharBuffer charBuffer = charset.decode(ByteBuffer.wrap(bytes));
        char[] chars = Arrays.copyOf(charBuffer.array(), bytes.length);
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        return chars;
    }

    public static boolean isValidEncryptedString(char[] data){
        try {
            return new String(encrypt(decrypt(data))).equals(new String(data));
        }
        catch (Exception e) {
            return false;
        }
    }
}
