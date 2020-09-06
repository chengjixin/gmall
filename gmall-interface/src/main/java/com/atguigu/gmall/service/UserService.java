package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

/**
 * 业务逻辑层
 */
public interface UserService {

    /**
     * 查询所有用户信息
     */
    List<UserInfo> getUserInfoList();

    /**
     * 通过用户Id查询用户地址信息
     */
    List<UserAddress> getUserAddressByUserId(String userId);

    /**
     * 用户登录
     * @param userInfo 用户用户登录输入的信息（包含有户名，密码）
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 解析token
     * @param userId
     * @return
     */
    UserInfo verfiy(String userId);
}
