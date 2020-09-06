package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atuigu.gmall.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {

        //初始化部分orderInfo信息 用户Id,总金额,订单状态,订单编号,创建和失效时间,进程状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID); //订单状态,默认未支付

        String outTradeNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);    //订单编号

        orderInfo.setCreateTime(new Date());    //创建时间

        //通过日历对象,获取以当前时间为节点,一天后的过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());      //过期时间

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);   //进程状态,默认状态未支付

        orderInfo.sumTotalAmount(); //总金额

        //保存订单信息,orderInfo, orderDetail
        orderInfoMapper.insertSelective(orderInfo);

        // orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setId(null);
            orderDetail.setOrderId(orderInfo.getId());
            //保存到数据库
            orderDetailMapper.insertSelective(orderDetail);
        }
        //返回订单编号
        return orderInfo.getId();
    }

    //为了防止表单的重复提交，我们使用一个订单流水号标识订单唯一性，并把订单流水号存放到redis中，使用String类型
    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();

        //生成订单流水号的key
        String tradeNoKey = "user:"+userId+":tradeCode";

        String tradeNoValue = UUID.randomUUID().toString().replace("-","");

        //将流水号保存到redis中,过期时间为24小时
        jedis.setex(tradeNoKey,60*21,tradeNoValue);

        //资源关闭
        jedis.close();
        return tradeNoValue;
    }

    @Override
    public boolean checkTradeNo(String userId, String tradeNo) {
        Jedis jedis = redisUtil.getJedis();

        //生成订单流水号的key
        String tradeNoKey = "user:"+userId+":tradeCode";
        //从redis中获取订单流水号
        String tradeCode = jedis.get(tradeNoKey);
        //判断是否一致
        if (tradeNo.equals(tradeCode)){
            return true;
        }else{
            return false;
        }

    }

    @Override
    public void delTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();

        //生成订单流水号的key
        String tradeNoKey = "user:"+userId+":tradeCode";

        String tradeCode = jedis.get(tradeNoKey);

        // jedis.del(tradeNoKey);
        String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        jedis.eval(script, Collections.singletonList(tradeNoKey),Collections.singletonList(tradeCode));

        //资源关闭
        jedis.close();
    }

    //验证库存，需要参数商品id（skuId）和商品数量（skuNum） ，验证成功返回“1”，失败返回“0”
    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //远程调用库存接口方法 http://www.gware.com/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);

    }

    //通过orderId,主键查询order信息
    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);

        //封装参数
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);
        //初始化订单明细
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    //通过订单id，修改订单的相关状态
    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);                                 //订单编号
        orderInfo.setOrderStatus(processStatus.getOrderStatus()); //订单状态
        orderInfo.setProcessStatus(processStatus);                //订单进程状态
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }


    @Override
    public void sendOrderStatus(String orderId) {
        //发送消息到库存系统(获取连接，并启动)
        Connection connection = activeMQUtil.getConnection();
        //通过orderId查询对应的信息，封装库存系统所需要的参数列表
        String orderJson = initWareOrder(orderId);
        try {
            connection.start();
            //创建session对象
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建队列
            Queue queue = session.createQueue("ORDER_RESULT_QUEUE");
            //创建消息提供者
            MessageProducer producer = session.createProducer(queue);

            ActiveMQTextMessage message = new ActiveMQTextMessage();
            message.setText(orderJson);
            //发送消息
            producer.send(message);
            //开启事务，提交
            session.commit();

            //资源关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    //根据订单Id，查询订单中字段组成json的字符串
    private String initWareOrder(String orderId) {
        OrderInfo orderInfo = getOrderInfo(orderId);

        //将orderInfo中的部分字段封装到map集合中，并转换为Json字符串
        Map map = initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    private Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());                       //订单id
        map.put("consignee",orderInfo.getConsignee());              //收货人信息
        map.put("consigneeTel", orderInfo.getConsigneeTel());       //收货人电话
        map.put("orderComment",orderInfo.getOrderComment());        //订单备注
        map.put("orderBody","过年买年货");                           //订单概要
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());  //订单收货地址
        map.put("paymentWay","2");                                  //付款方式：1 表示货到付款(目前暂不支持) 2 表示在线支付
        map.put("wareId",orderInfo.getWareId());                    //仓库信息

        //封装订单明细信息，声明一个hashmap用于存放订单明细信息
        ArrayList<Map> orderDetails = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //订单明细格式：{skuId:101,skuNum:1,skuName:’小米手64G’}
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());             //商品ID
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());           //商品数量
            orderDetailMap.put("skuName",orderDetail.getSkuName());         //商品名称
            orderDetails.add(orderDetailMap);
        }

        // 订单明细放入map
        map.put("details",orderDetails);
        return map;
    }
}
