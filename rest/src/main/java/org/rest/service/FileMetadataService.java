package org.rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.exception.FileMetadataNotFoundException;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FileMetadataService {
    
    private final FileMetadataRepository fileMetadataRepository;
    
    public FileMetadataResponseDto createFileMetadata(FileMetadataCreateDto createDto) {
        log.info("Creating file metadata for filename: {}", createDto.getFilename());
        
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFilename(createDto.getFilename());
        fileMetadata.setAuthor(createDto.getAuthor());
        fileMetadata.setFileType(createDto.getFileType());
        fileMetadata.setSize(createDto.getSize());
        
        FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
        log.info("File metadata created with ID: {}", savedMetadata.getId());
        
        return mapToResponseDto(savedMetadata);
    }
    
    @Transactional(readOnly = true)
    public FileMetadataResponseDto getFileMetadataById(Long id) {
        log.info("Retrieving file metadata with ID: {}", id);
        
        FileMetadata fileMetadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException("File metadata not found with ID: " + id));
        
        return mapToResponseDto(fileMetadata);
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadataResponseDto> getAllFileMetadata() {
        log.info("Retrieving all file metadata");
        
        return fileMetadataRepository.findByOrderByUploadTimeDesc()
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadataResponseDto> searchFileMetadata(String keyword) {
        log.info("Searching file metadata with keyword: {}", keyword);
        
        return fileMetadataRepository.searchByKeyword(keyword)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadataResponseDto> getFileMetadataByAuthor(String author) {
        log.info("Retrieving file metadata by author: {}", author);
        
        return fileMetadataRepository.findByAuthor(author)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<FileMetadataResponseDto> getFileMetadataByFileType(String fileType) {
        log.info("Retrieving file metadata by file type: {}", fileType);
        
        return fileMetadataRepository.findByFileType(fileType)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }
    
    public FileMetadataResponseDto updateFileMetadata(Long id, FileMetadataUpdateDto updateDto) {
        log.info("Updating file metadata with ID: {}", id);
        
        FileMetadata fileMetadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new FileMetadataNotFoundException("File metadata not found with ID: " + id));
        
        if (updateDto.getFilename() != null) {
            fileMetadata.setFilename(updateDto.getFilename());
        }
        if (updateDto.getAuthor() != null) {
            fileMetadata.setAuthor(updateDto.getAuthor());
        }
        if (updateDto.getFileType() != null) {
            fileMetadata.setFileType(updateDto.getFileType());
        }
        if (updateDto.getSize() != null) {
            fileMetadata.setSize(updateDto.getSize());
        }
        
        FileMetadata updatedMetadata = fileMetadataRepository.save(fileMetadata);
        log.info("File metadata updated with ID: {}", updatedMetadata.getId());
        
        return mapToResponseDto(updatedMetadata);
    }
    
    public void deleteFileMetadata(Long id) {
        log.info("Deleting file metadata with ID: {}", id);
        
        if (!fileMetadataRepository.existsById(id)) {
            throw new FileMetadataNotFoundException("File metadata not found with ID: " + id);
        }
        
        fileMetadataRepository.deleteById(id);
        log.info("File metadata deleted with ID: {}", id);
    }
    
    private FileMetadataResponseDto mapToResponseDto(FileMetadata fileMetadata) {
        return new FileMetadataResponseDto(
                fileMetadata.getId(),
                fileMetadata.getFilename(),
                fileMetadata.getAuthor(),
                fileMetadata.getFileType(),
                fileMetadata.getSize(),
                fileMetadata.getUploadTime(),
                fileMetadata.getLastEdited()
        );
    }
}