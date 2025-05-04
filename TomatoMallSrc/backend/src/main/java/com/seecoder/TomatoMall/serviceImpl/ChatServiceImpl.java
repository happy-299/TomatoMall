package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.controller.ChatController;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.ChatMessage;
import com.seecoder.TomatoMall.po.ChatSession;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.repository.ChatMessageRepository;
import com.seecoder.TomatoMall.repository.ChatSessionRepository;
import com.seecoder.TomatoMall.service.ChatService;
import com.seecoder.TomatoMall.vo.ChatMessageVO;
import com.seecoder.TomatoMall.vo.ChatSessionMetaVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionRepository sessionRepo;

    @Autowired
    private ChatMessageRepository messageRepo;

    @Autowired
    private AccountRepository accountRepo;

    /* 新建或取会话 */
    private ChatSession ensureSession(Integer uid, Integer sid) {
        if (sid != 0) {
            ChatSession s = sessionRepo.findById(sid)
                    .orElseThrow(() -> new TomatoMallException("会话不存在"));
            if (!s.getAccount().getId().equals(uid))
                throw new TomatoMallException("无权访问该会话");
            return s;
        }
        ChatSession s = new ChatSession();
        s.setAccount(accountRepo.getOne(uid));
        return sessionRepo.save(s);
    }

    @Override
    @Transactional
    public Integer post(Integer uid, ChatController.ChatPostDTO dto) {
        ChatSession session = ensureSession(uid, dto.getSessionId());

        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setRole(ChatMessage.Role.valueOf(dto.getRole()));
        msg.setContent(dto.getContent());
        messageRepo.save(msg);

        session.setLastTime(LocalDateTime.now());
        return session.getId();
    }

    @Override
    public List<ChatSessionMetaVO> listSessions(Integer uid) {
        return sessionRepo.findByAccountIdOrderByLastTimeDesc(uid).stream()
                .map(s -> {
                    ChatSessionMetaVO vo = new ChatSessionMetaVO();
                    vo.setId(s.getId());
                    vo.setTitle(s.getTitle());
                    vo.setCreateTime(s.getCreateTime());
                    vo.setLastTime(s.getLastTime());
                    vo.setMessageCount((long) s.getMessages().size());
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> listMessages(Integer uid, Integer sid) {
        ChatSession s = sessionRepo.findById(sid)
                .orElseThrow(() -> new TomatoMallException("会话不存在"));
        if (!s.getAccount().getId().equals(uid))
            throw new TomatoMallException("无权查看");

        return messageRepo.findBySessionIdOrderByIdAsc(sid).stream()
                .map(m -> {
                    ChatMessageVO vo = new ChatMessageVO();
                    vo.setId(m.getId());
                    vo.setRole(m.getRole().name());
                    vo.setContent(m.getContent());
                    vo.setTime(m.getCreateTime());
                    return vo;
                }).collect(Collectors.toList());
    }
}
