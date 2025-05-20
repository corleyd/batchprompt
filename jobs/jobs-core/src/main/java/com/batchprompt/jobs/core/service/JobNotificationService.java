package com.batchprompt.jobs.core.service;

import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.mapper.JobMapper;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.notifications.client.NotificationSender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for sending job-related notifications.
 * This class encapsulates the notification logic specific to the job service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobNotificationService {
    private final NotificationSender notificationSender;
    private final JobMapper jobMapper = new JobMapper();

    /**
     * Sends a notification for a job status change.
     *
     * @param job the job with the new status
     */
    public void sendJobUpdateNotification(Job job) {
        try {
            log.debug("Sending job update notification for job: {}, status: {}", job.getJobUuid(), job.getStatus());
            
            /*
             * Send notifications to:
             * 1. The general jobs topic - for job lists/dashboards
             * 2. The specific job topic - for job detail views watching a single job
             */
            notificationSender.send("jobs", jobMapper.toDto(job), job.getUserId());
            notificationSender.send("jobs/" + job.getJobUuid().toString(), jobMapper.toDto(job), job.getUserId());
            
            // Log the destination paths for debugging
            log.debug("Sent job notifications to 'jobs' and 'jobs/{}'", job.getJobUuid().toString());
        } catch (Exception e) {
            log.error("Failed to send job status notification for job {}: {}", job.getJobUuid(), e.getMessage(), e);
        }
    }

}
