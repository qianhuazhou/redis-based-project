package com.qhzhou.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;//逻辑过期时间
    private Object data;//shop
}
