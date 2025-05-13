package com.seecoder.TomatoMall.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seecoder.TomatoMall.controller.ChatController;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.ChatMessage;
import com.seecoder.TomatoMall.po.ChatSession;
import com.seecoder.TomatoMall.repository.AccountRepository;
import com.seecoder.TomatoMall.repository.ChatMessageRepository;
import com.seecoder.TomatoMall.repository.ChatSessionRepository;
import com.seecoder.TomatoMall.service.ChatService;
import com.seecoder.TomatoMall.service.ProductService;
import com.seecoder.TomatoMall.util.AiUtil;
import com.seecoder.TomatoMall.vo.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @Autowired
    private ProductService productService;

    @Autowired
    private AiUtil aiClient;

    /** 生成系统 Prompt，把图书列表嵌进去（可按需过滤 / 限制条数） */
    private AiUtil.Message buildSystemPrompt() {
        List<ProductVO> products = productService.getAllProducts();
        // 只取必要字段，避免 prompt 过长
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名电商导购，根据用户需求推荐一本图书。\n")
                .append("数据库图书列表(json 数组)：\n[");

        for (ProductVO p : products) {
            sb.append(String.format(
                    "{\"id\":%d,\"title\":\"%s\",\"price\":%s,\"desc\":\"%s\"},",
                    p.getId(), p.getTitle(), p.getPrice(), p.getDescription()));
        }
        if (!products.isEmpty()) sb.setLength(sb.length() - 1); // 去掉尾逗号
        sb.append("]\n")
                .append("返回格式：{\"id\":图书id,\"reason\":\"一句推荐理由\"} ，不要添加其它文字。");

        return new AiUtil.Message("system", sb.toString());
    }


    /* 私有工具：确保会话存在或创建 */
    private ChatSession ensure(Integer uid, Integer sid) {
        if (sid != null && sid != 0) {
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
    public ChatReplyVO talkWithAi(Integer uid, ChatTalkDTO dto) {

        /* ---------- 0. 会话 ---------- */
        ChatSession session = ensure(uid, dto.getSessionId());

        /* ---------- 1. 保存用户消息 ---------- */
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole(ChatMessage.Role.USER);
        userMsg.setContent(dto.getContent());
        messageRepo.save(userMsg);

        /* ---------- 2. 构造 Prompt ---------- */

        // 2-1 system prompt（推荐说明）
        AiUtil.Message systemMsg = buildSystemPrompt();

        // 2-2 最近 20 条历史
        List<ChatMessage> history =
                messageRepo.findTop20BySessionIdOrderByIdAsc(session.getId());

        List<AiUtil.Message> prompt = new ArrayList<>();
        prompt.add(systemMsg);

        prompt.addAll(history.stream()
                .map(m -> new AiUtil.Message(
                        m.getRole() == ChatMessage.Role.USER ? "user" : "assistant",
                        m.getContent()))
                .collect(Collectors.toList()));

        // 2-3 最新用户输入
        prompt.add(new AiUtil.Message("user", dto.getContent()));

        /* ---------- 3. 调 DeepSeek ---------- */
        String aiRaw = aiClient.chat(prompt);   // 期望 {"id":12,"reason":"…"}

        /* ---------- 4. 解析 / 校验 ---------- */
        int prodId;
        String reason;
        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode node = om.readTree(aiRaw);
            prodId = node.get("id").asInt();
            reason = node.get("reason").asText();
        } catch (Exception e) {
            throw new TomatoMallException("AI 返回格式错误，应为 {\"id\":...}: " + aiRaw);
        }

        // 若图书不存在会抛 TomatoMallException
        productService.getProductById(prodId);

        /* ---------- 5. 保存 AI 消息 ---------- */
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSession(session);
        aiMsg.setRole(ChatMessage.Role.AI);
        aiMsg.setContent(aiRaw);            // 保存原始 JSON
        messageRepo.save(aiMsg);

        session.setLastTime(LocalDateTime.now());

        /* ---------- 6. 组织返回 ---------- */
        ChatReplyVO vo = new ChatReplyVO();
        vo.setSessionId(session.getId());
        vo.setReply(aiRaw);                 // 前端可 JSON.parse() 再展示
        vo.setTime(aiMsg.getCreateTime());
        return vo;
    }


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
