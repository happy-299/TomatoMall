package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.controller.UtilController;
import com.seecoder.TomatoMall.vo.SearchHistoryVO;
import org.springframework.web.multipart.MultipartFile;
import com.seecoder.TomatoMall.vo.SearchResultVO;

import java.util.List;

public interface UtilService
{
    String upload(MultipartFile rawFileContent);

    String getCurrentTime();

    void updateFromVO(Object target, Object vo);

    SearchResultVO searchAll(String keyword);

    List<SearchHistoryVO> getSearchHistory();

}
