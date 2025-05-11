package com.sheikhimtiaz.vpp.config;

import com.sheikhimtiaz.vpp.model.BatteryQueryResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public ReactiveRedisTemplate<String, BatteryQueryResponse> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        RedisSerializationContext<String, BatteryQueryResponse> context = RedisSerializationContext
                .<String, BatteryQueryResponse>newSerializationContext(new StringRedisSerializer())
                .value(new Jackson2JsonRedisSerializer<>(BatteryQueryResponse.class))
                .build();

        return new ReactiveRedisTemplate<>(factory, context);

    }
}
