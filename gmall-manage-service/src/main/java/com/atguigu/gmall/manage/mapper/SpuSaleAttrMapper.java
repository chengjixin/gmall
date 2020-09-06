package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    /*
        通过spuId查询对应商品的销售属性和销售属性值
     */
    List<SpuSaleAttr> selectSpuSaleAttrList(String spuId);

    /*
        通过spuId查询对应的商品销售属性和商品的销售属性值集合，然后通过skuId锁定对应的商品属性
     */
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String skuId, String spuId);
}
