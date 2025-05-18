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
     * @param previousStatus the previous status
     */
    public void sendJobUpdateNotification(Job job) {
        try {
            notificationSender.send("job-updated", jobMapper.toDto(job), job.getUserId());
        } catch (Exception e) {
            log.error("Failed to send job status notification", e);
        }
    }

}
