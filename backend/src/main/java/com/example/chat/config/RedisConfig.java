package com.example.chat.config;

import io.lettuce.core.RedisURI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    public static final String PRESENCE_TOPIC = "chat:presence";

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${REDIS_PORT_NUM:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    // Checks REDIS_URL first, then spring.data.redis.url, and also REDIS_PORT in case the user pasted the URL into REDIS_PORT
    @Value("${REDIS_URL:${spring.data.redis.url:${REDIS_PORT:}}}")
    private String redisUrl;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        String cleanUrl = redisUrl != null ? redisUrl.trim() : "";

        // Handle case where user pastes "redis-cli -u redis://..." or direct redis URL
        if (cleanUrl.contains("redis://") || cleanUrl.contains("rediss://")) {
            int idx = cleanUrl.indexOf("redis://");
            if (idx == -1) {
                idx = cleanUrl.indexOf("rediss://");
            }
            cleanUrl = cleanUrl.substring(idx).trim();

            // Strip quotes if present
            if ((cleanUrl.startsWith("\"") && cleanUrl.endsWith("\"")) ||
                (cleanUrl.startsWith("'") && cleanUrl.endsWith("'"))) {
                cleanUrl = cleanUrl.substring(1, cleanUrl.length() - 1).trim();
            }

            try {
                log.info("Configuring Redis using REDIS_URL: {}", cleanUrl.replaceAll(":[^@]+@", ":****@"));
                RedisURI redisUri = RedisURI.create(cleanUrl);

                RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
                standaloneConfig.setHostName(redisUri.getHost());
                standaloneConfig.setPort(redisUri.getPort());

                if (redisUri.getPassword() != null && redisUri.getPassword().length > 0) {
                    standaloneConfig.setPassword(RedisPassword.of(new String(redisUri.getPassword())));
                }
                if (StringUtils.hasText(redisUri.getUsername())) {
                    standaloneConfig.setUsername(redisUri.getUsername());
                }
                if (redisUri.getDatabase() != 0) {
                    standaloneConfig.setDatabase(redisUri.getDatabase());
                }

                LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder();
                if (redisUri.isSsl()) {
                    clientConfig.useSsl();
                }

                return new LettuceConnectionFactory(standaloneConfig, clientConfig.build());
            } catch (Exception e) {
                log.warn("Failed to parse REDIS_URL: '{}'. Falling back to host/port configuration. Error: {}", cleanUrl, e.getMessage());
            }
        }

        // Fallback to standard host:port configuration
        log.info("Configuring Redis using host: {}:{}", host, port);
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(host, port);
        if (StringUtils.hasText(password)) {
            standaloneConfig.setPassword(RedisPassword.of(password));
        }
        return new LettuceConnectionFactory(standaloneConfig);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public ChannelTopic presenceTopic() {
        return new ChannelTopic(PRESENCE_TOPIC);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer() {
            @Override
            public boolean isAutoStartup() {
                // Do not crash startup if Redis is temporarily offline; lifecycle is handled gracefully
                return false;
            }
        };
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
