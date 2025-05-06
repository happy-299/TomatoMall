package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface AccountRepository extends JpaRepository<Account, Integer>
{
    Account findByUsername(String username);

    Account findByUsernameAndPassword(String username, String password);

    // 增加关注数（关注别人）
    @Modifying
    @Query("UPDATE Account u SET u.followingCount = u.followingCount + 1 WHERE u.id = :userId")
    void incrementFollowingCount(@Param("userId") Integer userId);

    // 减少关注数（取消关注）
    @Modifying
    @Query("UPDATE Account u SET u.followingCount = u.followingCount - 1 WHERE u.id = :userId")
    void decrementFollowingCount(@Param("userId") Integer userId);

    // 增加粉丝数（被关注）
    @Modifying
    @Query("UPDATE Account u SET u.followerCount = u.followerCount + 1 WHERE u.id = :userId")
    void incrementFollowerCount(@Param("userId") Integer userId);

    // 减少粉丝数（被取消关注）
    @Modifying
    @Query("UPDATE Account u SET u.followerCount = u.followerCount - 1 WHERE u.id = :userId")
    void decrementFollowerCount(@Param("userId") Integer userId);

    //按用户名搜索
    @Query("SELECT a FROM Account a WHERE LOWER(a.username) LIKE %:keyword%")
    List<Account> searchAccounts(@Param("keyword") String keyword);

}
