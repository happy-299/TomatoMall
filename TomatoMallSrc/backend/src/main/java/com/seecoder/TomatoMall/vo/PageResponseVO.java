package com.seecoder.TomatoMall.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class PageResponseVO<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total;

    public PageResponseVO(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.total = page.getTotalElements();
    }
}

