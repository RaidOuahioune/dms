package com.example.documents.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                        AuthenticationException authException) throws IOException, ServletException {
        
        // Set the response status and content type
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Create error response JSON
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", HttpStatus.UNAUTHORIZED.value());
        errorDetails.put("error", "Unauthorized");
        errorDetails.put("message", authException.getMessage() != null ? 
                authException.getMessage() : "Authentication failed: Invalid or missing token");
        errorDetails.put("path", request.getRequestURI());
        
        // Write the error response to the output stream
        objectMapper.writeValue(response.getOutputStream(), errorDetails);
    }
}