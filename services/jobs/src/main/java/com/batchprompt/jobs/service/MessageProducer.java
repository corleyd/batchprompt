package com.batchprompt.jobs.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.batchprompt.jobs.dto.JobTaskMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.routing.jobs.key}")
    private String routingKey;
    
    public void sendJobTask(JobTaskMessage jobTaskMessage) {
        log.info("Sending job task message to queue: {}", jobTaskMessage.getJobTaskUuid());
        rabbitTemplate.convertAndSend(exchangeName, routingKey, jobTaskMessage);
        log.info("Job task message sent to queue: {}", jobTaskMessage.getJobTaskUuid());
    }
}