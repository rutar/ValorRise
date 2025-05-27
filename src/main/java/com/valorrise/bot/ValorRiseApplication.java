package com.valorrise.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.valorrise.bot.api.client")
public class ValorRiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValorRiseApplication.class, args);
    }
}