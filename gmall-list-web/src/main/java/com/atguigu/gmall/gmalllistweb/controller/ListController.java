package com.atguigu.gmall.gmalllistweb.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    //首页通过检索条件到商品列表展示页面
    @RequestMapping("list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        //设置默认分页，每页显示的记录数
        skuLsParams.setPageSize(1);

        //调用listService中的方法，通过es查询数据信息
        SkuLsResult skuLsResult = listService.search(skuLsParams);

        //页面渲染所需要的所有商品信息
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        //String skuLsInfoJson = JSON.toJSONString(search);

        //我们需要页面渲染平台属性名称和平台属性值名称（要求：首先获取到平台属性值Id的集合）
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrInfoList(attrValueIdList);

        //制作urlParam参数列表
        String urlParam = makeUrlParam(skuLsParams);

        //定义一个集合用来封装面包屑
        List<BaseAttrValue> crumbList = new ArrayList<>();

        /*
            需要：在点击平台属性的时候使平台属性消失
                步骤一：获取url中的平台属性值Id （skuLsParams对象中） 和 平台属性集合中的平台属性值Id
                步骤二：通过他们两之间的比较，如果相等，将集合中的元素删除，如果不等，不做操作
         */
        if (baseAttrInfoList != null && baseAttrInfoList.size() > 0){
            //通过迭代器，获取到平台属性
            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();

                //获取平台属性中的平台属性值集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    //baseAttrValue:平台属性集合中的平台属性值

                    if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){
                        for (String valueId : skuLsParams.getValueId()) {

                            //valueId:url中的平台属性值Id
                            //通过他们两之间的比较，如果相等，将集合中的元素删除，如果不等，不做操作
                            if(valueId.equals(baseAttrValue.getId())){
                                iterator.remove();

                                //我们在这里制作面包屑（面包屑的组成：平台属性名称：平台属性值名称）
                                String  crumbs = baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName();
                                //将我们的面包屑保存到对象实体中，并添加到集合中，用于页面的渲染
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();

                                String newUrlParams = makeUrlParam(skuLsParams, valueId);
                                //将不含我们所点击面包屑的urlParams 放入对象中
                                baseAttrValueed.setUrlParams(newUrlParams);
                                baseAttrValueed.setValueName(crumbs);

                                crumbList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }
        }

        //分页
        request.setAttribute("totalPages",skuLsResult.getTotalPages());     //总页数
        request.setAttribute("pageNo", skuLsParams.getPageNo());    //当前页

        //保存数据
        request.setAttribute("crumbList",crumbList);
        request.setAttribute("keyword",skuLsParams.getKeyword());
        request.setAttribute("urlParam",urlParam);
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);//平台属性名称和平台属性值名称
        request.setAttribute("skuLsInfoList",skuLsInfoList);//商品列表
        return "list";
    }

    //制作urlparams SkuLsParams 表示封装了请求参数的实体bean   valueIds表示我们想要取消面包屑的id
    private String makeUrlParam(SkuLsParams skuLsParams,String ... vauleIds) {
        String urlParam = "";
        //判断用户是否通过三级分类id进入的list.html 页面
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() >0){
            //http://list.gmall.com/list.html?catalog3Id=61
            urlParam = "catalog3Id=" + skuLsParams.getCatalog3Id();
        }
        //判断用户是否通过全文检索（keyword）进入的list.html 页面
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){
            //http://list.gmall.com/list.html?keyword=小米
            urlParam = "keyword=" + skuLsParams.getKeyword();
        }
        //判断用户是否点击了商品属性值名称（过滤），再次查询了页面
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){
            for (String valueId : skuLsParams.getValueId()) {

                if (vauleIds != null && vauleIds.length > 0){
                    //表示我们点击了取消面包屑，需要拼接一个不包含这个valueId参数列表
                    if (valueId.equals(vauleIds[0])){
                        //不拼接这个valueId
                        continue;
                    }
                }
                //urlParams 参数列表拼接
                urlParam += "&valueId=" + valueId;
            }
        }
        System.out.println("urlParam:" + urlParam);
        return urlParam;
    }
}
