package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManagerController {

    @Reference
    private ManageService manageService;

    //http://localhost:8082/spuList?catalog3Id=61       通过三级分类Id，查询对应的商品属性
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuList(SpuInfo spuInfo){
        return manageService.getSpuList(spuInfo);
    }

    //http://localhost:8082/baseSaleAttrList            获取商品的销售属性集合
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    //http://localhost:8082/saveSpuInfo                 保存spu信息
    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
    }
}
