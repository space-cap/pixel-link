package com.pixellink.mapper;

import com.pixellink.model.BoardArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BoardArticleMapper {
    BoardArticle findById(@Param("id") String id);
    
    List<BoardArticle> findAllPaged(
        @Param("boardType") String boardType,
        @Param("searchKeyword") String searchKeyword,
        @Param("offset") int offset,
        @Param("limit") int limit
    );
    
    int countAll(
        @Param("boardType") String boardType,
        @Param("searchKeyword") String searchKeyword
    );
    
    int insert(BoardArticle article);
    
    int update(BoardArticle article);
    
    int updateStatus(@Param("id") String id, @Param("status") String status);
    
    int incrementViewCount(@Param("id") String id);
    
    int delete(@Param("id") String id);
}
