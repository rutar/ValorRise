package com.valorrise.bot.api.client;

import com.valorrise.bot.configuration.ApiConfiguration;
import feign.Client;
import feign.Request;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(ApiConfiguration.class)
public class FeignConfig {
    private final ApiConfiguration apiConfig;

    public FeignConfig(ApiConfiguration apiConfig) {
        this.apiConfig = apiConfig;
    }

    @Bean
    public JacksonDecoder feignDecoder() {
        return new JacksonDecoder();
    }

    @Bean
    public JacksonEncoder feignEncoder() {
        return new JacksonEncoder();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new GameApiErrorDecoder();
    }

    @Bean
    public Client feignClient() {
        return new Client.Default(null, null) {
            @Override
            public Response execute(Request request, Request.Options options) throws IOException {
                // Use non-deprecated Options constructor
                Request.Options updatedOptions = new Request.Options(
                        apiConfig.getApi().getTimeout(), TimeUnit.MILLISECONDS, // Connect timeout
                        apiConfig.getApi().getTimeout(), TimeUnit.MILLISECONDS, // Read timeout
                        true // Follow redirects
                );
                return super.execute(request, updatedOptions);
            }
        };
    }
}