package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.controller.ChatController;
import com.seecoder.TomatoMall.vo.ChatMessageVO;
import com.seecoder.TomatoMall.vo.ChatSessionMetaVO;

import java.util.List;

public interface ChatService {
    /** 保存一条消息；sessionId=0 时自动创建新会话并返回真实 id */
    Integer post(Integer accountId, ChatController.ChatPostDTO dto);

    /** 我的会话 meta 列表 */
    List<ChatSessionMetaVO> listSessions(Integer accountId);

    /** 会话完整历史 */
    List<ChatMessageVO> listMessages(Integer accountId, Integer sessionId);
}
