package com.seecoder.TomatoMall.service;

import com.seecoder.TomatoMall.vo.AdvertisementVO;


import java.util.List;

public interface AdvertisementService
{
    List<AdvertisementVO> getAllAds();

    AdvertisementVO updateAdInfo(AdvertisementVO ad);

    AdvertisementVO createAd(AdvertisementVO ad);

    String deleteAdById(Integer id);
}
