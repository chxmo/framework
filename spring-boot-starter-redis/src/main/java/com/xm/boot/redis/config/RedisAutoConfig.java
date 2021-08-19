package com.xm.boot.redis.config;

import com.xm.boot.redis.annotation.LockAnnotationAdvisor;
import com.xm.boot.redis.annotation.LockInterceptor;
import com.xm.boot.redis.annotation.strategy.KeyGeneratorLoader;
import com.xm.boot.redis.annotation.strategy.SpelKeyGenerator;
import com.xm.boot.redis.init.RedisApplicationInit;
import com.xm.boot.redis.lock.RedisDistributedLock;
import com.xm.boot.redis.serializer.RedisKeySerializer;
import com.xm.boot.redis.serializer.RedisValueSerializer;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.redis.RedisHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.xm.boot.redis.common.Constant.*;

/**
 * @author xiehejun(玄墨)
 * @date 2021/8/13 5:14 下午
 */

@Configuration
@EnableConfigurationProperties(RedisConfig.class)
@ConditionalOnProperty(prefix = "xm.redis.conf", value = "enabled", havingValue = "true", matchIfMissing = false)
public class RedisAutoConfig {

    @Autowired
    public RedisConfig redisConfig;

    @Primary
    @Bean(destroyMethod = "shutdown")
    public DefaultClientResources lettuceClientResources() {
        return DefaultClientResources.create();
    }

    @Primary
    @Bean(name = "redisConnectionFactory")
    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) throws UnknownHostException {
        LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(
                clientResources, this.redisConfig.getPool());
        LettuceConnectionFactory factory = createLettuceConnectionFactory(clientConfig);
        if (!redisConfig.getShareNativeConnection()) {
            factory.setShareNativeConnection(false);//配置使用连接池
        }
        return factory;
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(
            LettuceClientConfiguration clientConfiguration) {
        if (getSentinelConfig() != null) {
            return new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
        }
        if (getClusterConfiguration() != null) {
            return new LettuceConnectionFactory(getClusterConfiguration(),
                    clientConfiguration);
        }
        return new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
    }

    /**
     * 哨兵模式配置
     *
     * @return
     */
    protected final RedisSentinelConfiguration getSentinelConfig() {
        RedisConfig.Sentinel sentinelProperties = this.redisConfig.getSentinel();
        if (sentinelProperties != null) {
            RedisSentinelConfiguration config = new RedisSentinelConfiguration();
            config.master(sentinelProperties.getMaster());
            config.setSentinels(convertString2ListForNodes(sentinelProperties.getNodes()));
            if (this.redisConfig.getPassword() != null) {
                config.setPassword(RedisPassword.of(this.redisConfig.getPassword()));
            }
            config.setDatabase(this.redisConfig.getDatabase());
            return config;
        }
        return null;
    }

    private List<RedisNode> convertString2ListForNodes(String nodes) {
        if (StringUtils.isEmpty(nodes)) {
            throw new IllegalStateException("Invalid redis, nodes is not empty");
        }
        String[] nodeStrs = nodes.split(COMMA);
        long port = Arrays.stream(nodeStrs).filter(s -> s.contains(COLON) && s.split(COLON)[1].matches(PORT_PATTERN)).count();
        if (port != nodeStrs.length) {
            throw new IllegalStateException("Invalid redis, nodes address is illegal for port");

        }
        return Arrays.stream(nodeStrs).map(s -> new RedisNode(s.split(COLON)[0], Integer.parseInt(s.split(COLON)[1]))).collect(Collectors.toList());
    }

    /**
     * 集群模式
     *
     * @return
     */
    protected final RedisClusterConfiguration getClusterConfiguration() {
        if (this.redisConfig.getCluster() == null) {
            return null;
        } else {
            RedisConfig.Cluster clusterProperties = this.redisConfig.getCluster();
            RedisClusterConfiguration config = new RedisClusterConfiguration();
            config.setClusterNodes(convertString2ListForNodes(clusterProperties.getNodes()));
            if (clusterProperties.getMaxRedirects() != null) {
                config.setMaxRedirects(clusterProperties.getMaxRedirects().intValue());
            }
            if (this.redisConfig.getPassword() != null) {
                config.setPassword(RedisPassword.of(this.redisConfig.getPassword()));
            }
            return config;
        }
    }

    protected final RedisStandaloneConfiguration getStandaloneConfig() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(this.redisConfig.getHost());
        config.setPort(this.redisConfig.getPort());
        config.setPassword(RedisPassword.of(this.redisConfig.getPassword()));
        config.setDatabase(this.redisConfig.getDatabase());
        return config;
    }

    @Primary
    @Bean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new RedisKeySerializer());
        redisTemplate.setValueSerializer(new RedisValueSerializer());
        return redisTemplate;
    }

    @Bean(name = "redisDistributedLock")
    public RedisDistributedLock redisDistributedLock(RedisTemplate redisTemplate) {
        return new RedisDistributedLock(redisTemplate);
    }

    /**
     * redis健康检查
     *
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(name = "redisHealthIndicator")
    public HealthIndicator redisHealthIndicator(RedisConnectionFactory redisConnectionFactory) {
        return new RedisHealthIndicator(redisConnectionFactory);
    }

    /**
     * 初始化redis init
     *
     * @return
     */
    @Order(1)
    @Bean
    @ConditionalOnMissingBean(name = "redisApplicationInit")
    public RedisApplicationInit redisApplicationInit() {
        return new RedisApplicationInit();
    }

    /*******************************************分布式锁初始化**************************************************/
    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(KeyGeneratorLoader keyGeneratorLoader) {
        return new LockInterceptor(keyGeneratorLoader);
    }

    /**
     * 简单key名自动生成
     *
     * @return
     */
    @Bean("simpleKeyGenerator")
    public SimpleKeyGenerator simpleKeyGenerator() {
        return new SimpleKeyGenerator();
    }

    /**
     * springel解析器动态生成key名
     *
     * @return
     */
    @Bean("spelKeyGenerator")
    public SpelKeyGenerator spelKeyGenerator() {
        return new SpelKeyGenerator();
    }

    /**
     * 策略模式生成key名
     *
     * @return
     */
    @Bean("keyGeneratorLoader")
    public KeyGeneratorLoader keyGeneratorLoader() {
        return new KeyGeneratorLoader();
    }

    private LettuceClientConfiguration getLettuceClientConfiguration(
            ClientResources clientResources, RedisConfig.Pool pool) {
        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder = createBuilder(pool);
        applyProperties(builder);
        builder.clientResources(clientResources);
        return builder.build();
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder createBuilder(RedisConfig.Pool pool) {
        if (pool == null) {
            return LettuceClientConfiguration.builder();
        }
        return new PoolBuilderFactory().createBuilder(pool);
    }

    private LettuceClientConfiguration.LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfiguration.LettuceClientConfigurationBuilder builder) {
        if (this.redisConfig.isSsl()) {
            builder.useSsl();
        }
        if (this.redisConfig.getTimeout() != null) {
            builder.commandTimeout(this.redisConfig.getTimeout());
        }
        if (this.redisConfig.getShutdownTimeout() != null) {
            builder.shutdownTimeout(this.redisConfig.getShutdownTimeout());
        }
        return builder;
    }

    /**
     * Inner class to allow optional commons-pool2 dependency.
     */
    private static class PoolBuilderFactory {

        public LettuceClientConfiguration.LettuceClientConfigurationBuilder createBuilder(RedisConfig.Pool properties) {
            return LettucePoolingClientConfiguration.builder()
                    .poolConfig(getPoolConfig(properties));
        }

        private GenericObjectPoolConfig getPoolConfig(RedisConfig.Pool properties) {
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxTotal(properties.getMaxActive());
            config.setMaxIdle(properties.getMaxIdle());
            config.setMinIdle(properties.getMinIdle());
            if (properties.getMaxWait() != null) {
                config.setMaxWaitMillis(properties.getMaxWait().toMillis());
            }
            return config;
        }

    }
}
