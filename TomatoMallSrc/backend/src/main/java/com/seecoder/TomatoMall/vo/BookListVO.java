package com.seecoder.TomatoMall.vo;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookListVO {
    private Integer id;
    private String title;
    private Integer creatorId;
    private String description;
    private List<ProductVO> products;

    private String creatorName;
    private String creatorAvatar;

    private LocalDateTime creationDate;
    private Integer favouriteCount;
}
