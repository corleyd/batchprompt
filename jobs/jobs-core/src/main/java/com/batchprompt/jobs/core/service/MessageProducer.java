package com.batchprompt.jobs.core.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.queue.job-output.routing-key}")
    private String jobsOutputRoutingKey;
    
    /**
     * Send a job task message to the appropriate queue based on the model id.
     * 
     * @param jobTaskMessage The job task message to send
     * @throws JobSubmissionException If there's an issue with the queue
     */
    public void sendJobTask(JobTaskMessage jobTaskMessage) {
        String modelId = jobTaskMessage.getModelId();
        String queueName = modelService.getQueueForModel(modelId);
        
        if (queueName == null || queueName.isEmpty()) {
            log.error("No queue configured for model: {}", modelId);
            throw new JobSubmissionException("No queue configured for model: " + modelId);
        }

        try {
            // Send the message to the model-specific queue
            log.info("Sending job task message via {} to queue '{}': {}", exchangeName, queueName, jobTaskMessage.getJobTaskUuid());
            rabbitTemplate.convertAndSend(exchangeName, queueName, jobTaskMessage);
            log.info("Job task message sent via {} to queue '{}': {}", exchangeName, queueName, jobTaskMessage.getJobTaskUuid());
        } catch (Exception e) {
            log.error("Error sending job task message to queue: {}", queueName, e);
            throw new JobSubmissionException("Error sending job task message to queue: " + queueName);
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