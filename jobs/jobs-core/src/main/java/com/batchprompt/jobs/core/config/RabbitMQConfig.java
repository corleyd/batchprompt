package com.batchprompt.jobs.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.job-output.name}")
    private String jobsOutputQueueName;
    
    @Value("${rabbitmq.queue.job-output.routing-key}")
    private String jobsOutputRoutingKey;
    
    @Value("${rabbitmq.queue.job-validation.name}")
    private String jobsValidationtQueueName;
    
    @Value("${rabbitmq.queue.job-validation.routing-key}")
    private String jobsValidationRoutingKey;
    

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public RabbitAdmin RabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }    

    @Bean
    public Queue jobsOutputQueueName() {
        return new Queue(jobsOutputQueueName, true);
    }

    @Bean
    public Binding jobsOutputBinding(DirectExchange exchange, Queue jobsOutputQueueName) {
        return BindingBuilder.bind(jobsOutputQueueName).to(exchange).with(jobsOutputRoutingKey);
    }

    @Bean
    public Queue jobsValidationQueueName() {
        return new Queue(jobsValidationtQueueName, true);
    }

    @Bean Binding jobsValidationBinding(DirectExchange exchange, Queue jobsValidationQueueName) {
        return BindingBuilder.bind(jobsValidationQueueName).to(exchange).with(jobsValidationRoutingKey);
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }


    
}