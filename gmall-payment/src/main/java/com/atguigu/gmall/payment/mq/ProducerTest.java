package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

//消息提供者
public class ProducerTest {
    public static void main(String[] args) throws JMSException {
        /**
         * 消息队列的流程步骤
         *      1.创建一个链接对象
         *      2.通过连接对象创建连接并开启连接
         *      3.通过连接对象创建Session对象
         */
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.92.226:61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();
        //通过连接对象获取session对象，参数一：是否开启事务 参数二:消息的签收方式
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        //创建一个队列，用于存储消息
        Queue queueName = session.createQueue("Atguigu");
        //通过session创建一个消息提供者，参数：队列（也就是我们的消息发送到哪里去？）
        MessageProducer producer = session.createProducer(queueName);

        ActiveMQTextMessage message = new ActiveMQTextMessage();
        message.setText("新年快乐");
        //通过消息提供者发送消息
        producer.send(message);

        //关闭资源（注意：如果开启了事务，那么一定需要commit提交）
        producer.close();
        session.close();
        connection.close();
    }
}
