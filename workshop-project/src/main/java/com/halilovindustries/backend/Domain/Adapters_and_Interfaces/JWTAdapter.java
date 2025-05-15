package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
// import javax.crypto.KeyGenerator;
// import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Date;
import java.util.function.Function;


@Component
@Primary
public class JWTAdapter implements IAuthentication {
    //private String token;
    @Value("${jwt.secret}")
    private String secret;
    
    private final long expirationTime = 3600000; // 1 hour
    private SecretKey key;

    @PostConstruct
    public void initKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.key = new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256");
}

    // public JWTAdapter() {
    //     try {
    //         KeyGenerator keyGen = KeyGenerator.getInstance("HmacSha256");
    //         key = keyGen.generateKey();
    //     } catch (NoSuchAlgorithmException e) {
    //         throw new RuntimeException("HmacSha256 algorithm not available", e);
    //     }
    // }

    public String generateToken(String username) {
        // Set the expiration time to 24 hours from now
        JwtBuilder builder = Jwts.builder()
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis())) 
        .expiration(new Date(System.currentTimeMillis() + expirationTime))
        .signWith(key);

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Token is invalid or expired
            return false;
        }
    }

    public String getUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().
                            verifyWith(key).
                            build().
                            parseSignedClaims(token).
                            getPayload();
    }

    public void setSecret(String base64Key) {
        this.secret = base64Key;
    }
}