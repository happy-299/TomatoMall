package com.seecoder.TomatoMall.vo;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartAccountVO {
    private Integer id;
    private String username;
    private String avatar;
    private Integer followerCount;
    private Integer followingCount;
    private Boolean isVerified;
    private String verifiedName;
    private Integer firstLogin;


}
