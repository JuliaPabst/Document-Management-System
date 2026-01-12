package org.batch.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Listener to archive successfully processed XML files
 */
@Slf4j
@Component
public class FileArchivingListener implements JobExecutionListener {

    @Value("${batch.input.folder}")
    private String inputFolder;

    @Value("${batch.archive.folder}")
    private String archiveFolder;

    @Value("${batch.file.pattern}")
    private String filePattern;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Starting batch job: {}", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            log.warn("Job completed with status: {}. Skipping file archiving.", jobExecution.getStatus());
            return;
        }

        log.info("Job completed successfully. Archiving processed files...");
        archiveProcessedFiles();
    }

    private void archiveProcessedFiles() {
        try {
            Path inputPath = Paths.get(inputFolder);
            Path archivePath = Paths.get(archiveFolder);

            // Create archive folder if it doesn't exist
            if (!Files.exists(archivePath)) {
                Files.createDirectories(archivePath);
                log.info("Created archive folder: {}", archiveFolder);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, filePattern)) {
                int archivedCount = 0;
                for (Path filePath : stream) {
                    if (Files.isRegularFile(filePath)) {
                        String fileName = filePath.getFileName().toString();
                        String archivedFileName = fileName.replace(".xml", "_" + timestamp + ".xml");
                        Path targetPath = archivePath.resolve(archivedFileName);

                        try {
                            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            archivedCount++;
                            log.info("Archived file: {} -> {}", fileName, archivedFileName);
                        } catch (IOException e) {
                            log.error("Failed to archive file {}: {}", fileName, e.getMessage());
                        }
                    }
                }
                log.info("Archived {} files to {}", archivedCount, archiveFolder);
            }
        } catch (IOException e) {
            log.error("Error during file archiving: {}", e.getMessage(), e);
        }
    }
}
