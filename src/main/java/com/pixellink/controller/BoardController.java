package com.pixellink.controller;

import com.pixellink.dto.SessionUser;
import com.pixellink.model.BoardArticle;
import com.pixellink.model.BoardAttachment;
import com.pixellink.model.BoardComment;
import com.pixellink.service.BoardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    @Autowired
    private HttpSession httpSession;

    @Autowired
    private com.pixellink.service.LinkService linkService;

    @GetMapping("/list")
    public String list(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "searchKeyword", required = false) String searchKeyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            Model model) {

        int size = 10;
        List<BoardArticle> articles = boardService.getArticles(boardType, searchKeyword, page, size);
        int totalArticles = boardService.getArticleCount(boardType, searchKeyword);
        int totalPages = (int) Math.ceil((double) totalArticles / size);
        if (totalPages == 0) totalPages = 1;

        model.addAttribute("articles", articles);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("boardType", boardType);
        model.addAttribute("searchKeyword", searchKeyword);
        
        // 로그인 정보 전달
        SessionUser user = (SessionUser) httpSession.getAttribute("user");
        model.addAttribute("sessionUser", user);

        return "board/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") String id, Model model) {
        BoardArticle article = boardService.getArticle(id);
        if (article == null) {
            return "redirect:/board/list";
        }

        SessionUser user = (SessionUser) httpSession.getAttribute("user");
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

        // 비밀글 권한 체크
        if (article.isSecret()) {
            boolean hasAccess = false;
            
            // 1. 관리자 통과
            if (isAdmin) {
                hasAccess = true;
            }
            // 2. 작성자 본인 통과 (회원글인 경우)
            else if (user != null && user.getId().equals(article.getAuthorId())) {
                hasAccess = true;
            }
            // 3. 비회원 비밀번호 인증 통과 (세션 검증)
            else {
                Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + id);
                if (verified != null && verified) {
                    hasAccess = true;
                }
            }

            if (!hasAccess) {
                // 권한 없으면 비밀번호 검증 폼으로 리다이렉트
                return "redirect:/board/password/" + id;
            }
        }

        // 조회수 증가 및 상세 조회 정보 조회
        boardService.incrementViewCount(id);
        List<BoardAttachment> attachments = boardService.getAttachments(id);
        List<BoardComment> comments = boardService.getComments(id);

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
                Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + id);
                canComment = (verified != null && verified);
            }
        } else if ("ADMIN_ONLY".equals(commentPolicy)) {
            canComment = isAdmin;
        } // "DISABLED"의 경우는 canComment = false

        model.addAttribute("article", article);
        model.addAttribute("attachments", attachments);
        model.addAttribute("comments", comments);
        model.addAttribute("sessionUser", user);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("commentPolicy", commentPolicy);
        model.addAttribute("canComment", canComment);

        return "board/detail";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam(value = "boardType", defaultValue = "FREE") String boardType, Model model) {
        SessionUser user = (SessionUser) httpSession.getAttribute("user");
        
        // 공지사항(NOTICE)은 ADMIN만 작성 가능
        if ("NOTICE".equals(boardType)) {
            if (user == null || !"ADMIN".equals(user.getRole())) {
                return "redirect:/board/list?boardType=" + boardType;
            }
        }

        model.addAttribute("boardType", boardType);
        model.addAttribute("sessionUser", user);
        return "board/create";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") String id, Model model) {
        BoardArticle article = boardService.getArticle(id);
        if (article == null) {
            return "redirect:/board/list";
        }

        SessionUser user = (SessionUser) httpSession.getAttribute("user");
        boolean isAdmin = user != null && "ADMIN".equals(user.getRole());

        // 수정 권한 확인
        boolean hasAccess = false;
        if (isAdmin) {
            hasAccess = true;
        } else if (user != null && user.getId().equals(article.getAuthorId())) {
            hasAccess = true;
        } else if (article.getAuthorId() == null || article.getAuthorId().isEmpty()) {
            // 비회원 글의 경우 세션 인증이 되어 있어야 함
            Boolean verified = (Boolean) httpSession.getAttribute("verified_article_" + id);
            if (verified != null && verified) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            return "redirect:/board/detail/" + id;
        }

        List<BoardAttachment> attachments = boardService.getAttachments(id);
        model.addAttribute("article", article);
        model.addAttribute("attachments", attachments);
        model.addAttribute("sessionUser", user);

        return "board/edit";
    }

    @GetMapping("/password/{id}")
    public String passwordForm(@PathVariable("id") String id, Model model) {
        BoardArticle article = boardService.getArticle(id);
        if (article == null) {
            return "redirect:/board/list";
        }
        
        model.addAttribute("articleId", id);
        model.addAttribute("boardType", article.getBoardType());
        return "board/password";
    }

    @PostMapping("/password/{id}")
    public String verifyPassword(
            @PathVariable("id") String id,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {
        
        boolean match = boardService.verifyNonMemberPassword(id, password);
        if (match) {
            httpSession.setAttribute("verified_article_" + id, true);
            return "redirect:/board/detail/" + id;
        } else {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/board/password/" + id;
        }
    }
}
