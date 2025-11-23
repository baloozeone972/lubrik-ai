package com.nexusai.conversation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;

/**
 * APPLICATION PRINCIPALE - MODULE CONVERSATION
 * 
 * Point d'entrée de l'application
 * 
 * @author NexusAI Dev Team
 * @version 1.0.0
 */
@SpringBootApplication(scanBasePackages = "com.nexusai.conversation")
@EnableReactiveMongoRepositories(basePackages = "com.nexusai.conversation.persistence")
@EnableKafka
@EnableScheduling
@EnableWebFlux
public class ConversationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ConversationApplication.class, args);
    }
}

// ============================================================================
// CONFIGURATION APPLICATION
// ============================================================================

/**
 * Configuration principale de l'application
 */
@Configuration
public class ApplicationConfig implements WebFluxConfigurer {
    
    /**
     * Configuration CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000", "https://nexusai.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
    
    /**
     * ObjectMapper pour JSON
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}

// ============================================================================
// CONFIGURATION MONGODB
// ============================================================================

/**
 * Configuration MongoDB Reactive
 */
@Configuration
public class MongoConfig {
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${spring.data.mongodb.database}")
    private String databaseName;
    
    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        return new ReactiveMongoTemplate(
            MongoClients.create(mongoUri),
            databaseName
        );
    }
    
    /**
     * Configuration des convertisseurs pour types custom
     */
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(List.of(
            new InstantToDateConverter(),
            new DateToInstantConverter()
        ));
    }
    
    // Convertisseurs Instant <-> Date
    static class InstantToDateConverter implements Converter<Instant, Date> {
        @Override
        public Date convert(Instant source) {
            return Date.from(source);
        }
    }
    
    static class DateToInstantConverter implements Converter<Date, Instant> {
        @Override
        public Instant convert(Date source) {
            return source.toInstant();
        }
    }
}

// ============================================================================
// CONFIGURATION REDIS
// ============================================================================

/**
 * Configuration Redis Reactive
 */
@Configuration
public class RedisConfig {
    
    @Value("${spring.data.redis.host}")
    private String redisHost;
    
    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (!redisPassword.isEmpty()) {
            config.setPassword(RedisPassword.of(redisPassword));
        }
        
        return new LettuceConnectionFactory(config);
    }
    
    @Bean
    public ReactiveRedisTemplate<String, MessageEntity> messageRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        Jackson2JsonRedisSerializer<MessageEntity> serializer = 
            new Jackson2JsonRedisSerializer<>(MessageEntity.class);
        
        RedisSerializationContext<String, MessageEntity> context = 
            RedisSerializationContext.<String, MessageEntity>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(serializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(serializer)
                .build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
    
    @Bean
    public ReactiveRedisTemplate<String, ConversationContext> contextRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        Jackson2JsonRedisSerializer<ConversationContext> serializer = 
            new Jackson2JsonRedisSerializer<>(ConversationContext.class);
        
        RedisSerializationContext<String, ConversationContext> context = 
            RedisSerializationContext.<String, ConversationContext>newSerializationContext()
                .key(StringRedisSerializer.UTF_8)
                .value(serializer)
                .hashKey(StringRedisSerializer.UTF_8)
                .hashValue(serializer)
                .build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
}

// ============================================================================
// CONFIGURATION KAFKA
// ============================================================================

/**
 * Configuration Kafka
 */
@Configuration
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "conversation-service");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.nexusai.*");
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
}

// ============================================================================
// CONFIGURATION OPENAI
// ============================================================================

/**
 * Configuration OpenAI
 */
@Configuration
public class OpenAIConfig {
    
    @Value("${openai.api-key}")
    private String apiKey;
    
    @Value("${openai.timeout-seconds:30}")
    private int timeoutSeconds;
    
    @Bean
    public OpenAiService openAiService() {
        return new OpenAiService(apiKey, Duration.ofSeconds(timeoutSeconds));
    }
}

// ============================================================================
// CONFIGURATION ANTHROPIC
// ============================================================================

/**
 * Configuration Anthropic
 */
@Configuration
public class AnthropicConfig {
    
    @Bean
    public WebClient anthropicWebClient(
            @Value("${anthropic.api-url}") String apiUrl,
            @Value("${anthropic.timeout-seconds:30}") int timeoutSeconds) {
        
        return WebClient.builder()
            .baseUrl(apiUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(timeoutSeconds))
            ))
            .build();
    }
}

// ============================================================================
// SCHEDULED TASKS
// ============================================================================

/**
 * Tâches planifiées
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasks {
    
    private final ConversationRepository conversationRepository;
    
    /**
     * Nettoie les conversations éphémères expirées
     * 
     * Exécuté toutes les heures
     */
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredConversations() {
        log.info("Démarrage nettoyage conversations éphémères");
        
        conversationRepository
            .findByIsEphemeralTrueAndExpiresAtBefore(Instant.now())
            .flatMap(conv -> conversationRepository.delete(conv))
            .doOnComplete(() -> log.info("Nettoyage terminé"))
            .subscribe();
    }
}

// ============================================================================
// HEALTH CHECKS
// ============================================================================

/**
 * Health checks pour monitoring
 */
@Component
public class ConversationHealthIndicator implements ReactiveHealthIndicator {
    
    private final ConversationRepository repository;
    private final ReactiveRedisTemplate<String, ?> redisTemplate;
    
    public ConversationHealthIndicator(
            ConversationRepository repository,
            ReactiveRedisTemplate<String, ?> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Mono<Health> health() {
        return Mono.zip(
            checkMongoDB(),
            checkRedis()
        )
        .map(tuple -> {
            boolean mongoUp = tuple.getT1();
            boolean redisUp = tuple.getT2();
            
            if (mongoUp && redisUp) {
                return Health.up()
                    .withDetail("mongodb", "UP")
                    .withDetail("redis", "UP")
                    .build();
            } else {
                return Health.down()
                    .withDetail("mongodb", mongoUp ? "UP" : "DOWN")
                    .withDetail("redis", redisUp ? "UP" : "DOWN")
                    .build();
            }
        });
    }
    
    private Mono<Boolean> checkMongoDB() {
        return repository.count()
            .map(count -> true)
            .onErrorReturn(false);
    }
    
    private Mono<Boolean> checkRedis() {
        return redisTemplate.hasKey("health-check")
            .map(exists -> true)
            .onErrorReturn(false);
    }
}

// ============================================================================
// MÉTRIQUES PROMETHEUS
// ============================================================================

/**
 * Configuration des métriques custom
 */
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags("application", "conversation-service")
            .commonTags("service", "nexusai");
    }
    
    @Bean
    public Counter messagesSentCounter(MeterRegistry registry) {
        return Counter.builder("conversation.messages.sent")
            .description("Nombre total de messages envoyés")
            .register(registry);
    }
    
    @Bean
    public Timer conversationDuration(MeterRegistry registry) {
        return Timer.builder("conversation.duration")
            .description("Durée des conversations")
            .register(registry);
    }
    
    @Bean
    public Gauge activeConversations(
            MeterRegistry registry,
            ConversationRepository repository) {
        
        return Gauge.builder("conversation.active", repository, repo -> 
            repo.count().block()
        )
        .description("Nombre de conversations actives")
        .register(registry);
    }
}

// ============================================================================
// DOCUMENTATION SWAGGER
// ============================================================================

/**
 * Configuration Swagger/OpenAPI
 */
@Configuration
public class SwaggerConfig {
    
    @Bean
    public GroupedOpenApi conversationApi() {
        return GroupedOpenApi.builder()
            .group("conversation-api")
            .pathsToMatch("/api/v1/conversations/**")
            .build();
    }
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NexusAI Conversation API")
                .version("1.0.0")
                .description("API de gestion des conversations avec les compagnons IA")
                .contact(new Contact()
                    .name("NexusAI Dev Team")
                    .email("dev@nexusai.com")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Développement"),
                new Server()
                    .url("https://api.nexusai.com")
                    .description("Production")
            ));
    }
}

// ============================================================================
// SECURITY CONFIGURATION
// ============================================================================

/**
 * Configuration de sécurité
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .cors().and()
            .csrf().disable()
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .pathMatchers("/ws/**").permitAll()
                .pathMatchers("/api/v1/**").authenticated()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
            )
            .build();
    }
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // Configuration JWT
        // À adapter selon votre provider (Auth0, Keycloak, etc.)
        return ReactiveJwtDecoders.fromIssuerLocation(
            "https://auth.nexusai.com"
        );
    }
}
