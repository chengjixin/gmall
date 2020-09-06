package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    //利用注解来获取消息的监听工厂
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage){
        //获取订单发送来的消息
        try {
            String result = mapMessage.getString("result");
            String orderId = mapMessage.getString("orderId");

            if ("success".equals(result)){
                //表示支付成功，修改订单状态信息
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);

                //修改完订单状态后，发送消息给库存，用于库存修改
                orderService.sendOrderStatus(orderId);

                //发送完消息给库存，再次修改订单状态(已通知仓储)
                orderService.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //库存处理消息完成后，需要发送一条消息给订单模块，用于提示用户再次修改订单状态为代发货
    @JmsListener(destination = "SKU_DEDUCT_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeSkuDeduct(MapMessage mapMessage){
        try {
            String orderId = mapMessage.getString("orderId");
            String status = mapMessage.getString("status");
            if ("DEDUCTED".equals(status)){
                //表示减库存成功，修改订单状态(代发货)
                orderService.updateOrderStatus(orderId,ProcessStatus.WAITING_DELEVER);
            }else {
                //表示减库存失败，修改订单状态(库存异常)
                orderService.updateOrderStatus(orderId, ProcessStatus.STOCK_EXCEPTION);
                //发送一条消息队列，调用其他的仓库
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

}
