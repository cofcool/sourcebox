package net.cofcool.sourcebox.util;

import io.vertx.core.Future;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;

public abstract class Utils {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd HH:mm:ss");

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

    public static void zipDir(String sourceDirPath, String zipFilePath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(Paths.get(zipFilePath)))) {
            var sourceDir = Paths.get(sourceDirPath);
            var p = FilenameUtils.getName(sourceDirPath).replace(".", "");
            var dir = p + "/" + p + "/";
            zipOut.putNextEntry(new ZipEntry(dir));
            zipOut.closeEntry();
            try (var s = Files.walk(sourceDir)) {
                s.forEach(path -> {
                    try {
                        var relativePath = sourceDir.relativize(path);
                        if (!Files.isDirectory(path)) {
                            zipOut.putNextEntry(new ZipEntry(dir + relativePath));
                            Files.copy(path, zipOut);
                            zipOut.closeEntry();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error reading file: " + path + " -> " + e.getMessage(), e);
                    }
                });
            }
        }
    }

    public static <T> T requestLocalData(String port, String methodPath, Class<T> bodyType, Function<Builder, Builder> requestAction, Consumer<Exception> errorAction) {
        try (var client = HttpClient.newHttpClient()){
            var builder = requestAction.apply(
                HttpRequest
                    .newBuilder()
                    .uri(new URI("http://localhost:" + port + methodPath))
            );

            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) {
                return JsonUtil.toPojo(response.body(), bodyType);
            }
            if (errorAction != null) {
                errorAction.accept(new RuntimeException("response error code is " + response.statusCode()));
            }
        } catch (Exception e) {
            if (errorAction != null) {
                errorAction.accept(e);
            }
        }

        return null;
    }

    public static String formatDatetime(LocalDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }


    private static BufferedReader br;
    static synchronized BufferedReader reader() {
        if (br == null) {
            br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        }
        return br;
    }

    public static String readLine(String hint) {
        try {
            System.out.print("please enter " + hint + ": ");
            return reader().readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
