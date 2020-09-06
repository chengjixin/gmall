package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {
    /**
        通过用户ID查询对应的购物车信息和实时价格信息
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
