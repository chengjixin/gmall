package com.atguigu.gmall.bean;

import lombok.Data;

import javax.naming.directory.SearchResult;
import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

//基本属性对象
@Data
public class BaseAttrInfo implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY) //表示主键自增策略
    private String id;
    @Column
    private String attrName;
    @Column
    private String catalog3Id;

    @Transient //该注解用于修饰不属于数据库字段，但是业务需求真实存在的字段
    private List<BaseAttrValue> attrValueList;

}
