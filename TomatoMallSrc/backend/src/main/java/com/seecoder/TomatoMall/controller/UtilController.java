package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.po.Account;
import com.seecoder.TomatoMall.po.BookList;
import com.seecoder.TomatoMall.po.Product;
import com.seecoder.TomatoMall.service.UtilService;
import com.seecoder.TomatoMall.vo.PageResponseVO;
import com.seecoder.TomatoMall.vo.Response;
import com.seecoder.TomatoMall.vo.SearchHistoryVO;
import com.seecoder.TomatoMall.vo.SearchResultVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/utils")
public class UtilController
{
    @Resource
    UtilService utilService;

    @PostMapping("/upload")
    public Response<String> create(@RequestParam("rawFileContent") MultipartFile rawFileContent)
    {
        return Response.buildSuccess(utilService.upload(rawFileContent));
    }


    @GetMapping("/search")
    public Response<SearchResultVO> search(@RequestParam String keyword)
    {
        return Response.buildSuccess(utilService.searchAll(keyword));
    }

    @GetMapping("/history")
    public Response<List<SearchHistoryVO>> getHistory()
    {
        return Response.buildSuccess(utilService.getSearchHistory());
    }


}
