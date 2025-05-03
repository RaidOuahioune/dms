package com.example.documents.controller;

import com.example.documents.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("status", "UP");
        healthDetails.put("service", "Documents Service");
        healthDetails.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success("Service is healthy from raid", healthDetails));
    }
}