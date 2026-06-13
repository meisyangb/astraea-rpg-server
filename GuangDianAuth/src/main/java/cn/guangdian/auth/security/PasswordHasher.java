package cn.guangdian.auth.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {

    private static final int SALT_LENGTH = 32;
    private static final int ITERATIONS = 10000;
    private final SecureRandom random = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            byte[] passwordBytes = password.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            
            md.update(saltBytes);
            byte[] hash = md.digest(passwordBytes);
            
            for (int i = 0; i < ITERATIONS; i++) {
                md.reset();
                md.update(saltBytes);
                hash = md.digest(hash);
            }
            
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean verify(String password, String hash, String salt) {
        String computed = hash(password, salt);
        return computed.equals(hash);
    }
}
