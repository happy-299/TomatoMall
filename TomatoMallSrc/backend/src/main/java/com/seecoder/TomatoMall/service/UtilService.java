package com.seecoder.TomatoMall.service;

import org.springframework.web.multipart.MultipartFile;

public interface UtilService
{
    String upload(MultipartFile rawFileContent);

    String getCurrentTime();

    void updateFromVO(Object target, Object vo);

}
