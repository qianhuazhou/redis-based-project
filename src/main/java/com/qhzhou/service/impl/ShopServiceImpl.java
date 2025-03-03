package com.qhzhou.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qhzhou.dto.Result;
import com.qhzhou.entity.Shop;
import com.qhzhou.mapper.ShopMapper;
import com.qhzhou.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qhzhou.utils.CacheClient;
import com.qhzhou.utils.RedisConstants;
import com.qhzhou.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.qhzhou.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //缓存穿透[null]
        //Shop shop = queryWithPassThrough(id);
        //Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);//使用工具类

        //互斥锁解决缓存击穿
        //Shop shop = queryWithMutex(id);

        //逻辑过期解决缓存击穿
        //Shop shop = queryWithLogicalExpire(id);
         Shop shop = cacheClient
                 .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);

        if(shop == null){
            return Result.fail("店铺不存在!");
        }

        //返回
        return Result.ok(shop);
    }


    /**
     * 缓存击穿[Method1. 逻辑过期]
     * @param id
     * @return
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR =Executors.newFixedThreadPool(10);
    public Shop queryWithLogicalExpire(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺换乘
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否承载
        if(StrUtil.isBlank(shopJson)){
            //3.存在,直接返回
            return null;
        }
        //4.命中,需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //5.1.未过期,直接返回店铺信息
            return shop;
        }
        //5.2.已过期,需要缓存重建
        //6. 缓存重建
        //6.1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //6.2.判断是否获取锁成功
        if(isLock){
            //TODO 6.3.成功: 开启独立线程,实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try{
                    //重建缓存
                    this.saveShop2Redis(id, 20L);
                }catch (Exception e){
                    throw new RuntimeException(e);
                }finally {
                    //释放锁
                    unlock(lockKey);
                }
            });

        }
        //6.4.返回过期商铺信息

        return shop;
    }

    /**
     * 缓存击穿[Method2. 互斥锁]
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺换乘
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否存在
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在,直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断命中的是否是null[缓存穿透]
        if(shopJson != null){
            //返回错误信息
            return null;
        }
        //4.实现缓存重建
        //4.1获取互斥锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try{boolean isLock = tryLock(lockKey);
            //4.2判断是否换区成功
            if(!isLock){
                //4.3失败: 休眠并重试
                Thread.sleep(50);
                return queryWithPassThrough(id);
            }
            //TODO 获取锁成功后,再次监测redis缓存是否存在,如果存在则无需重建缓存
            //4.4成功: 根据id查询数据库
            shop = getById(id);
            //模拟重建延时
            Thread.sleep(200);
            //5.不存在,返回错误
            if(shop == null){
                //将null写入redis[缓存穿透]
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //6.存在,写入redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        }catch (InterruptedException e){
            throw new RuntimeException(e);
        }finally {
            //7.释放互斥锁
            unlock(lockKey);
        }
        //8.返回
        return shop;
    }

    /**
     * 缓存穿透
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id){
        String key = CACHE_SHOP_KEY + id;
        //1.从redis查询商铺换乘
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //2.判断是否承载
        if(StrUtil.isNotBlank(shopJson)){
            //3.存在,直接返回

            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断命中的是否是null[缓存穿透]
        if(shopJson != null){
            //返回错误信息
            return null;
        }

        //4.不存在,根据id查询数据库
        Shop shop = getById(id);
        //5.不存在,返回错误
        if(shop == null){
            //将null写入redis[缓存穿透]
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //6.存在,写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //7.返回
        return shop;
    }

    /**
     * 获取锁
     * @param key
     * @return
     */
    private  boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key
     */
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        //1.查询店铺数据
        Shop shop = getById(id);
        Thread.sleep(200);
        //2.封装逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺id不能为空");
        }
        //1.更新数据库
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
