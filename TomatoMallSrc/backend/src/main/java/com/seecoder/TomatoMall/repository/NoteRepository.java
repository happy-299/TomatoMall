package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.BookList;
import com.seecoder.TomatoMall.po.Note;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Integer>
{
    List<Note> findAllByCreator(Account creator);

    @Query("SELECT n FROM Note n WHERE LOWER(n.title) LIKE %:keyword% OR LOWER(n.content) LIKE %:keyword%")
    List<Note> searchNotes(@Param("keyword") String keyword);
}
