package com.seecoder.TomatoMall.vo;

import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.BookList;
import com.seecoder.TomatoMall.po.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SearchResultVO
{
    private List<partAccount> accounts;
    private List<ProductVO> products;
    private List<partBookList> bookLists;

    public SearchResultVO(List<Account> account, List<Product> product, List<BookList> bookList)
    {
        this.products = product.stream().map(Product::toVO).collect(Collectors.toList());

        this.bookLists = new ArrayList<>();
        bookList.forEach(l ->
        {
            this.bookLists.add(partBookList.builder()
                    .id(l.getId())
                    .creationDate(l.getCreateTime())
                    .title(l.getTitle())
                    .creatorId(l.getCreator().getId())
                    .creatorAvatar(l.getCreator().getAvatar())
                    .creatorName(l.getCreator().getUsername())
                    .description(l.getDescription())
                    .favouriteCount(l.getFavoriteCount())
                    .build());
        });

        this.accounts = new ArrayList<>();
        account.forEach(acc ->
        {
            this.accounts.add(partAccount.builder()
                    .id(acc.getId())
                    .username(acc.getUsername())
                    .avatar(acc.getAvatar())
                    .followerCount(acc.getFollowerCount())
                    .followingCount(acc.getFollowingCount())
                    .build());
        });


    }

    @Setter
    @Getter
    @Builder
    private static class partAccount//隐藏敏感信息
    {
        private Integer id;
        private String username;
        private String avatar;
        private Integer followerCount;
        private Integer followingCount;
    }

    @Setter
    @Getter
    @Builder
    private static class partBookList//隐藏敏感信息
    {
        private Integer id;
        private String title;
        private Integer creatorId;
        private String description;
        private String creatorName;
        private String creatorAvatar;
        private LocalDateTime creationDate;
        private Integer favouriteCount;

    }

}




