package com.qhzhou.service.impl;

import com.qhzhou.entity.BlogComments;
import com.qhzhou.mapper.BlogCommentsMapper;
import com.qhzhou.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
