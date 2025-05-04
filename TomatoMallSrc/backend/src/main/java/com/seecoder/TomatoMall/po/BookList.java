package com.seecoder.TomatoMall.po;

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
public class BookList {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Basic
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Account creator;

    @Basic
    private String description;

    @OneToMany(mappedBy = "bookList",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<BookListItem> items = new ArrayList<>();

    private LocalDateTime createTime = LocalDateTime.now();

    private Integer favoriteCount = 0;

}
