package com.srh.cinepass.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String SECRET_KEY = "SRHCinePassSuperSecretKeyForJWTAuthentication2026";

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 24 hours

    // =========================
    // CREATE SECRET KEY
    // =========================

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    // =========================
    // GENERATE TOKEN
    // =========================

    public String generateToken(String email) {

        Date currentDate = new Date();

        Date expirationDate = new Date(
                currentDate.getTime()
                        + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(email)
                .issuedAt(currentDate)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    // =========================
    // EXTRACT EMAIL
    // =========================

    public String extractEmail(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    // =========================
    // VALIDATE TOKEN
    // =========================

    public boolean isTokenValid(
            String token,
            String email) {

        try {

            String tokenEmail = extractEmail(token);

            return tokenEmail.equals(email)
                    && !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }

    // =========================
    // CHECK EXPIRATION
    // =========================

    private boolean isTokenExpired(String token) {

        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims
                .getExpiration()
                .before(new Date());
    }
}