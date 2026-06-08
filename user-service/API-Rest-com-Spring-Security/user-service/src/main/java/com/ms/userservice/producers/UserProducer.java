package com.ms.userservice.producers;

import com.ms.userservice.dtos.EmailDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${broker.queue.email.name}")
    private String emailQueueName;

    public void publicarEmailOtp(EmailDto emailDto) {
        rabbitTemplate.convertAndSend(emailQueueName, emailDto);
    }
}
