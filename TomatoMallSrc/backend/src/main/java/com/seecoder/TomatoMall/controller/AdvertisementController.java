package com.seecoder.TomatoMall.controller;

import com.seecoder.TomatoMall.service.AdvertisementService;

import com.seecoder.TomatoMall.vo.AdvertisementVO;
import com.seecoder.TomatoMall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementController
{
    @Autowired
    AdvertisementService adService;


    @GetMapping()
    public Response<List<AdvertisementVO>> getAllAds()
    {
        return Response.buildSuccess(adService.getAllAds());
    }

    @PutMapping()
    public Response<AdvertisementVO> updateAdInfo(@RequestBody AdvertisementVO ad)
    {
        return Response.buildSuccess(adService.updateAdInfo(ad));
    }

    @PostMapping()
    public Response<AdvertisementVO> createAd(@RequestBody AdvertisementVO ad)
    {
        return Response.buildSuccess(adService.createAd(ad));
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteAdById(@PathVariable Integer id)
    {
        return Response.buildSuccess(adService.deleteAdById(id));
    }

}
