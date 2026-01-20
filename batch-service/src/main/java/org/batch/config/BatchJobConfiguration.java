package org.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batch.batch.AccessLogProcessor;
import org.batch.batch.AccessLogWriter;
import org.batch.batch.AccessLogXmlReader;
import org.batch.batch.FileArchivingListener;
import org.batch.dto.DocumentAccessRecord;
import org.batch.model.DocumentAccessStatistics;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch job configuration for processing access log XML files
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchJobConfiguration {

    private final AccessLogXmlReader reader;
    private final AccessLogProcessor processor;
    private final AccessLogWriter writer;
    private final FileArchivingListener fileArchivingListener;

    @Value("${batch.chunk.size:100}")
    private int chunkSize;

    @Bean
    public Job accessLogProcessingJob(JobRepository jobRepository, Step accessLogStep) {
        return new JobBuilder("accessLogProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(fileArchivingListener)
                .start(accessLogStep)
                .build();
    }

    @Bean
    public Step accessLogStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("accessLogStep", jobRepository)
                .<DocumentAccessRecord, DocumentAccessStatistics>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(100)
                .skip(Exception.class)
                .build();
    }
}
