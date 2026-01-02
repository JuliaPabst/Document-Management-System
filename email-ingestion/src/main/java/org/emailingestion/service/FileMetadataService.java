package org.emailingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.emailingestion.dto.FileMessageDto;
import org.emailingestion.exception.DuplicateFileException;
import org.emailingestion.model.FileMetadata;
import org.emailingestion.repository.FileMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileMetadataService {

    private final FileMetadataRepository fileMetadataRepository;
    private final MessageProducerService messageProducerService;

    public FileMetadata createFileMetadata(FileMetadata fileMetadata) {
        log.info("Creating file metadata for filename: {} by author: {}",
                fileMetadata.getFilename(), fileMetadata.getAuthor());

        // Check for duplicate file with same name and author
        if (fileMetadataRepository.existsByFilenameAndAuthor(
                fileMetadata.getFilename(), fileMetadata.getAuthor())) {
            log.warn("Duplicate file detected: {} by author: {}",
                    fileMetadata.getFilename(), fileMetadata.getAuthor());
            throw new DuplicateFileException(
                    String.format("File with name '%s' already exists for author '%s'",
                            fileMetadata.getFilename(), fileMetadata.getAuthor()));
        }

        FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
        log.info("File metadata created with ID: {}", savedMetadata.getId());

        return savedMetadata;
    }

    public FileMetadata createFileMetadataWithWorkerNotification(FileMetadata fileMetadata) {
        FileMetadata savedMetadata = createFileMetadata(fileMetadata);

        // Send to OCR Queue for processing (OCR will then send to GenAI)
        log.info("Sending file metadata with id {} to OCR queue", savedMetadata.getId());
        FileMessageDto fileMessage = new FileMessageDto(
                savedMetadata.getId(),
                savedMetadata.getFilename(),
                savedMetadata.getAuthor(),
                savedMetadata.getFileType(),
                savedMetadata.getSize(),
                savedMetadata.getUploadTime(),
                savedMetadata.getObjectKey()
        );
        messageProducerService.sendToOcrQueue(fileMessage);

        return savedMetadata;
    }
}
