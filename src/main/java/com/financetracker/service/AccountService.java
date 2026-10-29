package com.financetracker.service;

import com.financetracker.model.UserAccount;
import com.financetracker.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private final UserAccountRepository users;
    // Bounded, per-address failure windows prevent unlimited repeated password guesses.
    private final Map<String, Attempts> attempts = new java.util.concurrent.ConcurrentHashMap<>();

    public AccountService(UserAccountRepository users) {
        this.users = users;
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] hash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 600000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        } finally {
            spec.clearPassword();
        }
    }

    @Transactional
    public UserAccount register(String email, String name, String password) {
        email = normalize(email);
        if (!email.matches("[^ @]+@[^ @]+[.][^ @]+") || email.chars().anyMatch(Character::isWhitespace) || email.length() > 254)
            throw new IllegalArgumentException("Enter a valid email address.");
        if (password.length() < 12 || password.length() > 128)
            throw new IllegalArgumentException("Use a password between 12 and 128 characters.");
        if (users.findByEmail(email).isPresent())
            throw new IllegalArgumentException("Unable to create account. Try signing in instead.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setDisplayName(name == null || name.isBlank() ? "there" : name.trim().substring(0, Math.min(name.trim().length(), 80)));
        user.setPasswordHash(Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash(password, salt)));
        return users.save(user);
    }

    public synchronized UserAccount login(String email, String password) {
        email = normalize(email);
        long now = System.currentTimeMillis();
        attempts.entrySet().removeIf(e -> e.getValue().until() < now);
        Attempts previous = attempts.get(email);
        if (previous != null && previous.count() >= 5)
            throw new IllegalArgumentException("Too many attempts. Try again in 15 minutes.");
        UserAccount user = users.findByEmail(email).orElse(null);
        String encoded = user == null ? "AAAAAAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" : user.getPasswordHash();
        String[] parts = encoded.split(":");
        byte[] computed = hash(password.length() > 128 ? "invalid" : password, Base64.getDecoder().decode(parts[0]));
        if (user == null || !MessageDigest.isEqual(computed, Base64.getDecoder().decode(parts[1]))) {
            if (attempts.size() > 10000) attempts.clear();
            attempts.put(email, new Attempts(previous == null ? 1 : previous.count() + 1, previous == null ? now + 900000 : previous.until()));
            throw new IllegalArgumentException("Email or password is incorrect.");
        }
        attempts.remove(email);
        return user;
    }

    private record Attempts(int count, long until) {
    }
}
