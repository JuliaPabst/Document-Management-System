package org.rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.FileMessageDto;
import org.rest.exception.DuplicateFileException;
import org.rest.exception.FileMetadataNotFoundException;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    
    @Transactional(readOnly = true)
    public FileMetadata getFileMetadataById(Long id) {
        log.info("Retrieving file metadata with ID: {}", id);
        
        return fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException("File metadata not found with ID: " + id));
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadata> getAllFileMetadata() {
        log.info("Retrieving all file metadata");
        
        return fileMetadataRepository.findByOrderByUploadTimeDesc();
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadata> searchFileMetadata(String keyword) {
        log.info("Searching file metadata with keyword: {}", keyword);
        
        return fileMetadataRepository.searchByKeyword(keyword);
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadata> getFileMetadataByAuthor(String author) {
        log.info("Retrieving file metadata by author: {}", author);
        
        return fileMetadataRepository.findByAuthor(author);
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadata> getFileMetadataByFileType(String fileType) {
        log.info("Retrieving file metadata by file type: {}", fileType);
        
        return fileMetadataRepository.findByFileType(fileType);
    }
    
    public FileMetadata updateFileMetadata(Long id, FileMetadata updates) {
        log.info("Updating file metadata with ID: {}", id);
        
        FileMetadata fileMetadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException("File metadata not found with ID: " + id));
        
        if (updates.getFilename() != null) {
            fileMetadata.setFilename(updates.getFilename());
        }
        if (updates.getAuthor() != null) {
            fileMetadata.setAuthor(updates.getAuthor());
        }
        if (updates.getFileType() != null) {
            fileMetadata.setFileType(updates.getFileType());
        }
        if (updates.getSize() != null) {
            fileMetadata.setSize(updates.getSize());
        }
        if (updates.getObjectKey() != null) {
            fileMetadata.setObjectKey(updates.getObjectKey());
        }
        
        FileMetadata updatedMetadata = fileMetadataRepository.save(fileMetadata);
        log.info("File metadata updated with ID: {}", updatedMetadata.getId());
        
        return updatedMetadata;
    }
    
    public FileMetadata updateFileMetadataWithWorkerNotification(Long id, FileMetadata updates, boolean fileReplaced) {
        FileMetadata updatedMetadata = updateFileMetadata(id, updates);
        
        // Send to OCR Queue only if file was replaced (OCR will then send to GenAI)
        if (fileReplaced) {
            // Clear the old summary so UI shows "Summary is being generated..." and triggers auto-refresh
            updatedMetadata.setSummary(null);
            updatedMetadata = fileMetadataRepository.save(updatedMetadata);
            log.info("Summary cleared for file {} before reprocessing", updatedMetadata.getId());
            
            log.info("File with id {} was replaced, sending to OCR queue for reprocessing", updatedMetadata.getId());
            FileMessageDto fileMessage = new FileMessageDto(
                    updatedMetadata.getId(),
                    updatedMetadata.getFilename(),
                    updatedMetadata.getAuthor(),
                    updatedMetadata.getFileType(),
                    updatedMetadata.getSize(),
                    updatedMetadata.getUploadTime(),
                    updatedMetadata.getObjectKey()
            );
            messageProducerService.sendToOcrQueue(fileMessage);
        } else {
            // Only metadata updated (author change), send UPDATE event to Elasticsearch
            log.info("Only metadata updated for id {}, sending UPDATE event to search-indexing queue", updatedMetadata.getId());
            org.rest.dto.DocumentUpdateEventDto updateEvent = org.rest.dto.DocumentUpdateEventDto.builder()
                    .documentId(updatedMetadata.getId())
                    .filename(updatedMetadata.getFilename())
                    .author(updatedMetadata.getAuthor())
                    .fileType(updatedMetadata.getFileType())
                    .size(updatedMetadata.getSize())
                    .objectKey(updatedMetadata.getObjectKey())
                    .summary(updatedMetadata.getSummary())
                    .extractedText("") // Not needed for metadata-only updates
                    .eventType(org.rest.dto.DocumentUpdateEventDto.EventType.UPDATE)
                    .build();
            messageProducerService.sendDocumentUpdateEvent(updateEvent);
        }
        
        return updatedMetadata;
    }
    
    public void updateSummary(Long id, String summary) {
        log.info("Updating summary for file metadata with ID: {}", id);
        
        FileMetadata fileMetadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException("File metadata not found with ID: " + id));
        
        fileMetadata.setSummary(summary);
        fileMetadataRepository.save(fileMetadata);
        
        log.info("Summary updated for file metadata with ID: {} (summary length: {} chars)", 
                id, summary != null ? summary.length() : 0);
    }
    
    public void deleteFileMetadata(Long id) {
        log.info("Deleting file metadata with ID: {}", id);
        
        if (!fileMetadataRepository.existsById(id)) {
            throw new FileMetadataNotFoundException("File metadata not found with ID: " + id);
        }
        
        fileMetadataRepository.deleteById(id);
        log.info("File metadata deleted from database with ID: {}", id);
        
        // Send DELETE event to Elasticsearch
        log.info("Sending DELETE event to search-indexing queue for document ID: {}", id);
        org.rest.dto.DocumentUpdateEventDto deleteEvent = org.rest.dto.DocumentUpdateEventDto.builder()
                .documentId(id)
                .eventType(org.rest.dto.DocumentUpdateEventDto.EventType.DELETE)
                .build();
        messageProducerService.sendDocumentUpdateEvent(deleteEvent);
    }
}