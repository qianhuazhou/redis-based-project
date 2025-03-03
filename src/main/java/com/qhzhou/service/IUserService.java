package com.qhzhou.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qhzhou.dto.LoginFormDTO;
import com.qhzhou.dto.Result;
import com.qhzhou.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IUserService extends IService<User> {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}
