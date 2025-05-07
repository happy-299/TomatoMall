package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.Note;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Integer>
{
    List<Note> findAllByCreator(Account creator);
}
