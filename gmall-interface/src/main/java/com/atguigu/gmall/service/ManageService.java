package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

//业务逻辑层接口
public interface ManageService {


    /**
     * 获取前端中以及分类信息
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 通过一级分类id，串对应的二级分类信息
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类id查询对应的三级分类信息
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 通过三级分类id查询对应的平台属性集合
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    /**
     * 保存平台属性（包含平台属性，平台属性值）
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 通过平台属性id，查询对应的平台属性和平台属性值
     * @param attrId
     */
    BaseAttrInfo getAttrValueList(String attrId);

    /**
     * 通过三级分类Id对应的商品属性
     * @param spuInfo:用于接收前端传入的的参数，对用封装了三级分类id（catalog3Id）
     * @return
     */
    List<SpuInfo> getSpuList(SpuInfo spuInfo);

    /**
     * 获取销售属性集合
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存商品信息
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);


    /**
     * 通过skuId获取库存单元图片集合
     * @param spuImage
     * @return
     */
    List<SpuImage> getSpuImageList(SpuImage spuImage);

    /**
     * 通过spuId获取商品的销售属性和销售属性值
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 保存sku库存单元信息，涉及四张表（skuInfo库存单元表 skuImage库存单元图片表 skuAttrValue平台属性 skuSaleAttrValue销售属性）
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 通过skuId查询对应的skuInfo信息
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 通过skuId查询对应的库存图片信息集合
     * @param skuId
     * @return
     */
    List<SkuImage> getImageListBySkuId(String skuId);

    /**
     * 通过spuId查询对应的销售属性和销售属性值集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 查询同一个spuId的商品销售属性和销售属性值的集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 通过平台属性值Id，获取对应的平台属性名称和平台属性值名称
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList);
}
