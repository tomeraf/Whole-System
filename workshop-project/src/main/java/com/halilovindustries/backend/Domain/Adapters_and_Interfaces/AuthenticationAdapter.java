package com.halilovindustries.backend.Domain.Adapters_and_Interfaces;

import java.util.Date;
import java.util.function.Function;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;


@Component
public class AuthenticationAdapter implements IAuthentication {

    private IAuthentication authentication;

    // Dependency injection of the authentication implementation adapter

    public AuthenticationAdapter(IAuthentication authentication) {
        this.authentication = authentication;
    }

    @Override
    public String generateToken(String username) {
        return authentication.generateToken(username);
    }

    @Override
    public boolean validateToken(String token) {
        return authentication.validateToken(token);
    }

    @Override
    public String getUsername(String token) {
        return authentication.getUsername(token);
    }

    @Override
    public Date extractExpiration(String token) {
        return authentication.extractExpiration(token);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return authentication.extractClaim(token, claimsResolver);
    }
}