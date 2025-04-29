package com.batchprompt.jobs.core.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.config.WorkerConfig;
import com.batchprompt.jobs.core.config.WorkerConfig.WorkerConfiguration;
import com.batchprompt.jobs.core.exception.JobSubmissionException;
import com.batchprompt.jobs.model.dto.JobOutputMessage;
import com.batchprompt.jobs.model.dto.JobTaskMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ModelService modelService;
    private final WorkerConfig workerConfig;
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.queue.job-output.routing-key}")
    private String jobsOutputRoutingKey;
    
    /**
     * Send a job task message to the appropriate queue based on the model name
     * with rate limiting applied according to worker configuration.
     * 
     * @param jobTaskMessage The job task message to send
     * @throws JobSubmissionException If there's an issue with the queue or rate limit
     */
    public void sendJobTask(JobTaskMessage jobTaskMessage) {
        String modelName = jobTaskMessage.getModelName();
        String queueName = modelService.getQueueForModel(modelName);
        
        if (queueName == null || queueName.isEmpty()) {
            log.error("No queue configured for model: {}", modelName);
            throw new JobSubmissionException("No queue configured for model: " + modelName);
        }
        
        // Get worker configuration for this queue
        WorkerConfiguration workerConfig = this.workerConfig.getWorkerConfigForQueue(queueName);
        if (workerConfig == null) {
            log.error("No worker configuration found for queue: {}", queueName);
            throw new JobSubmissionException("No worker configuration for queue: " + queueName);
        }
        
        try {
            // Apply rate limiting based on worker configuration
            int rateLimit = workerConfig.getRateLimit();
            if (rateLimit > 0) {
                WorkerConfig.QueueRateTracker rateTracker = this.workerConfig.getRateTracker(queueName);
                rateTracker.recordRequestAndWaitIfNecessary(rateLimit);
            }
            
            // Send the message to the model-specific queue
            log.info("Sending job task message via {} to queue '{}': {}", exchangeName, queueName, jobTaskMessage.getJobTaskUuid());
            rabbitTemplate.convertAndSend(exchangeName, queueName, jobTaskMessage);
            log.info("Job task message sent via {} to queue '{}': {}", exchangeName, queueName, jobTaskMessage.getJobTaskUuid());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for rate limit on queue: {}", queueName);
            throw new JobSubmissionException("Rate limit wait interrupted for queue: " + queueName);
        }
    }
    
    /**
     * Send a job output message to the output queue
     * 
     * @param outputMessage The output message to send
     */
    public void sendJobOutput(JobOutputMessage outputMessage) {
        log.info("Sending job output message to queue: {}", outputMessage.getJobUuid());
        rabbitTemplate.convertAndSend(exchangeName, jobsOutputRoutingKey, outputMessage);
        log.info("Job output message sent to queue: {}", outputMessage.getJobUuid());
    }
}