package com.batchprompt.jobs.core;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Component;

import com.batchprompt.jobs.core.config.WorkerConfig;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class QueueHelper {

    private final RabbitAdmin rabbitAdmin;
    private final DirectExchange exchange;
    private final WorkerConfig workerConfig;


    public QueueHelper(
        RabbitAdmin rabbitAdmin, 
        DirectExchange exchange,
        WorkerConfig workerConfig) 
    {
        this.rabbitAdmin = rabbitAdmin;
        this.exchange = exchange;
        this.workerConfig = workerConfig;
    }

    public Queue createAndBindQueue(String queueName) {
   
        var queue = new Queue(queueName, true);
        
        // Check if queue already exists before creating it
        if (rabbitAdmin.getQueueInfo(queueName) == null) {
            // Declare queue first, then create the binding
            rabbitAdmin.declareQueue(queue);
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(queueName);
            rabbitAdmin.declareBinding(binding);
        }
        return queue;
    }

    @PostConstruct
    public void initializeQueues() {
        if (workerConfig.getConfigurations() == null) {
            log.warn("No worker configurations found");
            return;
        }
        
        // Create a queue for each model from configuration
        for (var worker : workerConfig.getConfigurations()) {
            log.info(exchange.getName() + " - Creating queue for worker: " + worker.getQueue());
            createAndBindQueue(worker.getQueue());
        }
    }    
    
}
