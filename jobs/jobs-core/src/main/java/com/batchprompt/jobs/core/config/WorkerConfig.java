package com.batchprompt.jobs.core.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "workers")
public class WorkerConfig {
    
    private List<WorkerConfiguration> configurations;
    
    // In-memory map to track request rates per queue
    private final Map<String, QueueRateTracker> queueRateTrackers = new ConcurrentHashMap<>();
    
    public List<WorkerConfiguration> getConfigurations() {
        return configurations;
    }
    
    public void setConfigurations(List<WorkerConfiguration> configurations) {
        this.configurations = configurations;
    }
    
    /**
     * Get the worker configuration for a specific queue
     * 
     * @param queueName The name of the queue
     * @return The worker configuration, or null if not found
     */
    public WorkerConfiguration getWorkerConfigForQueue(String queueName) {
        if (configurations == null) {
            return null;
        }
        
        return configurations.stream()
                .filter(config -> config.getQueue().equals(queueName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get or create a rate tracker for a specific queue
     * 
     * @param queueName The name of the queue
     * @return The queue rate tracker
     */
    public QueueRateTracker getRateTracker(String queueName) {
        return queueRateTrackers.computeIfAbsent(queueName, key -> new QueueRateTracker());
    }
    
    public static class WorkerConfiguration {
        private String queue;
        private int concurrentRequests;
        private int rateLimit;
        
        public String getQueue() {
            return queue;
        }
        
        public void setQueue(String queue) {
            this.queue = queue;
        }
        
        public int getConcurrentRequests() {
            return concurrentRequests;
        }
        
        public void setConcurrentRequests(int concurrentRequests) {
            this.concurrentRequests = concurrentRequests;
        }
        
        public int getRateLimit() {
            return rateLimit;
        }
        
        public void setRateLimit(int rateLimit) {
            this.rateLimit = rateLimit;
        }
    }
    
    /**
     * Helper class to track request rates for a queue
     */
    public static class QueueRateTracker {
        private final ConcurrentHashMap<Long, Integer> requestsPerMinute = new ConcurrentHashMap<>();
        private final Object lockObject = new Object();
        
        /**
         * Check if adding a request would exceed the rate limit
         * 
         * @param rateLimit The maximum number of requests per minute
         * @return true if the rate limit would be exceeded, false otherwise
         */
        public boolean wouldExceedRateLimit(int rateLimit) {
            long currentMinute = System.currentTimeMillis() / (60 * 1000);
            int currentCount = requestsPerMinute.getOrDefault(currentMinute, 0);
            return currentCount >= rateLimit;
        }
        
        /**
         * Record a request and wait if necessary to avoid exceeding the rate limit
         * 
         * @param rateLimit The maximum number of requests per minute
         * @throws InterruptedException If the waiting thread is interrupted
         */
        public void recordRequestAndWaitIfNecessary(int rateLimit) throws InterruptedException {
            synchronized (lockObject) {
                AtomicLong currentMinute = new AtomicLong(System.currentTimeMillis() / (60 * 1000));
                
                // Clean up old entries
                requestsPerMinute.keySet().removeIf(minute -> minute < currentMinute.get());
                
                // Get the current count
                int currentCount = requestsPerMinute.getOrDefault(currentMinute, 0);
                
                // Wait if necessary
                while (currentCount >= rateLimit) {
                    // Calculate time to wait until the next minute
                    long nextMinuteMs = (currentMinute.get() + 1) * 60 * 1000;
                    long waitMs = nextMinuteMs - System.currentTimeMillis();
                    
                    if (waitMs > 0) {
                        lockObject.wait(waitMs);
                    }
                    
                    // Recalculate after waiting
                    currentMinute.set(System.currentTimeMillis() / (60 * 1000));
                    requestsPerMinute.keySet().removeIf(minute -> minute < currentMinute.get());
                    currentCount = requestsPerMinute.getOrDefault(currentMinute, 0);
                }
                
                // Record the request
                requestsPerMinute.put(currentMinute.get(), currentCount + 1);
            }
        }
    }
}