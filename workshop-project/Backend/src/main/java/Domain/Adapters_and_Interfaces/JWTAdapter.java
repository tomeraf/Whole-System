package Domain.Adapters_and_Interfaces;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.function.Function;


@Component
public class JWTAdapter implements IAuthentication {
    //private String token;
    @Value("${jwt.secret}")
    private String secret;
    
    private final long expirationTime = 24 * 3600000; // 24 hours
    private SecretKey key;

    public JWTAdapter() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSha256");
            key = keyGen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("HmacSha256 algorithm not available", e);
        }
    }

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
}