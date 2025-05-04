package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {
    List<ChatSession> findByAccountIdOrderByLastTimeDesc(Integer uid);
}
