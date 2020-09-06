package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import com.atuigu.gmall.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import lombok.SneakyThrows;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    private PaymentInfoMapper  paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Reference
    private OrderService orderService;

    @Value("${appid}")
    private String appId;   //公众账号ID(唯一标识)

    @Value("${partner}")    //商户号
    private String mchId;

    @Value("${partnerkey}") //商户秘钥
    private String partnerkey;

    //保存paymenInfo信息到数据库中
    @Override
    public void savPaymentInfo(PaymentInfo paymentInfo) {
        //保存订单相关数据
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    //修改paymentinfo中的支付状态
    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo,String out_trade_no) {
        // update paymentInfo set payment_status = PAID ,callback_time = ? where out_trade_no = ?
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    //查询paymentInfo信息
    @Transactional()
    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
            return paymentInfoMapper.selectOne(paymentInfo);
    }

    //退款
    @SneakyThrows
    @Override
    public boolean refund(String orderId) {

        //通过orderId查询对应paymentInfo信息
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId",orderId);
        PaymentInfo paymentInfo = paymentInfoMapper.selectOneByExample(example);

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        HashMap<String, String> map = new HashMap<>();
        map.put("out_trade_no",paymentInfo.getOutTradeNo());           //查看文档,必填参数
        map.put("refund_amount",paymentInfo.getTotalAmount()+"");          //查看文档,必填参数

        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = alipayClient.execute(request);

        if(response.isSuccess()){
            System.out.println("调用成功");
            //退款成功后需要修改orderInfo和paymenInfo中的支付状态
            PaymentInfo paymentInfoUpd = new PaymentInfo();
            paymentInfoUpd.setPaymentStatus(PaymentStatus.ClOSED);
            paymentInfoUpd.setCallbackTime(new Date());
            updatePaymentInfo(paymentInfoUpd,paymentInfo.getOutTradeNo());

            //orderService.updateOrderInfoStatus();
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    /*
         微信支付(步骤)
            1.封装所需要的业务参数
            2.将业务参数发送给微信统一下单接口
            3.获取结果中的code_url
     */
    @Override
    public Map createNative(String orderId, String totalAmout) {

        //1.封装业务参数(详情查看微信开发平台网址:https://pay.weixin.qq.com/wiki/doc/api/native.php?chapter=9_1)
        HashMap<String, String> param = new HashMap<>();
        param.put("appid",appId);
        param.put("mch_id",mchId);
        param.put("nonce_str", WXPayUtil.generateNonceStr());
        param.put("body","过年买年货");
        param.put("out_trade_no",orderId);
        param.put("total_fee",totalAmout);
        param.put("spbill_create_ip","127.0.0.1");
        param.put("notify_url","http://v2q8627575.zicp.vip:20374/wx/callback/notify");
        param.put("trade_type","NATIVE");

        try {
            //2.微信统一下单结果所接受参数类型为:xml,所以我需要将map 转成  xml
            String xmlParam = WXPayUtil.generateSignedXml(param, partnerkey);
            //通过httpClient 发送数据
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");

            //设置发送的数据
            httpClient.setXmlParam(xmlParam);
            //使用https安全协议
            httpClient.setHttps(true);
            //设置访问方式 post
            httpClient.post();

            //3.获取结果中的code_url(注意:微信返回的数据类型同样也是xml格式,我们需要讲xml格式 转换为 可操作的数据类型)
            //获取执行结果
            String result = httpClient.getContent();
            Map<String, String> resultMap = WXPayUtil.xmlToMap(result);

            //构造一个集合,封装我们所需要的业务参数
            HashMap<String, String> map = new HashMap<>();
            map.put("code_url",resultMap.get("code_url"));    //二维码链接
            map.put("total_fee",totalAmout);            //总金额
            map.put("out_trade_no",orderId);            //订单id

            //4.保存微信交易记录到数据库
//            PaymentInfo paymentInfo = new PaymentInfo();
//            paymentInfo.setOutTradeNo(orderId);
//            paymentInfo.setOrderId("");
//            BigDecimal bigDecimal = new BigDecimal(totalAmout);
//            paymentInfo.setTotalAmount(bigDecimal);
//            paymentInfo.setSubject("过年买年货");
//            paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
//            paymentInfo.setCreateTime(new Date());
//            savPaymentInfo(paymentInfo);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //支付成功后，将支付结果和订单号以map的形式作为消息，发送给订单模块
    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        //通过工具类获取连接
        Connection connection = activeMQUtil.getConnection();
        try {
            //启动连接
            connection.start();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            //创建一个队列
            Queue queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            //创建一个消息生产者
            MessageProducer producer = session.createProducer(queue);
            //封装需要发送的消息
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId",paymentInfo.getOrderId());
            activeMQMapMessage.setString("result",result);
            //消息发送
            producer.send(activeMQMapMessage);
            //开启了事务，所有一定需要commit
            session.commit();

            //资源关闭
            producer.close();
            session.close();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }


    //延迟队列
    //检查订单状态（是否完成交易）

}
