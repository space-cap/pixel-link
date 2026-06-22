package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoardComment {
    private String id;
    private String articleId;
    private String authorId;
    private String authorName;
    private String content;
    private boolean isAdminReply;
    private LocalDateTime createdAt;
}
