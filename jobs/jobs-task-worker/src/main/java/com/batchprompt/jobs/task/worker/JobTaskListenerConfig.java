package com.batchprompt.jobs.task.worker;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;

import com.batchprompt.jobs.core.config.WorkerConfig;
import com.batchprompt.jobs.core.config.WorkerConfig.WorkerConfiguration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for dynamic RabbitMQ listeners based on worker configuration.
 * This class creates message listener containers dynamically for each queue
 * defined in the worker configuration, respecting the concurrency settings.
 */
@Configuration
@EnableRabbit
@Slf4j
public class JobTaskListenerConfig implements RabbitListenerConfigurer {

    @Autowired
    private WorkerConfig workerConfig;
    
    @Autowired
    private ConnectionFactory connectionFactory;
    
    @Autowired
    private JobTaskWorker jobTaskWorker;

    @Autowired
    private MessageConverter jsonMessageConverter;
    
    // Map to store all created listener containers
    private final Map<String, SimpleMessageListenerContainer> listenerContainers = new HashMap<>();
    
    @PostConstruct
    public void initializeListeners() {
        if (workerConfig.getConfigurations() == null) {
            log.warn("No worker configurations found");
            return;
        }
        
        log.info("Initializing {} worker configurations", workerConfig.getConfigurations().size());
        
        for (WorkerConfiguration workerConfig : workerConfig.getConfigurations()) {
            createListenerForQueue(workerConfig);
        }
    }
    
    /**
     * Create a message listener container for a specific queue
     * 
     * @param workerConfig The worker configuration for the queue
     */
    private void createListenerForQueue(WorkerConfiguration workerConfig) {
        String queueName = workerConfig.getQueue();
        int concurrentRequests = workerConfig.getConcurrentRequests();
        
        if (queueName == null || queueName.isEmpty()) {
            log.warn("Queue name is empty for worker configuration");
            return;
        }
        
        if (concurrentRequests <= 0) {
            log.warn("Invalid concurrent requests for queue {}: {}", queueName, concurrentRequests);
            concurrentRequests = 1; // Default to 1
        }
        
        log.info("Creating listener for queue {} with concurrency {}", queueName, concurrentRequests);

        /*
         * NOTE: the queue will already exist because it is created in the RabbitMQConfig class
         * and bound to the exchange. 
         */

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(concurrentRequests);
        container.setMaxConcurrentConsumers(concurrentRequests);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        
        // Set message listener to process job task messages
        container.setMessageListener(message -> {
            try {
                Object convertedMessage = jsonMessageConverter.fromMessage(message);
                if (convertedMessage instanceof com.batchprompt.jobs.model.dto.JobTaskMessage) {
                    jobTaskWorker.processJobTask((com.batchprompt.jobs.model.dto.JobTaskMessage) convertedMessage);
                } else {
                    log.error("Received message of unexpected type: {}", convertedMessage.getClass().getName());
                }
            } catch (Exception e) {
                log.error("Error processing message from queue {}: {}", queueName, e.getMessage(), e);
            }
        });
        
        // Start the container
        container.start();
        
        // Store the container for potential later reference
        listenerContainers.put(queueName, container);
        
        log.info("Listener for queue {} started with concurrency {}", queueName, concurrentRequests);
    }
    
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        return factory;
    }
    
    @Bean
    public MessageHandlerMethodFactory messageHandlerMethodFactory() {
        DefaultMessageHandlerMethodFactory factory = new DefaultMessageHandlerMethodFactory();
        return factory;
    }
    
    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar registrar) {
        registrar.setMessageHandlerMethodFactory(messageHandlerMethodFactory());
    }
}