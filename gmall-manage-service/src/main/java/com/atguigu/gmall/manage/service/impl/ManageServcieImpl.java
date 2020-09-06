package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.SkuSaleAttrValueMapper;
import com.atguigu.gmall.manage.mapper.SpuInfoMapper;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.lang3.StringUtils;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServcieImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private  SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private  SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private  SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired //从spring 容器中获取数据
    private RedisUtil redisUtil;

    /**
     * 查询一级分类信息
     * @return 一级分类集合
     * sql语句：select * form basecatalog1
     */
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    /**
     * 查询二级分类信息
     * @return 对应二级分类集合
     * sql语句：select * form basecatalog1
     */
    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    /**
     * 查询三级分类信息
     * @return 对应三级分类集合
     * sql语句：select * form basecatalog1
     */
    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        Example example = new Example(BaseCatalog3.class);
        example.createCriteria().andEqualTo("catalog2Id",catalog2Id);
        return baseCatalog3Mapper.selectByExample(example);
    }

    /**
     * 通过三级id查询对应的平台属性 （sku中需要平台属性和平台属性值）
     * @return 对应二级分类集合
     * sql语句：select * form basecatalog1
     */
    @Override
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        //sql:select * from base_attr_info where catalog3Id = 61
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);

        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    /**
     * 保存平台属性值
     * @return 对应二级分类集合
     * sql语句：
     *         1. INSERT INTO base_attr_info ( id,attr_name,catalog3_id ) VALUES( ?,?,? )
     *         2. INSERT INTO base_attr_value ( id,value_name,attr_id ) VALUES( ?,?,? )
     *      这里同课件上的不同
     */
    @Transactional //多表操作一定涉及事务
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if (baseAttrInfo.getId() == null){
            //保存平台属性
            baseAttrInfoMapper.insert(baseAttrInfo);
        }else {
            //修改平台属性
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
            //修改操作平台属性值，先删除，在添加
            Example example = new Example(BaseAttrValue.class);
            example.createCriteria().andEqualTo("attrId",baseAttrInfo.getId());
            baseAttrValueMapper.deleteByExample(example);
        }

        //int num = 1/0;

        //获取平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //判断平台属性值集合中是否有平台属性值
        if (attrValueList != null && attrValueList.size() > 0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //为平台属性值对象初始化attrId
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                //保存平台属性值
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }
    }


    /*                   -------------  SPU商品 操作 -----------------              */
    //获取平台属性值集合
    @Override
    public BaseAttrInfo getAttrValueList(String attrId) {
        /**
         * 查询平台属性用于数据回显，
         * 1、通过平台属性id查询对应的平台属性
         * 2，通过平台属性id查询对应的平台属性值
         * 3.将查询出来的平台属性质存放到平台属性对象当中
         */
        //1、通过平台属性id查询对应的平台属性
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //2，通过平台属性id查询对应的平台属性值
        Example example = new Example(BaseAttrValue.class);
        example.createCriteria().andEqualTo("attrId",attrId);
        List<BaseAttrValue> baseAttrValues = baseAttrValueMapper.selectByExample(example);
        if (baseAttrValues != null && baseAttrValues.size() > 0){
            //3.将查询出来的平台属性质存放到平台属性对象当中
            baseAttrInfo.setAttrValueList(baseAttrValues);
        }
        return baseAttrInfo;
    }

    //获取通过三级id，获取商品销售属性集合
    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    //获取基本销售属性
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    //保存spu信息（包含spuInfo spuImage spuSaleAttr spuSaleAttrValue）
    @Transactional //多表操作一定涉及事务
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //首先spuInfo添加到数据库中
        spuInfoMapper.insertSelective(spuInfo);
        //添加spuImage添加到数据库中
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList != null && spuImageList.size() > 0){
            for (SpuImage spuImage : spuImageList) {
                //舒适化spuId
                spuImage.setSpuId(spuInfo.getId());
                //添加spuImage到数据库中
                spuImageMapper.insertSelective(spuImage);
            }
        }

        //添加上平销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList != null && spuSaleAttrList.size() >0 ){

            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //初始化spuId
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //添加平台属性值集合到数据库中
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //初始化spuId
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }

    }

    /*                  ----------- SKU ------------------                   */
    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        //sql:select * from sku_Image where sku_id = {?}
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @AfterTransaction
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存sku库存单元信息，涉及四张表（skuInfo库存单元表 skuImage库存单元图片表 skuAttrValue平台属性 skuSaleAttrValue销售属性）
        //首先保存 skuInfo表的数据
        skuInfoMapper.insertSelective(skuInfo);

        //保存 skuImage 表的数据
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList != null && skuImageList.size() >0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        //保存 skuAttrValue平台属性
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList != null && skuAttrValueList.size() > 0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }

        //保存 skuSaleAttrValue销售属性
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }

    /**
     * redis 的使用：
     *      redis的使用涉及到两个问题，1.缓存击穿 2.缓存穿透
     *          1.缓存击穿：当我们在查询时，我们的某一个key值失效，那么此时的高并发会导致数据库压力增大，从而导致数据库瘫痪
     *              解决方案：使用分布式锁来解决问题
     *          2.缓存穿透：当我们在查询数据某一条不存在的数据，此时并不会进入缓存，而在此时的产生的高并发同样也会导致数据库瘫痪
     *              解决方案：如果我们在查询数据一条不存的的数据时，我们同样也将这条数据添加到redis当中，此时的value值：null
     *
     * redis类型选择；String 类型， 原因：因为我们商品详情页面的数据容易发生变动，为了方便修改我们采用set类型存储
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //return getSkuInfoBySet(skuId);
        return getSKuInfoByRedisson(skuId);
    }

    private SkuInfo getSKuInfoByRedisson(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
            try {
             jedis = redisUtil.getJedis();
             String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuInfoJson = jedis.get(skuInfoKey);
            if (skuInfoJson == null){
                //表示redis里面没有数据，直接通过数据库查询
                Config config = new Config();
                //设置redis节点信息
                config.useSingleServer().setAddress("redis://192.168.92.226:6379");
                //创建redisson实例
                RedissonClient redisson = Redisson.create(config);
                //创建分布式锁
                RLock lock = redisson.getLock("myLock");
                //尝试加锁，最多等待时间100秒，10秒后自动解锁 参数三：表示时间单位
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if (res){
                    try {
                        //表示上锁成功，从数据库中查询对应数据，并存入redis中
                        skuInfo = getSkuInfoDB(skuId);
                        if (skuInfo != null){
                            //查询成功，保存redis，并返回结果
                            jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                            return skuInfo;
                        }else {
                            //表示数据中没有数据，防止缓存穿透，需要给定一个默认值
                            jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,"");
                        }
                    } finally {
                        //解锁
                        lock.unlock();
                    }
                }

            }else {
                //reids里有数据，直接取出返回即可
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        //redis所在服务器宕机，直接通过数据库查询
        return getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoBySet(String skuId){
        
        //1.首先获取一个jedis
        Jedis jedis = null;
        SkuInfo skuInfo = null;
        try {
            jedis = redisUtil.getJedis();
            String  skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;

            String skuInfoJson = jedis.get(skuInfoKey);
            if (skuInfoJson != null && skuInfoJson.length() >0){
                //表示redis中有数据，并且数据有效
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
                return skuInfo;
            }else {
                //表示redis中没有数据，直接通过数据库查询 ,准备加锁 set k1 v1 px(毫秒) 过期时间 nx
                //需要定义分布式锁的key
                String skuInfoLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                //定义key的值
                String skuInfoLockValue = UUID.randomUUID().toString().replace("-","");
                //执行加锁命令
                String lockKey = jedis.set(skuInfoLockKey, skuInfoLockValue, "nx", "px", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    //如果相等，表示添加锁成功,然后进行数据库查询数据
                    skuInfo = getSkuInfoDB(skuId);
                    //判断我们通过skuId查询查来的skuInfo信息，是否为空，防止缓存穿透问题
                    if (skuInfo != null){
                        //将数据库中查询出来的数据存放到redis中
                        jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                    }else {
                        jedis.setex(skuInfoKey,ManageConst.SKUKEY_TIMEOUT,"");
                    }

                    //解锁，使用lua脚本
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuInfoLockKey),Collections.singletonList(skuInfoLockValue));

                    //返回skuInfo
                    return skuInfo;
                }else {
                    //表示添加所失败,目前已经有人在实行数据库查询操作
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }
        } catch (Exception e) {
            //如果获取redis失败，则需要抛出异常
            e.printStackTrace();
        } finally {
            //资源关闭
            if (jedis != null){
                jedis.close();
            }
        }
        //如果redis宕机的话，直接使用数据库查询
        return getSkuInfoDB(skuId);
    }

    //普通redis整合，或出现缓存击穿和缓存穿透，redis宕机问题
    private SkuInfo getSkuInfoByCommon(String skuId) {
        //redis使用：当请求进来，首先需要查询redis缓存，如果缓存中没有数据，则查询数据库，查询完毕，需要将数据更新到redis当中
        Jedis jedis = redisUtil.getJedis();

        SkuInfo skuInfo = null;
        //拼接查询redis时所需要用的key值
        String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
        //判断key值在数据库中是否存在
        if(jedis.exists(skuInfoKey)){
            //表示key值存在，那么通过key值，查询redis
            String skuInfoJson = jedis.get(skuInfoKey);
            if (skuInfoJson != null && skuInfoJson.length() > 0){
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);

            }
        }else {
            //key值不存在，表示缓存中没有数据，则直接通过数据库查询数据
            skuInfo = this.getSkuInfoDB(skuId);
            if (skuInfo != null){
                String skuInfoJson = JSON.toJSONString(skuInfo);
                //将数据库数据放入redis中
                jedis.set(skuInfoKey,skuInfoJson);
            }else {
                jedis.set(skuInfoKey,null);
            }

        }
        //资源关闭
        jedis.close();
        //返回最终数据
        return skuInfo;
    }

    //通过skuId 在数据库中查询对应的skuInfo信息
    private SkuInfo getSkuInfoDB(String skuId) {
        //sql:select * from sku_Info where skuId = 66;
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);

        //通过skuId查询图片信息，并封装到skuInfo当中
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuInfo.setSkuImageList(skuImageMapper.select(skuImage));

        //查询商品属性值id集合
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));
        return skuInfo;
    }

    @Override
    public List<SkuImage> getImageListBySkuId(String skuId) {
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        return skuImageMapper.select(skuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(List<String> attrValueIdList) {
        //通过stringUtils 工具类，将集合转变成一个字符串
        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        //SELECT * FROM  base_attr_info bai INNER  JOIN base_attr_value bav ON bai.id=bav.attr_id WHERE bav.id in (83,120,13);
        System.out.println("传入的字符串：" + valueIds);
        return baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
    }


}
