package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.controller.ChatController;
import com.seecoder.TomatoMall.vo.ChatMessageVO;
import com.seecoder.TomatoMall.vo.ChatReplyVO;
import com.seecoder.TomatoMall.vo.ChatSessionMetaVO;
import com.seecoder.TomatoMall.vo.ChatTalkDTO;

import java.util.List;

public interface ChatService {

    ChatReplyVO talkWithAi(Integer accountId, ChatTalkDTO dto);

    /** 保存一条消息；sessionId=0 时自动创建新会话并返回真实 id */
    Integer post(Integer accountId, ChatController.ChatPostDTO dto);

    /** 我的会话 meta 列表 */
    List<ChatSessionMetaVO> listSessions(Integer accountId);

    /** 会话完整历史 */
    List<ChatMessageVO> listMessages(Integer accountId, Integer sessionId);
}
