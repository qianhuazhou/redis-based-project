package com.qhzhou.service.impl;

import com.qhzhou.entity.UserInfo;
import com.qhzhou.mapper.UserInfoMapper;
import com.qhzhou.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
