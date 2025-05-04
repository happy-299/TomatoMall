package com.seecoder.TomatoMall.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionMetaVO {
    private Integer id;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime lastTime;
    private Long messageCount;
}
