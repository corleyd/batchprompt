package com.batchprompt.jobs.core.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.queue.job-output.name}")
    private String jobsOutputQueueName;
    
    @Value("${rabbitmq.queue.job-output.routing-key}")
    private String jobsOutputRoutingKey;
    
    @Autowired
    private ModelConfig modelConfig;

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
    public List<Queue> modelQueues(DirectExchange exchange, RabbitAdmin rabbitAdmin) {
        List<Queue> queues = new ArrayList<>();
        
        // Create a queue for each model from configuration
        if (modelConfig.getSupported() != null) {
            for (ModelConfig.ModelDefinition modelDef : modelConfig.getSupported()) {
                if (modelDef.getQueue() != null && !modelDef.getQueue().isEmpty()) {
                    var queueName = modelDef.getQueue();
                    var queue = new Queue(queueName, true);
                    
                    // Always add the queue to the returned list
                    queues.add(queue);
                    
                    // Check if queue already exists before creating it
                    if (rabbitAdmin.getQueueInfo(queueName) == null) {
                        // Declare queue first, then create the binding
                        rabbitAdmin.declareQueue(queue);
                        Binding binding = BindingBuilder.bind(queue).to(exchange).with(queueName);
                        rabbitAdmin.declareBinding(binding);
                    }
                }
            }
        }
        return queues;
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