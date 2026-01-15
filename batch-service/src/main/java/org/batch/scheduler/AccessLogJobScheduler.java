package org.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for the access log batch job
 * Runs every 2 minutes by default (configurable via cron expression)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job accessLogProcessingJob;

    /**
     * Scheduled job execution
     * Default: Every 2 minutes (cron: 0 *&#47;2 * * * ?)
     * Configurable via batch.schedule.cron property
     */
    @Scheduled(cron = "${batch.schedule.cron:0 */2 * * * ?}")
    public void runScheduledJob() {
        log.info("Scheduled batch job triggered at {}", java.time.LocalDateTime.now());
        
        try {
            // Add unique parameters to make each run unique (required by Spring Batch)
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("triggeredBy", "scheduler")
                    .toJobParameters();

            jobLauncher.run(accessLogProcessingJob, params);
            
            log.info("Scheduled batch job completed successfully");
        } catch (Exception e) {
            log.error("Scheduled batch job failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manual trigger (can be called from REST controller)
     */
    public void runManualJob() {
        log.info("Manual batch job triggered");
        
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("triggeredBy", "manual")
                    .toJobParameters();

            jobLauncher.run(accessLogProcessingJob, params);
            
            log.info("Manual batch job completed successfully");
        } catch (Exception e) {
            log.error("Manual batch job failed: {}", e.getMessage(), e);
            throw new RuntimeException("Batch job execution failed", e);
        }
    }
}
