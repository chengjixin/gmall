package com.atguigu.gmall.payment.mq;


import lombok.SneakyThrows;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQSession;

import javax.jms.*;
import javax.management.Query;

//消息消费者
public class ConsumerTest {
    public static void main(String[] args) throws JMSException {
        //1.创建一个连接工厂对象，通过连接工厂对象创建一个链接，并启动
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.92.226:61616");
        Connection connection = connectionFactory.createConnection();
        connection.start();

        Session session = connection.createSession(false, ActiveMQSession.AUTO_ACKNOWLEDGE);

        Queue queue = session.createQueue("Atguigu");
        MessageConsumer consumer = session.createConsumer(queue);

        consumer.setMessageListener(new MessageListener() {
            @SneakyThrows
            @Override
            public void onMessage(Message message) {
                if (message instanceof TextMessage){
                    String text = ((TextMessage) message).getText();
                    System.out.println("接受的消息为：" + text);

                }
            }
        });
    }
}
