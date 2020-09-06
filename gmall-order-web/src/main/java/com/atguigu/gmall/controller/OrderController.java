package com.atguigu.gmall.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;
    
    @Reference
    private ManageService manageService;

    //确认订单页面
    // http://localhost:8081/trade?userId=1 trade：交易，贸易，买卖
    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        //获取用户Id
        String userId = (String) request.getAttribute("userId");

        //获取用户的收货地址
        List<UserAddress> userAddressList = userService.getUserAddressByUserId(userId);

        //获取选中的购物车列表
        //声明一个集合用来封装订单明细信息
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();
        //购物车列表信息
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        for (CartInfo cartInfo : cartInfoList) {
            //初始化订单明细信息
            OrderDetail orderDetail = new OrderDetail();

            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());

            orderDetailList.add(orderDetail);
        }

        //OrderInfo 中包含总价格
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        //计算一下总价格
        orderInfo.sumTotalAmount();
        System.out.println("总价格" + orderInfo.getTotalAmount());

        //生成订单流水号,用于做表单恶意提交校验
        String tradeNo = orderService.getTradeNo(userId);

        //保存数据
        request.setAttribute("tradeNo",tradeNo);
        request.setAttribute("userAddressList",userAddressList);
        request.setAttribute("orderDetailList",orderDetailList);
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "trade";
    }

    //http://trade.gmall.com/submitOrder
    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request,OrderInfo orderInfo){
        /*
        1.保存单据前要做交易：验库存。
        2.保存单据: orderInfo orderDetail。
        3.保存以后把购物车中的商品删除。
        4.重定向到支付页面。
         */
        //初始化部分orderInfo信息 用户Id,总金额,订单状态,订单编号,创建和失效时间,进程状态
        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);   //用户id

        //为了防止表单的恶意重复提交,我们需要验证订单流水号
        //获取页面中的订单流水号
        String tradeNo = request.getParameter("tradeNo");
        //校验订单流水号是否一致
        boolean flag = orderService.checkTradeNo(userId, tradeNo);
        if (!flag){
            //表示订单校验胜败
            request.setAttribute("errMsg","请勿重复提交订单！");
            return "tradeFail";
        }
        //订单流水号校验成功之后,删除订单流水号
        orderService.delTradeNo(userId);

        //为了订单有效,验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {

            //通过httpClient远程调用，验证库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(),orderDetail.getSkuNum());
            if (!result){
                //验证库存不通过(库存不足)
                request.setAttribute("errMsg",orderDetail.getSkuName() + "库存不足,请及时联系客服！");
                return "tradeFail";
            }
            //价格验证
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
            if (res != 0){
                //表示价格验证失败
                request.setAttribute("errMsg",orderDetail.getSkuName() + ":价格有所变动,请重新下单！");
                //及时更新缓存中的价格
                cartService.loadCartCache(orderInfo.getUserId());
                return "tradeFail";
            }
        }

        //保存订单信息
        String orderId = orderService.saveOrder(orderInfo);

        //我们生成订单号之后，需要在订单过期的时候发送一条消息，用于关闭订单
        return  "redirect://payment.gmall.com/index?orderId="+orderId;
    }
}
