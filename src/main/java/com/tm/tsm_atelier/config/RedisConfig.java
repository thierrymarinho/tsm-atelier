package com.tm.tsm_atelier.config;

import com.tm.tsm_atelier.common.dto.CustomPageImpl;
import com.tm.tsm_atelier.domain.collection.dto.CollectionResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductResponseDTO;
import com.tm.tsm_atelier.domain.product.dto.ProductSummaryDTO;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

	private static final Duration TTL = Duration.ofMinutes(10);

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		JsonMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
		TypeFactory typeFactory = objectMapper.getTypeFactory();

		JavaType productPageType = typeFactory.constructParametricType(CustomPageImpl.class, ProductSummaryDTO.class);
		JavaType collectionListType = typeFactory.constructCollectionType(List.class, CollectionResponseDTO.class);

		return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(baseConfig())
				.withCacheConfiguration(CacheNames.CATALOG_PRODUCTS,
						baseConfig().serializeValuesWith(serializerFor(objectMapper, productPageType)))
				.withCacheConfiguration(CacheNames.CATALOG_SLUG,
						baseConfig().serializeValuesWith(
								serializerFor(objectMapper, typeFactory.constructType(ProductResponseDTO.class))))
				.withCacheConfiguration(CacheNames.CATALOG_COLLECTIONS,
						baseConfig().serializeValuesWith(serializerFor(objectMapper, collectionListType)))
				.disableCreateOnMissingCache().build();
	}

	@Override
	public CacheErrorHandler errorHandler() {
		return new CacheErrorHandler() {

			@Override
			public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Cache {} unavailable while reading key {}: {}", cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
				log.warn("Cache {} unavailable while writing key {}: {}", cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
				log.error("Failed to evict key {} from cache {} — stale data may be served until the TTL expires: {}",
						key, cache.getName(), exception.getMessage());
			}

			@Override
			public void handleCacheClearError(RuntimeException exception, Cache cache) {
				log.error("Failed to clear cache {} — stale data may be served until the TTL expires: {}",
						cache.getName(), exception.getMessage());
			}
		};
	}

	private RedisCacheConfiguration baseConfig() {
		return RedisCacheConfiguration.defaultCacheConfig().entryTtl(TTL)
				.serializeKeysWith(
						RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.disableCachingNullValues();
	}

	private static RedisSerializationContext.SerializationPair<Object> serializerFor(JsonMapper objectMapper,
			JavaType javaType) {
		RedisSerializer<Object> serializer = new JacksonJsonRedisSerializer<>(objectMapper, javaType);
		return RedisSerializationContext.SerializationPair.fromSerializer(serializer);
	}
}
