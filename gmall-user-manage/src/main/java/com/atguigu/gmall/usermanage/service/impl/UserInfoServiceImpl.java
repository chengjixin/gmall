package com.atguigu.gmall.usermanage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    //前缀，后缀，过期时间
    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;

    @Override
    public List<UserInfo> getUserInfoList() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressByUserId(String userId) {
        Example example = new Example(UserAddress.class);
        example.createCriteria().andEqualTo("userId",userId);
        return userAddressMapper.selectByExample(example);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //我们通过数据库表可以看出我们在数据库保存的密码都是加密过后的，所以我们同样需要用加密后的密码进行数据库查询
        String pwdMD5 = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        userInfo.setPasswd(pwdMD5);

        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (userInfo != null){
            //登录成功，将用户信息保存到redis中
            Jedis jedis = redisUtil.getJedis();
            //创建userKey，用户redis保存使用
            String userKey = userKey_prefix + info.getId() + userinfoKey_suffix;
            //redis选型：String 因为内容不经常修改
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));

            //资源关闭，并返回结果
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verfiy(String userId) {
        //通过传入的id，查询redis中是否有数据
        Jedis jedis = redisUtil.getJedis();

        //按照条件构造userKey
        String userKey = userKey_prefix + userId + userinfoKey_suffix;
        String userJson = jedis.get(userKey);

        if (userJson != null && userJson.length() > 0){
            //表示redis中含有数据，将Json字符串转换成对应的实体对象
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
