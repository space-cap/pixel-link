package com.pixellink.service;

import com.pixellink.mapper.BoardArticleMapper;
import com.pixellink.mapper.BoardAttachmentMapper;
import com.pixellink.mapper.BoardCommentMapper;
import com.pixellink.model.BoardArticle;
import com.pixellink.model.BoardAttachment;
import com.pixellink.model.BoardComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BoardService {

    @Autowired
    private BoardArticleMapper boardArticleMapper;

    @Autowired
    private BoardCommentMapper boardCommentMapper;

    @Autowired
    private BoardAttachmentMapper boardAttachmentMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public BoardArticle getArticle(String id) {
        return boardArticleMapper.findById(id);
    }

    public void incrementViewCount(String id) {
        boardArticleMapper.incrementViewCount(id);
    }

    public List<BoardArticle> getArticles(String boardType, String searchKeyword, int page, int size) {
        int offset = (page - 1) * size;
        return boardArticleMapper.findAllPaged(boardType, searchKeyword, offset, size);
    }

    public int getArticleCount(String boardType, String searchKeyword) {
        return boardArticleMapper.countAll(boardType, searchKeyword);
    }

    public void createArticle(BoardArticle article, List<MultipartFile> files) throws IOException {
        article.setId(UUID.randomUUID().toString());
        
        // 비회원 작성 시 비밀번호 암호화
        if ((article.getAuthorId() == null || article.getAuthorId().isEmpty()) && article.getPassword() != null && !article.getPassword().isEmpty()) {
            article.setPassword(passwordEncoder.encode(article.getPassword()));
        } else {
            // 회원 작성 시 패스워드는 필요 없음
            article.setPassword(null);
        }

        if (article.getStatus() == null || article.getStatus().isEmpty()) {
            article.setStatus("OPEN");
        }
        
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        article.setViewCount(0);

        boardArticleMapper.insert(article);

        // 파일 저장 처리
        saveFiles(article.getId(), files);
    }

    public void updateArticle(BoardArticle article, List<MultipartFile> files, List<String> deleteAttachmentIds) throws IOException {
        BoardArticle existing = boardArticleMapper.findById(article.getId());
        if (existing == null) {
            throw new IllegalArgumentException("게시글이 존재하지 않습니다.");
        }

        existing.setTitle(article.getTitle());
        existing.setContent(article.getContent());
        existing.setSecret(article.isSecret());
        existing.setUpdatedAt(LocalDateTime.now());

        // 비회원 비밀번호가 변경되어 넘어왔다면 암호화 후 반영
        if (article.getPassword() != null && !article.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(article.getPassword()));
        }

        boardArticleMapper.update(existing);

        // 첨부파일 삭제 처리
        if (deleteAttachmentIds != null && !deleteAttachmentIds.isEmpty()) {
            for (String attachId : deleteAttachmentIds) {
                BoardAttachment attach = boardAttachmentMapper.findById(attachId);
                if (attach != null) {
                    deletePhysicalFile(attach.getFilePath());
                    boardAttachmentMapper.delete(attachId);
                }
            }
        }

        // 새 첨부파일 저장 처리
        saveFiles(article.getId(), files);
    }

    public void deleteArticle(String id) {
        // 기존 첨부파일 디스크에서 모두 삭제
        List<BoardAttachment> attachments = boardAttachmentMapper.findByArticleId(id);
        for (BoardAttachment attach : attachments) {
            deletePhysicalFile(attach.getFilePath());
        }
        
        // cascade delete에 의해 DB 데이터는 삭제되지만, 명시적으로 삭제 흐름을 보장함
        boardArticleMapper.delete(id);
    }

    public boolean verifyNonMemberPassword(String articleId, String plainPassword) {
        BoardArticle article = boardArticleMapper.findById(articleId);
        if (article == null || article.getPassword() == null) {
            return false;
        }
        return passwordEncoder.matches(plainPassword, article.getPassword());
    }

    // --- 댓글 / 관리자 답변 관련 로직 ---

    public List<BoardComment> getComments(String articleId) {
        return boardCommentMapper.findByArticleId(articleId);
    }

    public void addComment(BoardComment comment) {
        comment.setId(UUID.randomUUID().toString());
        comment.setCreatedAt(LocalDateTime.now());
        boardCommentMapper.insert(comment);

        // 1:1 문의(QNA) 또는 제휴제안(PARTNERSHIP)글에 관리자가 공식 피드백/댓글을 작성한 경우 글의 상태를 RESOLVED로 변경
        if (comment.isAdminReply()) {
            BoardArticle article = boardArticleMapper.findById(comment.getArticleId());
            if (article != null && ("QNA".equals(article.getBoardType()) || "PARTNERSHIP".equals(article.getBoardType()))) {
                boardArticleMapper.updateStatus(article.getId(), "RESOLVED");
            }
        }
    }

    public void deleteComment(String commentId) {
        boardCommentMapper.delete(commentId);
    }

    // --- 첨부파일 관련 로직 ---

    public List<BoardAttachment> getAttachments(String articleId) {
        return boardAttachmentMapper.findByArticleId(articleId);
    }

    public BoardAttachment getAttachment(String attachmentId) {
        return boardAttachmentMapper.findById(attachmentId);
    }

    // --- 내부 헬퍼 메소드 ---

    private void saveFiles(String articleId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return;
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unnamed";
            }

            String extension = "";
            int dotIdx = originalFilename.lastIndexOf('.');
            if (dotIdx > 0) {
                extension = originalFilename.substring(dotIdx);
            }

            String storedFilename = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadPath.resolve(storedFilename);
            
            // 디스크에 저장
            Files.copy(file.getInputStream(), targetLocation);

            // DB 메타데이터 등록
            BoardAttachment attachment = new BoardAttachment();
            attachment.setId(UUID.randomUUID().toString());
            attachment.setArticleId(articleId);
            attachment.setOriginalFilename(originalFilename);
            attachment.setStoredFilename(storedFilename);
            attachment.setFilePath(targetLocation.toString());
            attachment.setFileSize(file.getSize());
            attachment.setFileType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
            attachment.setCreatedAt(LocalDateTime.now());

            boardAttachmentMapper.insert(attachment);
        }
    }

    private void deletePhysicalFile(String filePathStr) {
        try {
            Path path = Paths.get(filePathStr);
            Files.deleteIfExists(path);
        } catch (Exception e) {
            // 파일 삭제 실패는 비즈니스 예외로 던지지 않고 로그 등으로만 남길 수 있게 무시 또는 로깅
            System.err.println("실제 파일 삭제 중 오류 발생: " + filePathStr + " | " + e.getMessage());
        }
    }
}
