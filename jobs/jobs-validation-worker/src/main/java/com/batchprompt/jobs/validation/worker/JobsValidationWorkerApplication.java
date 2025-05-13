package com.batchprompt.jobs.validation.worker;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(scanBasePackages = {"com.batchprompt.jobs", "com.batchprompt.common"})
@EnableRabbit
@EnableTransactionManagement
public class JobsValidationWorkerApplication {

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;
    
    @Value("${rabbitmq.queue.job-validation.name}")
    private String jobValidationQueueName;
    
    @Value("${rabbitmq.queue.job-validation.routing-key}")
    private String jobValidationRoutingKey;
    
    public static void main(String[] args) {
        SpringApplication.run(JobsValidationWorkerApplication.class, args);
    }

}
