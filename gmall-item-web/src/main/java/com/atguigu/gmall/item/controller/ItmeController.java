package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;


@Controller
public class ItmeController {

    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    @LoginRequire(autoRedirect = false)   //表示访问详情页面一定需要登录
    @RequestMapping("{skuId}.html")
    public String skuInfoPage(@PathVariable("skuId") String skuId, HttpServletRequest request){

        Executors.newCachedThreadPool();
        //1.我们需要通过skuId查询出对应skuInfo信息，用于页面的加载
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //2.查询对应的销售属性和销售属性值集合,并根据skuId锁定对应商品信息
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //3.查询对应的销售属性值集合与skuId的联系，用于下一步的字符串拼接使用
        List<SkuSaleAttrValue> skuSaleAttrValues =  manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        //5.开始进行拼接字符串
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        if (skuSaleAttrValues != null && skuSaleAttrValues.size() > 0){
            for (int i = 0;i < skuSaleAttrValues.size();i++){
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValues.get(i);
                //拼接模板：{"属性值1|属性值2":sku1，"属性值1|属性值2":sku2，"属性值1|属性值2":sku3}
                if (key.length() > 0){
                    key += "|";
                }
                key += skuSaleAttrValue.getSaleAttrValueId();
                //拼接规则:1.循环结束，拼接结束 2.skuId改变，拼接结束
                if ((i + 1) == skuSaleAttrValues.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValues.get(i+1).getSkuId())){
                    //拼接结束，将对应的key和key所对应的skuId，保存到map集合当中
                    map.put(key,skuSaleAttrValue.getSkuId());
                    //并清空key值
                    key = "";
                }
            }
        }
        String valuesSkuJson  = JSON.toJSONString(map);
        System.out.println(valuesSkuJson);

        //通过拦截器,获取我们当前页面转码后的url
        String orginUrl = (String) request.getAttribute("orginUrl");

        //保存数据
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        //将skuInfo存放到session当中，便于前端页面thymeleaf渲染使用
        request.setAttribute("skuInfo",skuInfo);

        //将销售属性和销售属性值集合
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);

        request.setAttribute("orginUrl",orginUrl);
        //记录热点数据
        listService.incrHotScore(skuId);

        return "item";
    }
}
