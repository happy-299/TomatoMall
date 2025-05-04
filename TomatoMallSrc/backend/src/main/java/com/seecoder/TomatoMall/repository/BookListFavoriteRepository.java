package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.BookListFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface BookListFavoriteRepository extends JpaRepository<BookListFavorite, Integer> {
    boolean existsByAccountIdAndBookListId(Integer accountId, Integer bookListId);

    Page<BookListFavorite> findByAccountId(Integer accountId, Pageable p);

    @Transactional
    void deleteByAccountIdAndBookListId(Integer accountId, Integer bookListId);

    @Transactional
    void deleteByBookListId(Integer bookListId);

}
