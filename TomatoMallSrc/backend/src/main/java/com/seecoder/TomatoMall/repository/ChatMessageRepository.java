package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
    List<ChatMessage> findBySessionIdOrderByIdAsc(Integer sessionId);
}
