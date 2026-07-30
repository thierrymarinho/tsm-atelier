package com.tm.tsm_atelier.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
@org.springframework.cache.annotation.EnableCaching
public class RedisConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
				.allowIfBaseType("com.tm.tsm_atelier.")
				.allowIfBaseType("java.util.")
				.allowIfBaseType("java.lang.")
				.allowIfBaseType("java.time.")
				.allowIfBaseType("java.math.")
				.build();

		JsonMapper objectMapper = JsonMapper.builder().findAndAddModules()
				.activateDefaultTyping(ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY).build();

		GenericJacksonJsonRedisSerializer serializer = new GenericJacksonJsonRedisSerializer(objectMapper);

		RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(10))
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
				.disableCachingNullValues();

		return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(cacheConfig).build();
	}
}
