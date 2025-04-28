package com.batchprompt.jobs.core.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.jobs.name}")
    private String jobsQueueName;

    @Value("${rabbitmq.routing.jobs.key}")
    private String jobsRoutingKey;
    
    @Value("${rabbitmq.queue.output.name}")
    private String outputQueueName;
    
    @Value("${rabbitmq.routing.output.key}")
    private String outputRoutingKey;

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Queue jobsQueue() {
        return new Queue(jobsQueueName, true);
    }
    
    @Bean
    public Queue outputQueue() {
        return new Queue(outputQueueName, true);
    }

    @Bean
    public Binding jobsBinding(Queue jobsQueue, DirectExchange exchange) {
        return BindingBuilder.bind(jobsQueue).to(exchange).with(jobsRoutingKey);
    }
    
    @Bean
    public Binding outputBinding(Queue outputQueue, DirectExchange exchange) {
        return BindingBuilder.bind(outputQueue).to(exchange).with(outputRoutingKey);
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