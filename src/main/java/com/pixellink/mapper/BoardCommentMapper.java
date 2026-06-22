package com.pixellink.mapper;

import com.pixellink.model.BoardComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BoardCommentMapper {
    List<BoardComment> findByArticleId(@Param("articleId") String articleId);
    int insert(BoardComment comment);
    int delete(@Param("id") String id);
}
