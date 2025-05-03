package com.seecoder.TomatoMall.repository;

import com.seecoder.TomatoMall.po.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFollowRepository extends JpaRepository<UserFollow, Integer> {
    boolean existsByFollowerIdAndFollowedId(Integer followerId, Integer followedId);

    @Modifying
    @Query("DELETE FROM UserFollow uf WHERE uf.followerId = :followerId AND uf.followedId = :followedId")
    void deleteByFollowerIdAndFollowedId(@Param("followerId") Integer followerId, @Param("followedId") Integer followedId);

    // 获取关注列表
    @Query("SELECT uf.followedId FROM UserFollow uf WHERE uf.followerId = :userId")
    List<Integer> findFollowingIds(@Param("userId") Integer userId);

    // 获取粉丝列表
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followedId = :userId")
    List<Integer> findFollowerIds(@Param("userId") Integer userId);


}
