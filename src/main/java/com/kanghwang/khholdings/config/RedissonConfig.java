package com.kanghwang.khholdings.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 로컬 호스트 주소를 직접 박아버립니다.
        // 주소 앞에 반드시 redis:// 가 있어야 에러가 안 납니다.
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        return Redisson.create(config);
    }
}
