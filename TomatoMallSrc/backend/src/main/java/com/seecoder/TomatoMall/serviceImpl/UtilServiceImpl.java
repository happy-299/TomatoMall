package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.service.UtilService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.seecoder.TomatoMall.util.OssUtil;

import java.beans.PropertyDescriptor;

@Service
public class UtilServiceImpl implements UtilService
{
    @Autowired
    OssUtil oss;

    private final static Boolean IS_TEST = true;

    @Override
    public String upload(MultipartFile rawFileContent)
    {
        return oss.upload(rawFileContent);
    }

    @Override
    public String getCurrentTime()
    {
        return oss.getCurrentTime();
    }

    @Override
    public void updateFromVO(Object target, Object vo)
    {
        BeanWrapper srcWrap = new BeanWrapperImpl(vo);
        BeanWrapper targetWrap = new BeanWrapperImpl(target);
        for (PropertyDescriptor pd : srcWrap.getPropertyDescriptors())
        {
            String propertyName = pd.getName();
            Object value = srcWrap.getPropertyValue(propertyName);
            if (value != null && !pd.getName().equals("class") && !propertyName.equals("specifications"))//仅更新非空字段
            {
                targetWrap.setPropertyValue(propertyName, value);
            }
        }
    }


}
