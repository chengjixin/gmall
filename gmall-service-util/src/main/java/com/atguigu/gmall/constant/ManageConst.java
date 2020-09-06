package com.atguigu.gmall.constant;

public class ManageConst {

    public static final String SKUKEY_PREFIX="sku:";    //前缀

    public static final String SKUKEY_SUFFIX=":info";   //后缀

    public static final int SKUKEY_TIMEOUT=7*24*60*60;  //过期时间

    public static final int SKULOCK_EXPIRE_PX=10000;    //分布式锁的过期毫秒数

    public static final String SKULOCK_SUFFIX=":lock";  //锁的后缀


}
