package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.UtilService;
import com.seecoder.TomatoMall.vo.Response;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

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
}
