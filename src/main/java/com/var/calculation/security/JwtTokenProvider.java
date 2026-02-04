package com.var.calculation.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    
    private SecretKey key;
    
    @PostConstruct
    private void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
    
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        long now = System.currentTimeMillis();
        
        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("role", userPrincipal.getRole())
                .issuedAt(new Date(now))
                .expiration(new Date(now + jwtExpiration))
                .signWith(key)
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
