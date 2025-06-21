package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.BookList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookListRepository extends JpaRepository<BookList, Integer>
{
    Page<BookList> findByCreatorId(Integer createrId, Pageable pageable);

    @Modifying
    @Query("UPDATE BookList b SET b.favoriteCount = b.favoriteCount + 1 WHERE b.id = :book_list_id")
    void incrementFavoriteCount(@Param("book_list_id") Integer bookListId);

    @Modifying
    @Query("UPDATE BookList b SET b.favoriteCount = b.favoriteCount - 1 WHERE b.id = :book_list_id")
    void decrementFavoriteCount(@Param("book_list_id") Integer bookListId);

    @Query("SELECT b FROM BookList b WHERE LOWER(b.title) LIKE %:keyword% OR LOWER(b.description) LIKE %:keyword%")
    List<BookList> searchBookLists(@Param("keyword") String keyword);

    Page<BookList> findAllByOrderByFavoriteCountDesc(Pageable pageable);
}
