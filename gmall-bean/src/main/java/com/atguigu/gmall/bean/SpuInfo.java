package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

//spu对象 商品
@Data
public class SpuInfo implements Serializable {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column
    private String spuName;

    @Column
    private String description;

    @Column
    private  String catalog3Id;

    // 销售属性
    @Transient // 非数据库字段，业务需要的字段
    private List<SpuSaleAttr> spuSaleAttrList;

    @Transient // spu图片，商品图片
    private List<SpuImage> spuImageList;
}
