package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.service.UtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.seecoder.TomatoMall.util.OssUtil;

@Service
public class UtilServiceImpl implements UtilService
{
    @Autowired
    OssUtil oss;

    @Override
    public String upload(MultipartFile rawFileContent)
    {
        return oss.upload(rawFileContent);
    }
}
