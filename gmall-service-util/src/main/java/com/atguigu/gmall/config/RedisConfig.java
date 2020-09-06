package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {
    /**
     * 一共是两步操作：
     *      1.首先需要获取到配置文件中的相关参数
     *      2.将redisUtil交由spring容器管理
     */
    //获取配置文件中的相关参数，并赋予默认值
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;

    //2.将redisUtil交由spring容器管理
    @Bean
    public RedisUtil getRedisUtil(){
        //首先需要判断先关属性是否装配成功
        if("disabled".equals(host)){
            //表示我们没有提供host，则应该返回一个null
            return null;
        }
        //否则初始化redisUtil对象
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host,port,timeOut);
        return redisUtil;
    }
}
