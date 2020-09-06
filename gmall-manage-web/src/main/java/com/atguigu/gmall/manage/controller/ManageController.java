package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController     //作用一：表示作为前端可控制器层 作用二：表示返回结果为Json数据，用于前端渲染
@CrossOrigin        //解决访问控制器层中的跨域问题
public class ManageController {

    @Reference
    private ManageService manageService;

    //http://localhost:8082/getCatalog1 查询一级分类
    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    //http://localhost:8082/getCatalog2?catalog1Id=4 通过一级分类id，查询二级分类
    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);
    }

    //http://localhost:8082/getCatalog3?catalog2Id=17 通过二级分类Id，查询三级分类
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    //http://localhost:8082/attrInfoList?catalog3Id=100 通过三级分类Id，查询对应的平台属性 和 平台属性值（编写sku时，需要用到平台属性中的平台属性值）
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.attrInfoList(catalog3Id);
    }

    //http://localhost:8082/saveAttrInfo 保存平台属性值（设计两张表，baseattrinfo，baseattrvalue）
    //@RequestBody ：前端传入的数据为Json，这个注解可以将json数据转换为java中的对象
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }

    //http://localhost:8082/getAttrValueList?attrId=23  通过平台属性id查询对应的平台属性
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo baseAttrInfo = manageService.getAttrValueList(attrId);
        return baseAttrInfo.getAttrValueList();
    }

}
