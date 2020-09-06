package com.lenovo.hawk.redirect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 我们使用 thymeleaf 的时候,需要注意的是,返回页面不能使用restController 和 responseBody,
 * 并且thymeleaf页面跳转只能通过控制器来实现
 */
@Controller
public class RedirectController {
    @RequestMapping(value = "/loginPage",method = RequestMethod.GET)
    public String test1(){
        return "index";
    }

    @RequestMapping(value = "/login",method = RequestMethod.POST)
    @ResponseBody
    public String login(Map user){
        return "successful";
    }
}
