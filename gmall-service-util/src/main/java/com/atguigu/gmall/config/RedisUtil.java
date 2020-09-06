package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtil {
    private JedisPool jedisPool;

    public void initJedisPool(String host,int port,int timeOut){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(200);               //最大线程数
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);    //最长等待时间
        jedisPoolConfig.setMinIdle(10);                 //最少剩余数
        jedisPoolConfig.setBlockWhenExhausted(true);    //排列等待
        jedisPoolConfig.setTestOnBorrow(true);          //获取redis时，自动检测redis是否可用
        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }

    //获取jedis对象
    public Jedis getJedis(){
        return jedisPool.getResource();
}

}
