package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;

//首页通过三级分类id或者全文检索跳转到商品展示页面需要传入的参数，所封装的对象
@Data
public class SkuLsParams implements Serializable {
    //SkuName 商品名字
    String  keyword;

    //三级分类id
    String catalog3Id;

    //平台属性值结合
    String[] valueId;

    //当前页，默认为第一页
    int pageNo=1;

    //每页显示条数
    int pageSize=20;
}
