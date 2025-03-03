package com.qhzhou.service.impl;

import com.qhzhou.entity.Blog;
import com.qhzhou.mapper.BlogMapper;
import com.qhzhou.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
