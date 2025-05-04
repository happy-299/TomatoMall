package com.seecoder.TomatoMall.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageVO {
    private Integer id;
    private String role;
    private String content;
    private LocalDateTime time;
}

