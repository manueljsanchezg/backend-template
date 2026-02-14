package com.example.demo.Jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-at}")
    private long expirationAt;

    @Value("${jwt.expiration-rt}")
    private long expirationRt;

    public SecretKey getSecretKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKey);
        return Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateAccessTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .expiration(new Date(new Date().getTime() + expirationAt))
                .issuedAt(new Date())
                .signWith(getSecretKey())
                .compact();
    }

    public String generateRefreshTokenFromUsername(String username) {
        return Jwts.builder()
                .subject(username)
                .expiration(new Date(new Date().getTime() + expirationRt))
                .issuedAt(new Date())
                .signWith(getSecretKey())
                .compact();
    }

    public Date getExpirarationFromToken(String token) {
        return  Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
    }

    public String getUsernameFromToken(String token) {
        return  Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean verifyToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
