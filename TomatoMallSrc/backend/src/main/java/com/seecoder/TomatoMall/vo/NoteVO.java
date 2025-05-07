package com.seecoder.TomatoMall.vo;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import java.util.List;

@Getter
@Setter
@Builder
public class NoteVO
{
    private Integer id;
    private String title;
    private String content;
    private String img;
    private Integer price;
    private Integer creatorId;
    private List<Integer> payersId;
    private LocalDateTime createTime;
    private Integer viewCnt;
    private Integer likeCnt;
}
