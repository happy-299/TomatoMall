package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AccountService;
import com.seecoder.TomatoMall.service.ChatService;
import com.seecoder.TomatoMall.vo.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @Autowired
    private AccountService accountService;

    @Data
    public static class ChatPostDTO {
        private Integer sessionId;   // 0 = 新会话
        private String role;        // "USER" / "AI"
        private String content;
    }

    @PostMapping("/talk")
    public Response<ChatReplyVO> talk(@RequestBody ChatTalkDTO dto) {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(chatService.talkWithAi(uid, dto));
    }


    /** 前端把 USER / AI 消息都回传此接口保存 */
    @PostMapping("/post")
    public Response<Integer> post(@RequestBody ChatPostDTO dto) {
        Integer uid = accountService.getInformation().getId();
        Integer realSessionId = chatService.post(uid, dto);
        return Response.buildSuccess(realSessionId);  // 前端若传 0，可用此 id 更新本地会话
    }

    /** 我的会话元数据 */
    @GetMapping("/sessions")
    public Response<List<ChatSessionMetaVO>> sessions() {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(chatService.listSessions(uid));
    }

    /** 会话历史消息 */
    @GetMapping("/messages/{sessionId}")
    public Response<List<ChatMessageVO>> messages(@PathVariable Integer sessionId) {
        Integer uid = accountService.getInformation().getId();
        return Response.buildSuccess(chatService.listMessages(uid, sessionId));
    }

}
