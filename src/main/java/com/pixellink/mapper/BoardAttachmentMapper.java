package com.pixellink.mapper;

import com.pixellink.model.BoardAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BoardAttachmentMapper {
    BoardAttachment findById(@Param("id") String id);
    List<BoardAttachment> findByArticleId(@Param("articleId") String articleId);
    int insert(BoardAttachment attachment);
    int delete(@Param("id") String id);
}
