package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {
    /**
     * 商品上架
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 基于dls语句的查询
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 修改es中的热点值（hotScore），从而达到热点排序的功能
     * @param skuId
     */
    void incrHotScore(String skuId);

}
