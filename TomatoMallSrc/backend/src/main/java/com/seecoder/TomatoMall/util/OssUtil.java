package com.seecoder.TomatoMall.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.seecoder.TomatoMall.exception.TomatoMallException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Component
@Getter
@Setter
@NoArgsConstructor
@ConfigurationProperties("aliyun.oss")  //作用是可以加载配置文件中的值到你的bean属性中
public class OssUtil {
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    //传入(文件名字，MultipartFile.getInputStream),返回ossUrl;
    // oss中会以"fileName-Date"附加时间戳作为实际文件名
    public String upload(MultipartFile rawFileContent) {
        InputStream inputStream = null;
        try {
            inputStream = rawFileContent.getInputStream();
        } catch (IOException e) {
            throw TomatoMallException.noFileContent();
        }
        String objectName = addTimeToName(rawFileContent.getOriginalFilename());//附加时间戳解决重复文件问题

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
        try {
            ossClient.putObject(putObjectRequest);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        String retUrl = ossClient.generatePresignedUrl(bucketName, objectName, new Date()).toString().split("\\?Expires")[0];
        if (retUrl == null || retUrl.equals("")) {
            throw TomatoMallException.ossInternalWrong();
        }
        return retUrl;
    }

    public String addTimeToName(String originalName) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);

        int lastDotIndex = originalName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // 无扩展名，直接附加时间戳
            return originalName + "_" + timestamp;
        } else {
            // 分离名称和扩展名
            String namePart = originalName.substring(0, lastDotIndex);
            String extension = originalName.substring(lastDotIndex);
            return namePart + "_" + timestamp + extension;
        }

    }
}
