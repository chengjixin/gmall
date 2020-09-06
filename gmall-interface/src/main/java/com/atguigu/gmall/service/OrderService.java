package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

//订单模块 业务层
public interface OrderService {
    //保存订单信息,并返回订单号
    String saveOrder(OrderInfo orderInfo);

    /**
     * 生成订单流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 校验订单流水号是否正确
     * @param userId
     * @param tradeNo
     */
    boolean checkTradeNo(String userId, String tradeNo);

    /**
     * 删除订单流水号(redis)
     * @param userId
     */
    void delTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(String skuId, Integer skuNum);

    /**
     * 通过orderId查询对应的订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(String orderId);

    /**
     * 通过orderId 修改订单状态
     * @param orderId
     * @param paid
     */
    void updateOrderStatus(String orderId, ProcessStatus paid);

    /**
     * 根据订单Id，发送消息到库存系统
     * @param orderId
     */
    void sendOrderStatus(String orderId);
}
