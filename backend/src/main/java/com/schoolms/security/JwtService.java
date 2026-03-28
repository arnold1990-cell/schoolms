package com.schoolms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @Value("${app.jwt.secret:${jwt.secret:}}")
    private String secret;
    @Value("${app.jwt.expiration-ms:${jwt.expiration-ms:86400000}}")
    private long expirationMs;
    private SecretKey signingKey;

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret is not configured. Expected app.jwt.secret (or jwt.secret).");
        }
        try {
            this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret.trim()));
        } catch (IllegalArgumentException | DecodingException ex) {
            throw new IllegalStateException("JWT secret must be a valid Base64-encoded HMAC key.", ex);
        }
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256);
        claims.forEach(builder::claim);
        return builder.compact();
    }

    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        return jws.getPayload();
    }

    public boolean isValid(String token, String email) {
        Claims claims = parse(token);
        String subject = claims.getSubject() == null ? "" : claims.getSubject().trim().toLowerCase(java.util.Locale.ROOT);
        String expectedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        return subject.equals(expectedEmail) && claims.getExpiration().after(new Date());
    }

}
