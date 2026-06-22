package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BoardAttachment {
    private String id;
    private String articleId;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private long fileSize;
    private String fileType;
    private LocalDateTime createdAt;
}
