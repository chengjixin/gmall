package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.cart.config.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;


@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;
    /*
    业务逻辑思路：
        1.  先查看数据库中是否有该商品
            select * from cartInfo where userId = ? and skuId = ?
            true: 数量相加upd
            false: 直接添加
        2.  放入redis！
     */
    @Override
    public void addToCart(String skuId, String skuNum,String userId) {
        //查询购物车中是否有同样的商品
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("skuId",skuId).andEqualTo("userId",userId);
        CartInfo cartInfo = cartInfoMapper.selectOneByExample(example);

        if (cartInfo == null){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo1 = new CartInfo();
            //表述数据库 购物车中没有该商品信息，直接添加
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(Integer.parseInt(skuNum));
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setSkuPrice(skuInfo.getPrice());     //实时价格

            cartInfoMapper.insertSelective(cartInfo1);

            cartInfo = cartInfo1;
        }else {
             //表述数据中该商品信息，数量相加upd ,修改数据
             Integer newSkuNum = cartInfo.getSkuNum() + Integer.parseInt(skuNum);
             cartInfo.setSkuNum(newSkuNum);
             //初始化实时价格
            cartInfo.setSkuPrice(cartInfo.getCartPrice());
             cartInfoMapper.updateByPrimaryKey(cartInfo);
        }
        Jedis jedis = redisUtil.getJedis();
        //构建一个购物车的key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //保存到redis中
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfo));
        //设置key的过期时间
        setCartkeyExpireTime(userId,jedis,cartKey);

        //资源的关闭
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /*
        1.  先获取缓存中的数据
            1.1 true:
                直接查询并返回集合
            1.2 false:
                查询数据库，将数据放入缓存 并返回集合
                    涉及到查询一下实时价格：skuInfo.price
         */
        //用来封装购物车对象
        List<CartInfo> cartInfoList = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        // 定义key：user:userId:cart
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        List<String> stringList  = jedis.hvals(cartKey);


        if (stringList != null && stringList.size() > 0) {
            //表示redis有数据
            for (String cartInfoJson : stringList) {
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //排序,按照加入购物车的时间先后(也就是cartId的创建时间)
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
        } else {
           //表示redis中没有我们想要的数据,需要查询数据库,然后将查询出来了的数据放入redis中
            cartInfoList = loadCartCache(userId);
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartNoLoginList, String userId) {
        /**
         * 合并购物车:我们需要通过比对登录的购物车和未登录的购物车信息,如果有skuId相同的商品,
         *      则需要数量相加,如果不相同的商品则直接插入到购物车当中()
         */
        //创建一个集合用来封装最终的购物车信息,并返回
        List<CartInfo> cartInfoList = new ArrayList<>();
        //首先我们需要得到已登录的购物车信息
        List<CartInfo> cartLoginList = cartInfoMapper.selectCartListWithCurPrice(userId);
        //循环比对
        //定义一个标记用来标识skuId是否相同
        boolean isMatch = false;
        //判断登录时的购物车有相关数据
        if (cartLoginList != null & cartLoginList.size() > 0){
            for (CartInfo cartNologinInfo : cartNoLoginList) {
                for (CartInfo cartLoginInfo : cartLoginList) {
                    //判断未登录的商品和已登录的商品的skuId是否相同
                    if (cartNologinInfo.getSkuId().equals(cartLoginInfo.getSkuId())){
                        //那么商品数量相加
                        cartLoginInfo.setSkuNum(cartNologinInfo.getSkuNum() + cartLoginInfo.getSkuNum());
                        //并修改数据库信息
                        cartInfoMapper.updateByPrimaryKeySelective(cartLoginInfo);
                        //修改表示信息
                        isMatch = true;
                        //TODO 操作redis
                    }
                }
                if (!isMatch){
                    //表示商品Id不相同,直接添加到数据库
                    cartNologinInfo.setId(null);
                    cartNologinInfo.setUserId(userId);
                    //添加到数据库中
                    cartInfoMapper.insertSelective(cartNologinInfo);

                    cartInfoList.add(cartNologinInfo);
                    //TODO 操作redis
                }
            }
        }else {
            //表示登录状态的购物车中没有数据,我们直接将临时购物车的数据添加到数据库
            for (CartInfo cartNologinInfo : cartNoLoginList) {
                //表示商品Id不相同,直接添加到数据库
                cartNologinInfo.setId(null);
                cartNologinInfo.setUserId(userId);
                //添加到数据库中
                cartInfoMapper.insertSelective(cartNologinInfo);

            }
            //TODO 操作redis
        }
        //因为我们添加或修改完数据中的相关数据,那么直接通过userId查询数据库,并将数据中的信息放入缓存即可
        cartInfoList = loadCartCache(userId);

        //合并未登录时的购物车
        for (CartInfo cartLoginInfo : cartLoginList) {
            for (CartInfo cartNoLoginInfo : cartNoLoginList) {
                //表示为同一件商品
                if (cartLoginInfo.getSkuId().equals(cartNoLoginInfo.getSkuId())){
                    //那么我们将我们选中的商品保存到数据库
                    if ("1".equals(cartNoLoginInfo.getIsChecked())){
                        // 更改数据库的状态
                        cartLoginInfo.setIsChecked("1");
                        //调用选中的方法
                        checkCart(cartLoginInfo.getUserId(),userId,"1");
                    }
                }
            }
        }
        return cartInfoList;
    }

    //删除购物车信息
    @Override
    public void deleteCartList(String userTempId) {
        //通过userI的删除购物车信息
        Example example = new Example(CartInfo.class);
        //sql:delete from cart_info where userId = ?
        example.createCriteria().andEqualTo("userId",userTempId);
        cartInfoMapper.deleteByExample(example);

        //清空缓存中的先关购物车信息
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userTempId + CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);

        //关闭资源
        jedis.close();
    }

    //修改购物车的勾选状态
    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        //1.首先修改数据库然
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);

        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        //参数一:需要修改的属性 参数二:修改时需要的条件
        //update into cartInfo ischecked = ? where userId = ? and skuId = ?
        cartInfoMapper.updateByExampleSelective(cartInfo,example);

        //2.修改缓存redis
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //通过key获取redis中的数据
        String cartInfoJson = jedis.hget(cartKey, skuId);
        //转换成实体对象,并修改勾选的状态
        CartInfo cartInfoUpd = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfoUpd.setIsChecked(isChecked);

        //覆盖原有的redis数据
        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoUpd));

        //关闭资源
        jedis.close();
    }

    //通过用户Id,获取对应用户购物车中所有选中的商品
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        /*
            为什么我们可以直接通过reids直接获取购物车数据?
                因为我们这个操作是在生成订单时候的操作(也就是去结算),
                而我们在去去结算的时候后去已经将购物车中的数据更新到缓存中,所以这里可以直接获取
         */
        //定义个集合用来封装购物车信息
        List<CartInfo> cartInfoList = new ArrayList<>();
        Jedis jedis = redisUtil.getJedis();

        // 数据类型：jedis.hset(key,field,value);
        // key = user:userId:cart  field=skuId  value=cartInfo.toString();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;

        List<String> cartInfoJsonList = jedis.hvals(cartKey);
        for (String cartInfoJson : cartInfoJsonList) {
            CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
            if ("1".equals(cartInfo.getIsChecked())){
                //表示所有选中的商品信息
                cartInfoList.add(cartInfo);
            }
        }
        //资源的关闭
        jedis.close();
        return cartInfoList;
    }

    //根据userId查询数据库中的购物车信息,然后将查询出来的数据放入redis当中
    public List<CartInfo> loadCartCache(String userId) {
        //我们需要通过UserId查询购物车相关信息和商品的实时价格
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);

        if (cartInfoList != null && cartInfoList.size() > 0){
            //表示有数据,保存到redis中
            Jedis jedis = redisUtil.getJedis();
            String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            //设置到数据库
            HashMap<String, String> map = new HashMap<>();
            for (CartInfo cartInfo : cartInfoList) {
                map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            }
            jedis.hmset(cartKey,map);
            //设置超时时间
            setCartkeyExpireTime(userId,jedis,cartKey);
        }
        return cartInfoList;
    }

    //设置保存在redis中购物车信息的过期时间
    private void setCartkeyExpireTime(String userId, Jedis jedis, String cartKey) {
        //我们设置过期时间需要参考redis中用户的过期时间
        String userKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;
        //表示用户key存在,登录
        if (jedis.exists(userKey)){
            //获取这个用户的过期时间
            Long timeOut = jedis.ttl(userKey);

            //设置购物车信息的过期时间
            jedis.expire(cartKey,timeOut.intValue());
        }else{
            //不存在,直接设置过期时间
            jedis.expire(cartKey,7*24*3600);
        }
    }
}
