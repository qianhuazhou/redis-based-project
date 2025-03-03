package com.qhzhou.service.impl;

import cn.hutool.json.JSONUtil;
import com.qhzhou.dto.Result;
import com.qhzhou.entity.ShopType;
import com.qhzhou.mapper.ShopTypeMapper;
import com.qhzhou.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.qhzhou.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result findQueryTypeList() {
        //1.在redis中查询
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(CACHE_SHOP_KEY, 0, -1);
        //2.判断redis中是否存在
        // * 从redis中拿取的是JSON格式，需要逐一转换为ShopType格式进行返回才可生效！！！
        if (shopTypeList != null && !shopTypeList.isEmpty()) {
            //3.redis中存在，直接返回
            List<ShopType> typeList = new ArrayList<>();
            for (String s : shopTypeList) {
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }
        //4.redis中不存在，根据id查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        //5.数据库中不存在，直接报错
        if (typeList.isEmpty()) {
            return Result.fail("暂无店铺类型数据！");
        }
        //6.数据库中存在，先把数据写入redis
        // * 从数据库中拿取的是ShopType格式，需要逐一转换为JSON格式才可保存到redis中！！！
        List<String> list = new ArrayList<>();
        for (ShopType shopType : typeList) {
            String s = JSONUtil.toJsonStr(shopType);
            list.add(s);
        }
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_KEY, list);
        //7.返回
        return Result.ok(typeList);
    }

}


