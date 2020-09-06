package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static org.apache.catalina.manager.Constants.CHARSET;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentService paymentService;

    //http://payment.gmall.com/index?orderId=115 付款页面显示
    @RequestMapping("index")
    @LoginRequire
    public String index(HttpServletRequest request){
        //通过查看页面我们可以得知,页面需要用户名,订单号,和总金额,方便渲染
        String nickName = (String) request.getAttribute("nickName");

        String orderId = request.getParameter("orderId");
        //通过订单Id,查询对应的订单号和该订单的总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //页面渲染所需数据
        request.setAttribute("nickName",nickName);                          //用户名，
        request.setAttribute("orderId",orderInfo.getId());                  //订单编号
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());    //订单总金额
        return "index";
    }

    //payment.gmall.com/alipay/submit   支付页面点击支付宝支付 -->立即支付（按钮）
    @RequestMapping("alipay/submit")
    @ResponseBody
    public String aliPaySubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
       /*
            思路:我们在点击立即支付的时候,需要做两件事
                第一件:将支付的数据保存到数据库当中
                第二件:生成支付所需要的二维码
        */
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //构建一个orderInfo对象,并初始化对应属性
        PaymentInfo paymentInfo = new PaymentInfo();

        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());     //总金额
        paymentInfo.setSubject("过年买年货");                        //标题
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);         //支付状态
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());       //订单中已生成的对外交易编号。订单中获取
        paymentInfo.setOrderId(orderInfo.getId());                  //订单id
        paymentInfo.setCreateTime(new Date());                      //创建时间
        //1.保存信息
        paymentService.savPaymentInfo(paymentInfo);

        //2.生成二维码 ,首先初始化alipayclient 客户端对象
        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE); //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request，用于封装业务参数
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);    //支付成功后的同步回调
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);    //在公共参数中设置回跳和通知地址(异步通知)
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no", paymentInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",paymentInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
        //将参数封装到Alipayrequest对象中,生成二维码
        alipayRequest.setBizContent(JSON.toJSONString(map));
//        alipayRequest.setBizContent("{" +
//                "    \"out_trade_no\":\"20150320010101001\"," +
//                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\"," +
//                "    \"total_amount\":88.88," +
//                "    \"subject\":\"Iphone6 16G\"," +
//                "    \"body\":\"Iphone6 16G\"," +
//                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\"," +
//                "    \"extend_params\":{" +
//                "    \"sys_service_provider_id\":\"2088511833207846\"" +
//                "    }"+
//                "  }");//填充业务参数
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=" + CHARSET);
//        response.getWriter().write(form);//直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();
        return form;
    }

    //http://payment.gmall.com/alipay/callback/return  同步通知
    @RequestMapping("alipay/callback/return")
    public String callbackReturn(){
        //当支付成功的时候,重定向到订单列表,主要显示是给用户使用
        return "redirect:" + AlipayConfig.return_order_url;
    }


    //http://v2q8627575.zicp.vip:20374/alipay/callback/notify 异步通知
    /*
            异步回调:主要是给商家使用,一共两个作用
                作用一:确认并记录用户已付款
                作用二:通知电商模块
             接受到回调需要做的事:
                1.验证回调信息的真伪
                2.验证用户付款的成功与否
                3.把新的支付状态写入支付信息表
                4.通知电商
                5.把支付宝返回回执
         */
    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestBody Map<String,String> map,HttpServletRequest request) throws AlipayApiException {//将异步通知中收到的所有参数都存放到map中
        //交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功
        String trade_status = map.get("trade_status");
        String out_trade_no = map.get("out_trade_no");

        //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = AlipaySignature.rsaCheckV1(map, AlipayConfig.alipay_public_key, CHARSET, AlipayConfig.sign_type); //调用SDK验证签名
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                //需要注意:如果我们paymentInfo中的支付状态本来就是paid,那么表示该订单已完成支付,那么则不能通过验签
                //首先通过out_trade_no,获取对应的paymentInfo信息
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);
                if (paymentInfoQuery.getPaymentStatus() == PaymentStatus.PAID || paymentInfoQuery.getPaymentStatus() == PaymentStatus.ClOSED){
                    //表示已经支付过,订单已关闭
                    return "failure";
                }

                //表示支付成功,需要更改paymentInfo中的支付状态(通过out_trade_no更改对应信息的payment_status)
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUpd.setCallbackTime(new Date());
                paymentService.updatePaymentInfo(paymentInfoUpd,out_trade_no);

                //如果支付成功需要想订单发送一条消息，从而修改对应订单的进程状态,需要传入订单Id，支付状态
                paymentService.sendPaymentResult(paymentInfo,"success");
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    //退款
    @RequestMapping("refund")
    @ResponseBody
    public String refund(String orderId){
        HashMap a = new HashMap();
        a.put("s","d");
        boolean flag = paymentService.refund(orderId);
        if (flag){
            //表示退款成功
            return "true";
        }else {
            //表示退款失败
            return "false";
        }


    }

    //http:trade.gmall.com/sendPaymentResult?orderId=xxx&result=xxx
    //测试消息发送(参数一：支付订单号orderId 参数二：支付结果)
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo, @RequestParam("result") String result){
        paymentService.sendPaymentResult(paymentInfo,result);
        return "sent payment result";
    }

}
