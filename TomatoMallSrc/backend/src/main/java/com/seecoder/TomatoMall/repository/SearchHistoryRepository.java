package com.seecoder.TomatoMall.repository;


import com.seecoder.TomatoMall.po.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Integer>
{
    List<SearchHistory> findAllByAccount_Id(Integer accountId);
}
