package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.BookListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface BookListItemRepository extends JpaRepository<BookListItem, Integer> {
    void deleteByBookListId(Integer bookListId);


    boolean existsByBookListIdAndProductId(Integer listId, Integer productId);

    @Modifying
    @Transactional
    void deleteByBookListIdAndProductId(Integer listId, Integer productId);
}
