package com.nexusai.analytics.core.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ═══════════════════════════════════════════════════════════════
 * CLICKHOUSE CONFIGURATION
 * 
 * Configuration de la connexion à ClickHouse.
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Configuration
@EnableTransactionManagement
public class ClickHouseConfig {
    
    @Value("${spring.datasource.clickhouse.url}")
    private String url;
    
    @Value("${spring.datasource.clickhouse.username}")
    private String username;
    
    @Value("${spring.datasource.clickhouse.password}")
    private String password;
    
    @Bean(name = "clickHouseDataSource")
    public DataSource clickHouseDataSource() throws SQLException {
        log.info("Initializing ClickHouse DataSource: {}", url);
        
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        
        // Optimisations de performance
        properties.setProperty("socket_timeout", "300000");
        properties.setProperty("connection_timeout", "10000");
        properties.setProperty("max_execution_time", "300");
        properties.setProperty("max_threads", "8");
        
        // Compression
        properties.setProperty("compress", "1");
        properties.setProperty("compression", "lz4");
        
        // Batch insert optimizations
        properties.setProperty("max_insert_block_size", "100000");
        properties.setProperty("insert_deduplicate", "0");
        
        ClickHouseDataSource dataSource = new ClickHouseDataSource(url, properties);
        
        log.info("ClickHouse DataSource initialized successfully");
        return dataSource;
    }
    
    @Bean(name = "clickHouseJdbcTemplate")
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(clickHouseDataSource);
        jdbcTemplate.setFetchSize(1000);
        return jdbcTemplate;
    }
    
    @Bean
    public PlatformTransactionManager clickHouseTransactionManager(
            DataSource clickHouseDataSource) {
        return new DataSourceTransactionManager(clickHouseDataSource);
    }
}

// ═══════════════════════════════════════════════════════════════

package com.nexusai.analytics.core.config;

import com.nexusai.analytics.core.model.EventMessage;
import com.nexusai.analytics.core.model.MetricMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════════
 * KAFKA CONFIGURATION
 * 
 * Configuration Kafka pour les consumers et producers.
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Value("${nexusai.analytics.collection.batch-size:1000}")
    private int batchSize;
    
    // ═══════════════════════════════════════════════════════════════
    // CONSUMER CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    @Bean
    public ConsumerFactory<String, EventMessage> eventConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EventMessage.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Performance optimizations
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        // Reliability
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventMessage> 
            eventKafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, EventMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(eventConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setPollTimeout(3000);
        
        log.info("Event Kafka Listener Container Factory configured");
        return factory;
    }
    
    @Bean
    public ConsumerFactory<String, MetricMessage> metricConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MetricMessage.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MetricMessage> 
            metricKafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, MetricMessage> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(metricConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        log.info("Metric Kafka Listener Container Factory configured");
        return factory;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // PRODUCER CONFIGURATION
    // ═══════════════════════════════════════════════════════════════
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Performance & Reliability
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

// ═══════════════════════════════════════════════════════════════

package com.nexusai.analytics.core.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * ═══════════════════════════════════════════════════════════════
 * REDIS CONFIGURATION
 * 
 * Configuration Redis pour le cache.
 * ═══════════════════════════════════════════════════════════════
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Value("${spring.data.redis.host}")
    private String host;
    
    @Value("${spring.data.redis.port}")
    private int port;
    
    @Value("${spring.data.redis.password:}")
    private String password;
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing Redis connection: {}:{}", host, port);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        
        if (password != null && !password.isEmpty()) {
            config.setPassword(password);
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        
        log.info("Redis connection factory initialized");
        return factory;
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Serializers
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer(objectMapper());
        
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis Cache Manager");
        
        // Configuration par défaut
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper())))
            .disableCachingNullValues();
        
        // Configurations spécifiques par cache
        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration("eventCounts",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("metricStats",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(2)))
            .withCacheConfiguration("dashboardData",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(1)))
            .build();
        
        log.info("Redis Cache Manager configured with {} caches", 
            cacheManager.getCacheNames().size());
        
        return cacheManager;
    }
    
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}

// ═══════════════════════════════════════════════════════════════

package com.nexusai.analytics.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════
 * SWAGGER/OPENAPI CONFIGURATION
 * 
 * Configuration de la documentation API avec Swagger.
 * ═══════════════════════════════════════════════════════════════
 */
@Configuration
public class SwaggerConfig {
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    @Bean
    public OpenAPI analyticsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NexusAI Analytics & Monitoring API")
                .description("API REST pour l'analytics et le monitoring de NexusAI")
                .version("1.0.0")
                .contact(new Contact()
                    .name("NexusAI Team")
                    .email("analytics@nexusai.com")
                    .url("https://nexusai.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://nexusai.com/license")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Environnement de développement"),
                new Server()
                    .url("https://api.nexusai.com")
                    .description("Production")
            ));
    }
}

// ═══════════════════════════════════════════════════════════════

package com.nexusai.analytics.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * ═══════════════════════════════════════════════════════════════
 * SECURITY CONFIGURATION
 * 
 * Configuration de la sécurité (CORS, authentification).
 * ═══════════════════════════════════════════════════════════════
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/v1/analytics/health"
                ).permitAll()
                // Protected endpoints (à configurer avec JWT plus tard)
                .requestMatchers("/api/**").permitAll() // Temporaire
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:8080",
            "https://nexusai.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

// ═══════════════════════════════════════════════════════════════

package com.nexusai.analytics.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ═══════════════════════════════════════════════════════════════
 * ASYNC & SCHEDULING CONFIGURATION
 * 
 * Configuration pour l'exécution asynchrone et les tâches schedulées.
 * ═══════════════════════════════════════════════════════════════
 */
@Configuration
@EnableAsync
@EnableScheduling
@EnableRetry
public class AsyncConfig {
    // Configuration par défaut suffisante
}
