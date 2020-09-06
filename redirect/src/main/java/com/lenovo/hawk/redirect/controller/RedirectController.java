package com.lenovo.hawk.redirect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

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
