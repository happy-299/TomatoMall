package com.seecoder.TomatoMall.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatReplyVO {
    private Integer sessionId;
    private String  reply;
    private LocalDateTime time;
}

