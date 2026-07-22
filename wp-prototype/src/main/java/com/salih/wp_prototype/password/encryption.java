package com.salih.wp_prototype.password;

import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class encryption {
    private static final String SECRET_KEY = "PasswordKey12345";

    public static String encrypt(String beforencryPassword) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encryptedBytes = cipher.doFinal(beforencryPassword.getBytes()); // cipher.dofinal aes işlemlerini
                                                                                   // yapan kısım(karıştırma)
            return Base64.getEncoder().encodeToString(encryptedBytes); // base64 ingilizce olacak şekilde harflere,
                                                                       // rakamlara çeviriyor karmaşık yapıyı

        } catch (Exception e) {
            throw new RuntimeException("Şifreleme sırasında hata oluştu!", e);
        }
    }

    public static String decrypt(String encryptedPassword) {
        try {
            SecretKeySpec key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decryptedBytes = Base64.getDecoder().decode(encryptedPassword);
            byte[] rawBytes = cipher.doFinal(decryptedBytes);

            return new String(rawBytes);
        } catch (Exception e) {
            throw new RuntimeException("Şifre çözme sırasında hata oluştu!", e);
        }
    }
}
