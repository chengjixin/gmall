package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 将商品添加到购物车中
     * @param skuId
     * @param skuNum
     */
    void addToCart(String skuId, String skuNum,String userId);

    /**
     * 通过userId查询对应的购物车信息
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并临时购物车
     * @param cartNoLoginList 临时购物车集合
     * @param userId 用户Id
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartNoLoginList, String userId);

    /**
     * 通过用户Id,删除购物车信息(含有数据库和redis中的数据)
     * @param userTempId
     */
    void deleteCartList(String userTempId);

    /**
     * 修改购物车的勾选状态
     * @param userId
     * @param skuId
     * @param isChecked
     */
    void checkCart(String userId, String skuId, String isChecked);

    /**
     * 获取选中的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户Id,跟新缓存中的数据
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
