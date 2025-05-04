package com.seecoder.TomatoMall.po;

import com.seecoder.TomatoMall.vo.ReviewVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Review {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Integer id;

    @Column(nullable = false)
    private Integer productId;

    @Column(nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false)
    private String content;

    private LocalDateTime createTime = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(
            name = "review_img",
            joinColumns = @JoinColumn(name = "review_id")
    )
    @OrderColumn(name = "idx")
    private List<String> reviewImgs = new ArrayList<>();

    public ReviewVO toVO() {
        ReviewVO vo = new ReviewVO();
        vo.setId(id);
        vo.setProductId(productId);
        vo.setUserId(userId);
        vo.setRating(rating);
        vo.setContent(content);
        vo.setCreateTime(createTime);
        // vo only care about url
        vo.setImages(reviewImgs);
        return vo;
    }
}
