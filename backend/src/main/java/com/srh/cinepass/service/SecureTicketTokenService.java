package com.srh.cinepass.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SecureTicketTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final int REFERENCE_LENGTH = 12;
    private static final String REFERENCE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";

    /**
     * Base64 URL-safe, no padding. 32 random bytes produce 43 characters.
     */
    public static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    private final SecureRandom secureRandom = new SecureRandom();

    public String newVerificationToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String newTicketReference() {
        StringBuilder reference = new StringBuilder("SRH-");

        for (int i = 0; i < REFERENCE_LENGTH; i++) {
            reference.append(REFERENCE_ALPHABET.charAt(secureRandom.nextInt(REFERENCE_ALPHABET.length())));
        }

        return reference.toString().toUpperCase(Locale.ROOT);
    }

    public boolean isWellFormedToken(String token) {
        return token != null && TOKEN_PATTERN.matcher(token).matches();
    }
}
