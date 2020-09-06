package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 将支付数据保存到数据库当中
     * @param paymentInfo
     */
    void savPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 修改paymentInfo中的支付状态
     * @param paymentInfo
     */
    void updatePaymentInfo(PaymentInfo paymentInfo,String out_trade_no);

    /**
     * 通过out_trade_no查询对应的paymentInfo信息
     * @param paymentInfoQuery
     * @return
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfoQuery);

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(String orderId);


    /**
     * 微信支付,用于生成页面需要的二维码(code_url)
     * @param orderId:订单编号
     * @param totalAmout:订单总金额
     * @return
     */
    Map createNative(String orderId, String totalAmout);

    /**
     * 支付成功后，发送消息给订单模块，修改对应的订单状态
     * @param paymentInfo：包含需要修改的订单的Id
     * @param result：支付结果
     */
    void sendPaymentResult(PaymentInfo paymentInfo, String result);
}
