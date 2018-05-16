package no.nels.commons.utilities;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class SecurityUtilities {

    private static String getCipherKey(String key) {
        try {
            return DigestUtils.md5Hex(key).toString().substring(0, 16);
        } catch (Exception ex) {
            return null;
        }
    }

    public static String encrypt(String key, String plainText) {
        //1. get derived key  2. encrypt using derived key  3. base64 encode 4. toHexString
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(getCipherKey(key).getBytes(), "AES"));
            return StringUtilities.bytesToHex(Base64.encodeBase64(cipher.doFinal(plainText.getBytes())));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }

    public static String decypt(String key, String cipherText) {
        //1. fromHexString 2. base64 decode  3. get derived key  4. decrypt using derived key
        try {
            byte[] base64Decoded = Base64.decodeBase64(StringUtilities.hexToBytes(cipherText));
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(getCipherKey(key).getBytes(), "AES"));
            return new String(cipher.doFinal(base64Decoded));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
}
