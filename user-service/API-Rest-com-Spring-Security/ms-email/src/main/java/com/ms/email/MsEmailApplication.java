package com.ms.email;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class MsEmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsEmailApplication.class, args);
    }
}
