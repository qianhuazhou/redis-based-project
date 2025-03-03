package com.qhzhou.service;

import com.qhzhou.dto.Result;
import com.qhzhou.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IShopTypeService extends IService<ShopType> {

    Result findQueryTypeList();
}
