package com.pixellink.service;

import com.pixellink.mapper.ApiKeyMapper;
import com.pixellink.model.ApiKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    public ApiKey generateApiKey(String userId, String name) {
        String id = UUID.randomUUID().toString();
        // 접두사를 포함한 고유 토큰 생성
        String rawKey = "pxl_live_" + UUID.randomUUID().toString().replace("-", "");
        
        ApiKey apiKey = ApiKey.builder()
                .id(id)
                .userId(userId)
                .apiKey(rawKey)
                .name(name)
                .isActive(true)
                .build();
        
        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    public ApiKey validateKey(String rawKey) {
        if (rawKey == null || !rawKey.startsWith("pxl_live_")) {
            return null;
        }
        return apiKeyMapper.findByApiKey(rawKey);
    }

    public List<ApiKey> getApiKeysByUserId(String userId) {
        return apiKeyMapper.findByUserId(userId);
    }

    public void revokeKey(String id) {
        apiKeyMapper.deleteById(id);
    }
}
