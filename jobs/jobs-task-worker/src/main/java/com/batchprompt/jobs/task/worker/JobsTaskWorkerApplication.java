package com.batchprompt.jobs.task.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan
public class JobsTaskWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobsTaskWorkerApplication.class, args);
    }
}