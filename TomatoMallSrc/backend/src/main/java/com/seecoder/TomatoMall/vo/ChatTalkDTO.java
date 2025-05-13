package com.seecoder.TomatoMall.vo;

import lombok.Data;

@Data
public class ChatTalkDTO {
    private Integer sessionId;   // 0 = 新会话
    private String  content;     // 用户输入
}
