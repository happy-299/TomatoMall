package com.seecoder.TomatoMall.vo;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHistoryVO
{
    private Integer id;
    private String keyword;
}
