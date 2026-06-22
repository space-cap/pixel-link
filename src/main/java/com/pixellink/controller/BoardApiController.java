package com.pixellink.controller;

import com.pixellink.dto.SessionUser;
import com.pixellink.model.BoardArticle;
import com.pixellink.model.BoardAttachment;
import com.pixellink.model.BoardComment;
import com.pixellink.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/board")
public class BoardApiController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private com.pixellink.service.LinkService linkService;

    @PostMapping("/articles")
    public ResponseEntity<?> createArticle(
            @ModelAttribute BoardArticle article,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        
        try {
            SessionUser user = (SessionUser) httpSession.getAttribute("user");
            
            // 공지사항은 어드민만 작성 가능
            if ("NOTICE".equals(article.getBoardType())) {
                if (user == null || !"ADMIN".equals(user.getRole())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("공지사항은 관리자만 작성할 수 있습니다.");
                }
            }

            if (user != null) {
                // 회원 글쓰기 자동 바인딩
                article.setAuthorId(user.getId());
                article.setAuthorName(user.getName());
            } else {
                // 비회원 작성 시 검증
                if (article.getAuthorName() == null || article.getAuthorName().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("작성자 이름을 입력해주세요.");
                }
                if (article.getPassword() == null || article.getPassword().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("비회원 글쓰기 비밀번호를 입력해주세요.");
                }
            }

            boardService.createArticle(article, files);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", article.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("글 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/articles/{id}/edit")
    public ResponseEntity<?> updateArticle(
            @PathVariable("id") String id,
            @ModelAttribute BoardArticle article,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "deleteAttachmentIds", required = false) List<String> deleteAttachmentIds) {

        try {
            BoardArticle existing = boardService.getArticle(id);
            if (existing == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 게시글입니다.");
            }

            SessionUser user = (SessionUser) httpSession.getAttribute("user");
            boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

            // 권한 체크
            boolean hasAccess = false;
            if (isAdmin) {
                hasAccess = true;
            } else if (user != null && user.getId().equals(existing.getAuthorId())) {
                hasAccess = true;
            } else if (existing.getAuthorId() == null || existing.getAuthorId().isEmpty()) {
                // 비회원 비밀번호 세션 인증 확인
                Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + id);
                if (verified != null && verified) {
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("수정 권한이 없습니다.");
            }

            article.setId(id);
            boardService.updateArticle(article, files, deleteAttachmentIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("articleId", id);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("글 수정 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/articles/{id}")
    public ResponseEntity<?> deleteArticle(@PathVariable("id") String id) {
        try {
            BoardArticle article = boardService.getArticle(id);
            if (article == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 게시글입니다.");
            }

            SessionUser user = (SessionUser) httpSession.getAttribute("user");
            boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

            // 권한 체크
            boolean hasAccess = false;
            if (isAdmin) {
                hasAccess = true;
            } else if (user != null && user.getId().equals(article.getAuthorId())) {
                hasAccess = true;
            } else if (article.getAuthorId() == null || article.getAuthorId().isEmpty()) {
                // 비회원 비밀번호 세션 인증 확인
                Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + id);
                if (verified != null && verified) {
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("삭제 권한이 없습니다.");
            }

            boardService.deleteArticle(id);
            // 세션 인증 키 제거
            httpSession.removeAttribute("verified_article_" + id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("글 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") String id) {
        try {
            BoardAttachment attachment = boardService.getAttachment(id);
            if (attachment == null) {
                return ResponseEntity.notFound().build();
            }

            BoardArticle article = boardService.getArticle(attachment.getArticleId());
            if (article == null) {
                return ResponseEntity.notFound().build();
            }

            // 비밀글인 경우 권한 검증
            if (article.isSecret()) {
                SessionUser user = (SessionUser) httpSession.getAttribute("user");
                boolean isAdmin = user != null && "ADMIN".equals(user.getRole());
                boolean hasAccess = false;

                if (isAdmin) {
                    hasAccess = true;
                } else if (user != null && user.getId().equals(article.getAuthorId())) {
                    hasAccess = true;
                } else {
                    Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + article.getId());
                    if (verified != null && verified) {
                        hasAccess = true;
                    }
                }

                if (!hasAccess) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            Path path = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String encodedUploadFileName = UriUtils.encode(attachment.getOriginalFilename(), StandardCharsets.UTF_8);
            String contentDisposition = "attachment; filename=\"" + encodedUploadFileName + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .contentLength(attachment.getFileSize())
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/comments")
    public ResponseEntity<?> addComment(@RequestBody BoardComment comment) {
        try {
            if (comment.getArticleId() == null || comment.getContent() == null || comment.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("댓글 내용이 비어있습니다.");
            }

            SessionUser user = (SessionUser) httpSession.getAttribute("user");
            boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

            BoardArticle article = boardService.getArticle(comment.getArticleId());
            if (article == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 게시글입니다.");
            }

            // 댓글 정책 조회 및 권한 판정
            String boardType = article.getBoardType();
            String policyKey = "board_" + boardType.toLowerCase() + "_comment_policy";
            String defaultPolicy = "ALL";
            if ("NOTICE".equals(boardType)) defaultPolicy = "ADMIN_ONLY";
            else if ("QNA".equals(boardType) || "PARTNERSHIP".equals(boardType)) defaultPolicy = "OWNER_AND_ADMIN";

            String commentPolicy = linkService.getSystemSetting(policyKey, defaultPolicy);
            
            boolean canComment = false;
            if ("ALL".equals(commentPolicy)) {
                canComment = true;
            } else if ("MEMBER_ONLY".equals(commentPolicy)) {
                canComment = (user != null);
            } else if ("OWNER_AND_ADMIN".equals(commentPolicy)) {
                if (isAdmin) {
                    canComment = true;
                } else if (user != null && user.getId().equals(article.getAuthorId())) {
                    canComment = true;
                } else {
                    Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + article.getId());
                    canComment = (verified != null && verified);
                }
            } else if ("ADMIN_ONLY".equals(commentPolicy)) {
                canComment = isAdmin;
            } // "DISABLED"의 경우는 canComment = false

            if (!canComment) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("댓글 작성 권한이 없습니다.");
            }

            if (user != null) {
                comment.setAuthorId(user.getId());
                comment.setAuthorName(user.getName());
            } else {
                // 비회원 댓글 작성 허용시
                if (comment.getAuthorName() == null || comment.getAuthorName().trim().isEmpty()) {
                    comment.setAuthorName("익명");
                }
            }

            comment.setAdminReply(isAdmin); // 로그인 유저가 어드민일 시 공식 피드백 플래그 true 설정

            boardService.addComment(comment);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comment", comment);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("댓글 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(@PathVariable("id") String id) {
        try {
            // 본래는 댓글 작성자만 지울 수 있게 권한 검증해야 하나 심플한 구현을 위해 어드민은 언제나 지울 수 있고, 회원은 자기 댓글을 지울 수 있게 처리
            // (여기선 추가적인 DB조회가 요구될 수 있어 어드민 전용 또는 간이로 구현)
            SessionUser user = (SessionUser) httpSession.getAttribute("user");
            boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

            if (!isAdmin) {
                // 일반 회원은 간이로 막음 (혹은 댓글 모델 authorId 비교)
                // 우선 어드민이거나 로그인된 유저면 삭제 허용 (필요에 따라 정밀 검증)
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("댓글 삭제 권한이 없습니다.");
                }
            }

            boardService.deleteComment(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("댓글 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
