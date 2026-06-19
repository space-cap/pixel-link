package com.pixellink.dto;

import com.pixellink.model.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private final Map<String, Object> attributes;
    private final String nameAttributeKey;
    private final String name;
    private final String email;
    private final String registrationId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String registrationId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.registrationId = registrationId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        if ("naver".equalsIgnoreCase(registrationId)) {
            return ofNaver("id", attributes);
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            return ofKakao("id", attributes);
        } else if ("facebook".equalsIgnoreCase(registrationId)) {
            return ofFacebook("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId("google")
                .build();
    }

    private static OAuthAttributes ofFacebook(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId("facebook")
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .registrationId("naver")
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = kakaoAccount != null ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String name = profile != null ? (String) profile.get("nickname") : "KakaoUser";
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        // kakao의 id는 Long 타입이므로 String으로 변환해서 attributes 맵에 저장해 줍니다.
        Object idObj = attributes.get("id");
        String idStr = idObj != null ? idObj.toString() : "";

        return OAuthAttributes.builder()
                .name(name)
                .email(email)
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId("kakao")
                .build();
    }

    public User toEntity() {
        User user = new User();
        // 중복 방지를 위한 [provider]_[providerId] 형태의 PK 생성
        String idValue = "";
        if ("google".equalsIgnoreCase(registrationId)) {
            idValue = "google_" + attributes.get("sub");
        } else if ("naver".equalsIgnoreCase(registrationId)) {
            idValue = "naver_" + attributes.get("id");
        } else if ("kakao".equalsIgnoreCase(registrationId)) {
            idValue = "kakao_" + attributes.get("id").toString();
        } else if ("facebook".equalsIgnoreCase(registrationId)) {
            idValue = "facebook_" + attributes.get("id");
        }
        
        user.setId(idValue);
        // 이메일이 없는 소셜 계정의 경우를 대비한 폴백 처리
        user.setEmail(email != null ? email : idValue + "@pixellink.mock");
        user.setSubscriptionTier("FREE");
        user.setSubscriptionEndsAt(null);
        return user;
    }
}
