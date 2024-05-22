package net.cofcool.sourcebox.util;

import io.vertx.core.Future;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import lombok.SneakyThrows;

public abstract class Utils {

    public static String desdeEncrypt(String key, String src) {
        try {
            return Base64.getEncoder().encodeToString(encrypt(src, key));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String desdeDecrypt(String key, String src) {
        try {
            return decrypt(Base64.getDecoder().decode(src), key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] encrypt(String message, String secretKey) throws Exception {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        KeySpec keySpec = new DESedeKeySpec(keyBytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
        SecretKey key = keyFactory.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        return cipher.doFinal(messageBytes);
    }

    private static String decrypt(byte[] encryptedMessage, String secretKey) throws Exception {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        KeySpec keySpec = new DESedeKeySpec(keyBytes);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
        SecretKey key = keyFactory.generateSecret(keySpec);

        Cipher cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);

        byte[] decryptedBytes = cipher.doFinal(encryptedMessage);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String md5(String val) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messageDigest.reset();
        messageDigest.update(val.getBytes(StandardCharsets.UTF_8));

        byte[] byteArray = messageDigest.digest();

        StringBuilder md5StrBuff = new StringBuilder();

        for (byte b : byteArray) {
            if (Integer.toHexString(0xFF & b).length() == 1) {
                md5StrBuff.append("0").append(Integer.toHexString(0xFF & b));
            } else {
                md5StrBuff.append(Integer.toHexString(0xFF & b));
            }
        }

        return md5StrBuff.toString();
    }

    public static String generateShortUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static <T> T instance(Class<T> clazz) {
        try {
            return clazz.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T instance(Class<T> clazz, Object... args) {
        try {
            return clazz.getDeclaredConstructor(Arrays.stream(args).map(Object::getClass).toArray(Class[]::new)).newInstance(args);
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static <T> T getFutureResult(Future<T> future) {
        return future.toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
    }
}
