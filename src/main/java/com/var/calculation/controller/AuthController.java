package com.var.calculation.controller;

import com.var.calculation.model.dto.AuthRequest;
import com.var.calculation.model.dto.AuthResponse;
import com.var.calculation.security.JwtTokenProvider;
import com.var.calculation.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for user login and JWT token generation.
 * Provides endpoint for obtaining JWT tokens for API access.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    
    /**
     * Authenticates user and returns JWT token.
     * 
     * @param request Authentication credentials (username and password)
     * @return AuthResponse containing JWT token, role, and expiration time
     */
    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.username(), 
                request.password()
            )
        );
        
        var token = tokenProvider.generateToken(authentication);
        var userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        return ResponseEntity.ok(
            new AuthResponse(token, userPrincipal.getRole(), 86400L)
        );
    }
}
