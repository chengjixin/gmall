package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.service.PaymentService;
import com.atuigu.gmall.util.IdWorker;
import com.atuigu.gmall.util.StreamUtil;
import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PXpayController {

    @Reference
    private PaymentService paymentService;

    @Value("${partnerkey}") //商户秘钥
    private String partnerkey;

    /*  微信支付需要需要调用微信的统一下单接口 https://api.mch.weixin.qq.com/pay/unifiedorder
            返回值:我们通过前端可以知道在点击微信支付的时候,会触发一个Ajax请求,返回值data(map类型)
            需要接受的参数:orderId(订单Id) 和 totalAmout(总金额)
     */
    @RequestMapping("wx/submit")
    @ResponseBody
    public Map wxSubmit(){
        //调用Idwork工具类生成一个随机的orderId
        IdWorker idWorker = new IdWorker();
        long Id = idWorker.nextId();
        String orderId = "" + Id;

        //调用后台控制器(参数一:订单Id,参数二:订单总金额),返回一个map集合(集合中包含生成二维码的code_url)
        Map map = paymentService.createNative(orderId,"1");
        String code_url = (String) map.get("code_url");
        System.out.println("codeUrl:" + code_url);
        return map;
    }

    @RequestMapping("wx/callback/notify")   //微信支付成功之后的回调方法
    public String wxNotify(HttpServletResponse response, HttpServletRequest request) throws Exception {
        System.out.println("微信支付成功后的回调方法");
        ServletInputStream inputStream = request.getInputStream();

        //通过streamUtil工具类,将我们获取到数据流对象,转换成我们所需要的数据
        String xmlString = StreamUtil.inputStream2String(inputStream, "utf-8");
        
        //微信验签,成功返回true,失败返回false
        if (WXPayUtil.isSignatureValid(xmlString,partnerkey)){
            //微信接受和返回的都是xml格式的文件,所有都是需要我们转换成可以操作的对象
            Map<String, String> paramMap = WXPayUtil.xmlToMap(xmlString);
            String resultCode = paramMap.get("result_code");

            if (resultCode != null && "SUCCESS".equals(resultCode)){
                //表示支付成功,需要修改交易记录的状态(同支付宝一致)

                // return_code  return_msg 返回给商户
                // 声明一个map,用来封装业务需要参数
                HashMap<String, String> map = new HashMap<>();
                map.put("return_code","SUCCESS");
                map.put("return_msg","OK");

                System.out.println("交易编号："+paramMap.get("out_trade_no")+"支付成功！");
                //将数据map 转换成 xml格式
                String returnXml = WXPayUtil.mapToXml(map);
                //设置xml的格式
                response.setContentType("text/xml");
                //return_code return_msg
                return returnXml;
            }
        }
        //验签失败
        System.out.println("验签失败");
        HashMap<String, String> map = new HashMap<>();
        map.put("return_code","FAIL");
        return WXPayUtil.mapToXml(map);

        
    }
}
