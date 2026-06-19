package com.pixellink.controller;

import com.pixellink.model.ApiKey;
import com.pixellink.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard/keys")
@RequiredArgsConstructor
public class ApiKeyDashboardController {

    private final ApiKeyService apiKeyService;

    @GetMapping
    public ResponseEntity<List<ApiKey>> getMyKeys(@RequestParam("userId") String userId) {
        List<ApiKey> keys = apiKeyService.getApiKeysByUserId(userId);
        return ResponseEntity.ok(keys);
    }

    @PostMapping
    public ResponseEntity<ApiKey> createKey(@RequestBody Map<String, String> body) {
        String userId = body.get("userId");
        String name = body.getOrDefault("name", "API Key");
        
        if (userId == null || userId.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        ApiKey key = apiKeyService.generateApiKey(userId, name);
        return ResponseEntity.ok(key);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable("id") String id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.noContent().build();
    }
}
