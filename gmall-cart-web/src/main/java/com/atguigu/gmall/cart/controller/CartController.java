package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import tk.mybatis.mapper.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {
    @Reference
    private ManageService manageService;

    @Reference
    private CartService cartService;

    //http://cart.gmall.com/addToCart 将商品添加到购物车中
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        /*
          我们需要将商品详情信息，和我们购买的商品数量保存到域中，在下一个页面渲染使用
         */
        String skuId = request.getParameter("skuId");
        //获取商品购买的数量
        String skuNum = request.getParameter("skuNum");
        /*
            获取userId的思路流程:
                在拦截器中,如果我们登录了,并且认证成功之后,我们将userId保存到了request域,
         */
        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录？
        if (userId == null){
            // 用户未登录 存储一个临时的用户Id，存储在cookie 中!
            userId = CookieUtil.getCookieValue(request, "user-key", false);

            if(userId == null){
                //表示用户从未添加过购物车,那么我们需要添加一个临时的userId
               userId = UUID.randomUUID().toString().replace("-", "");

               CookieUtil.setCookie(request,response,"user-key",userId,7*24*3600,false);
            }
        }
        //并将我们所选中的商品添加购物车
        cartService.addToCart(skuId,skuNum,userId);
        //获取商品详情信息
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //保存数据
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    //查询购物车
    /*
        购物车合并思路:首先我们判断用户登录状态
            未登录
                未登录则不合并
            已登录
                已登录的话,我们判断cookie中的临时user是否有购物车信息,如果有,需要合并未登录的购物车
                并将数据库和redis中的相关信息删除,同时清空cookie
     */
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        List<CartInfo> cartInfoList = new ArrayList<>();
        //首先判断用户是否登录,登录了通过userId查询对应的购物车信息
        String userId = (String) request.getAttribute("userId");
        if(userId != null){
            //表示已登录
            //需要合并购物车,从cookie中获取临时的userId信息
            String userTempId = CookieUtil.getCookieValue(request, "user-key", false);
            //判断这个临时的userId是否为空, 空:表示用户从来没有添加过购物车 不为空:表示用户的添加过购物车
            if (!StringUtil.isEmpty(userTempId)){
                //不为空,则通过这个临时的userId查询用户在未登录的情况下添加的购物车信息
                List<CartInfo> cartNoLoginList = cartService.getCartList(userTempId);

                //调用service层,合并购物车
                cartInfoList = cartService.mergeToCartList(cartNoLoginList,userId);
                //调用service层,通过临时的用户Id删除临时购物车
                cartService.deleteCartList(userTempId);
            }
            cartInfoList = cartService.getCartList(userId);
        }else{
            //表示未登录,那么我们就需要获取临时的userId信息
            String userTempId = CookieUtil.getCookieValue(request, "user-key", false);
            if (!StringUtil.isEmpty(userTempId)) {
                cartInfoList = cartService.getCartList(userTempId);
            }
        }

        //保存数据,页面渲染
        request.setAttribute("cartInfoList",cartInfoList);
        String orginUrl = (String) request.getAttribute("orginUrl");
        request.setAttribute("orginUrl",orginUrl);
        String nickName = (String) request.getAttribute("nickName");
        request.setAttribute("nickName",nickName);
        return "cartList";
    }

    //http://cart.gmall.com/checkCart
    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request){
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");
        //userId为空,表示用户没有登录
        if(StringUtils.isEmpty(userId)){
            userId = CookieUtil.getCookieValue(request, "user-key", false);
        }

        //调用实现层,修改购物车的勾选状态
        cartService.checkCart(userId,skuId,isChecked);
    }

    //http://cart.gmall.com/toTrade 去结算
    //1.需要获取到用户的相关信息 2.需要获取到用户在购物车中所有选中状态的商品列表
    @RequestMapping("toTrade")
    @LoginRequire
    public String toTrade(HttpServletRequest request){
        //细节处理:因为我们在上面的合并购物车中并没有对未登录的进行处理,所以在这里,我们需要对未登录时候的购物车进行合并
        //获取用户Id
        String userId = (String) request.getAttribute("userId");

        String userTempId = CookieUtil.getCookieValue(request, "user-key", false);

        if (StringUtils.isEmpty(userTempId)){
            //表示我们曾经使用过临时用户添加购物车
            List<CartInfo> cartNoLoginList = cartService.getCartList(userTempId);
            //判断购物车中是否有数据
            if (cartNoLoginList != null && cartNoLoginList.size() > 0){
                //合并购物车,并删除临时购物车中的信息
                cartService.mergeToCartList(cartNoLoginList,userId);

                cartService.deleteCartList(userTempId);
            }
        }
        return "redirect://trade.gmall.com/trade";
    }
}
