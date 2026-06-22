package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoardArticle {
    private String id;
    private String boardType; // "NOTICE", "FREE", "QNA", "PARTNERSHIP"
    private String title;
    private String content;
    private String authorId;
    private String authorName;
    private boolean isSecret;
    private String password; // 암호화된 비밀번호 (비회원용)
    private String status; // "OPEN", "RESOLVED", "PENDING"
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
