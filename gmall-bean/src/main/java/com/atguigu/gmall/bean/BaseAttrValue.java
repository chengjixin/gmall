package com.atguigu.gmall.bean;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;

//基本属性值对象
@Data
public class BaseAttrValue implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    @Column
    private String valueName;
    @Column
    private String attrId;

    @Transient //用来实现面包屑的消失，主要记录着缺少我们所点击的面包屑的对应的valueIdd 的URL
    private  String urlParams;
}
