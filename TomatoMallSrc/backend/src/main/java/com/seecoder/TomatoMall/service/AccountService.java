package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.vo.AccountVO;

import java.util.List;

public interface AccountService {
    Boolean register(AccountVO accountVO);

    String login(String username, String password);

    AccountVO getInformation();

    Boolean updateInformation(AccountVO accountVO);

    Boolean isFollowed(Integer currentUserId, Integer followId);

    void followUser(Integer followerId, Integer followedId);

    void unfollowUser(Integer followerId, Integer followedId);

    List<AccountVO> getFollowingList(Integer userId);

    List<AccountVO> getFollowerList(Integer userId);

    List<AccountVO> getUserByVerifiedName(String verifiedName);
}
