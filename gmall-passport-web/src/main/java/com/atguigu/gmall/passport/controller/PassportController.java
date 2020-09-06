package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.config.JwtUtil;
import com.atguigu.gmall.service.UserService;
import io.jsonwebtoken.Jwt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    private UserService userService;


    @Value("${token.key}")
    private String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        //获取url上的参数，originUrl：用于记录用户是从哪一个页面链接跳转到登录的页面
        String originUrl = request.getParameter("originUrl");

        //保存数据
        request.setAttribute("originUrl",originUrl);
        return "index";
    }

    /*
         我们在这里需要使用一个工具JWT
            定义：是为里在网络环境中传递声明而执行的一个基于Json的开放标准
            作用：对token信息的防伪
            组成：公共部分，私有部分，签名部分
     */
    //http://localhost:8087/login
    @RequestMapping(value = "login",method = RequestMethod.POST)
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        //sql : select * from userInfo where username = ? and pwd = ?
        //用户登录逻辑
        UserInfo info = userService.login(userInfo);
        if (info != null){
            //登录成功

            HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());

            String salt = request.getHeader("X-forwarded-for");
            String token = JwtUtil.encode(key, map, salt);

            return token;
        }
        return "fail";
    }


    //用户的认证
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //需求：从缓存中获取用户信息，判断用户是否登录
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        //token解密，返回值为一个包含私有部分信息的map集合
        Map<String, Object> map = JwtUtil.decode(token, key, salt);

        if (map != null && map.size() > 0) {

            String userId = (String) map.get("userId");

            UserInfo userInfo = userService.verfiy(userId);
            //判断
            if (userInfo != null){
                return "success";
            }
        }
        return "fail";
    }

}
