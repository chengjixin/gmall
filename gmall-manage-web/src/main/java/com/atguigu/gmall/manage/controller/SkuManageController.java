package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {
    //http://localhost:8082/attrInfoList?catalog3Id=61  回显平台属性，平台属性值(该控制器在ManagerController)
    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;


    //http://localhost:8082/spuImageList?spuId=60       回显商品图片集合
    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(SpuImage spuImage){
        return manageService.getSpuImageList(spuImage);
    }

    //http://localhost:8082/spuSaleAttrList?spuId=61   回显销售属性，销售属性值
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId){
        return manageService.getSpuSaleAttrList(spuId);
    }

    //http://localhost:8082/saveSkuInfo             保存sku库存单元信息（涉及四张表）
    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        //正常情况下，在我们保存商品信息的时候，需要审核，然后做上架操作（报存到es中），
    }

    @RequestMapping("onSale")
    public void onSale(String skuId){

        SkuLsInfo skuLsInfo = new SkuLsInfo();
        //初始化skuLsInfo信息,将skuInfo中的属性相关信息，保存到skuLsInfo当中
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        BeanUtils.copyProperties(skuInfo,skuLsInfo);

        //商品上架
        listService.saveSkuLsInfo(skuLsInfo);
    }

}
