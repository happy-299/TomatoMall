package com.seecoder.TomatoMall.serviceImpl;

import com.seecoder.TomatoMall.exception.TomatoMallException;
import com.seecoder.TomatoMall.po.Advertisement;
import com.seecoder.TomatoMall.po.Product;
import com.seecoder.TomatoMall.repository.AdvertisementRepository;
import com.seecoder.TomatoMall.repository.ProductRepository;
import com.seecoder.TomatoMall.service.AdvertisementService;

import com.seecoder.TomatoMall.vo.AdvertisementVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdvertisementServiceImpl implements AdvertisementService
{

    @Autowired
    AdvertisementRepository adRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UtilServiceImpl utilServiceImpl;

    @Override
    @Transactional
    public List<AdvertisementVO> getAllAds()
    {
        return adRepository.findAll().stream()
                .map(Advertisement::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdvertisementVO updateAdInfo(AdvertisementVO ad)
    {
        Advertisement savedAd = adRepository.findById(ad.getId())
                .orElseThrow(TomatoMallException::adNotFound);
        Product product = productRepository.findById(ad.getProductId())
                .orElseThrow(TomatoMallException::productNotFound);

        //只进行非空字段的更新
        utilServiceImpl.updateFromVO(savedAd, ad);

        adRepository.save(savedAd);
        return savedAd.toVO();

    }

    @Transactional
    @Override
    public AdvertisementVO createAd(AdvertisementVO ad)
    {
        productRepository.findById(ad.getProductId())
                .orElseThrow(TomatoMallException::productNotFound);
        return adRepository.save(ad.toPO()).toVO();
    }

    @Override
    @Transactional
    public String deleteAdById(Integer id)
    {
        Advertisement savedAd = adRepository.findById(id)
                .orElseThrow(TomatoMallException::adNotFound);
        adRepository.deleteById(id);
        return "删除成功";
    }
}
