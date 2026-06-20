package com.Soham.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Custom Lettuce connection factory for Azure Managed Redis Enterprise.
 * Azure uses SSL on port 10000 with a certificate that isn't in the default
 * JVM truststore, so we disable peer verification.
 * (In production you'd import the cert; for a portfolio project this is fine.)
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder builder =
            LettuceClientConfiguration.builder();

        if (sslEnabled) {
            // Disable peer verification — Azure Managed Redis cert isn't in the default JVM truststore
            builder.useSsl().disablePeerVerification();
        }

        return new LettuceConnectionFactory(serverConfig, builder.build());
    }
}
