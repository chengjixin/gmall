package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {

    //商品展示页面中的商品数据集合
    List<SkuLsInfo> skuLsInfoList;

    //总记录数
    long total;

    //总页数
    long totalPages;

    //平台属性值Id的结合，后期用来获取平台属性名称和平台属性值名称
    List<String> attrValueIdList;
}
