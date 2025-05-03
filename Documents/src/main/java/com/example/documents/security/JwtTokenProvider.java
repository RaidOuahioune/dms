package com.example.documents.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT key initialized successfully");
    }

    public Authentication getAuthentication(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Collection<? extends GrantedAuthority> authorities;
            
            try {
                // Try to get roles from the token
                String rolesString = claims.get("roles", String.class);
                if (rolesString != null && !rolesString.isEmpty()) {
                    log.info("Roles found in token: " + rolesString);
                    authorities = Arrays.stream(rolesString.split(","))
                        .filter(StringUtils::hasText)
                        .map(role -> {
                            // Check if the role already starts with ROLE_
                            if (role.trim().startsWith("ROLE_")) {
                                return new SimpleGrantedAuthority(role.trim());
                            } else {
                                return new SimpleGrantedAuthority("ROLE_" + role.trim());
                            }
                        })
                        .collect(Collectors.toList());
                } else {
                    // Default to USER role if no roles found
                    authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                }
            } catch (Exception e) {
                log.warn("Could not extract roles from token, defaulting to USER role: {}", e.getMessage());
                authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            }

            UserDetails userDetails = new User(claims.getSubject(), "", authorities);
            return new UsernamePasswordAuthenticationToken(userDetails, "", authorities);
        } catch (Exception e) {
            log.error("Failed to authenticate token: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(key).build()
                .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
        .verifyWith(key)
        .build()
        .parseSignedClaims(token)
        .getPayload();
        return claims.getSubject();
    }
    
    /**
     * Extract user ID from JWT token
     * @param token The JWT token (without 'Bearer ' prefix)
     * @return The user ID from the token or null if invalid
     */
    public String extractUserIdFromToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return null;
            }
            
            return getUserIdFromJWT(token);
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    public String extractTokenFromHeader(String bearerToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}