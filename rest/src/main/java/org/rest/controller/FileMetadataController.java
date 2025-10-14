package org.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Metadata Management", description = "API for managing file metadata in the paperless document system")
public class FileMetadataController {
    
    private final FileMetadataService fileMetadataService;
    
    @PostMapping
    @Operation(summary = "Upload file metadata", description = "Create new file metadata entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File metadata created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> uploadFileMetadata(
            @Valid @RequestBody FileMetadataCreateDto createDto) {
        log.info("Received request to upload file metadata for: {}", createDto.getFilename());
        // multipath file handling can be added here if needed

        FileMetadata fileMetadata = mapToEntity(createDto);
        FileMetadata savedMetadata = fileMetadataService.createFileMetadata(fileMetadata);
        FileMetadataResponseDto response = mapToResponseDto(savedMetadata);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get file metadata by ID", description = "Retrieve file metadata by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "File metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> getFileMetadata(
            @Parameter(description = "File metadata ID") @PathVariable Long id) {
        log.info("Received request to get file metadata with ID: {}", id);
        
        FileMetadata fileMetadata = fileMetadataService.getFileMetadataById(id);
        FileMetadataResponseDto response = mapToResponseDto(fileMetadata);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @Operation(summary = "Get all file metadata", description = "Retrieve all file metadata entries, optionally filtered")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<FileMetadataResponseDto>> getAllFileMetadata(
            @Parameter(description = "Search keyword (optional)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by author (optional)") @RequestParam(required = false) String author,
            @Parameter(description = "Filter by file type (optional)") @RequestParam(required = false) String fileType) {
        log.info("Received request to get all file metadata with filters - search: {}, author: {}, fileType: {}", 
                search, author, fileType);
        
        List<FileMetadata> fileMetadataList;
        
        if (search != null && !search.trim().isEmpty()) {
            fileMetadataList = fileMetadataService.searchFileMetadata(search.trim());
        } else if (author != null && !author.trim().isEmpty()) {
            fileMetadataList = fileMetadataService.getFileMetadataByAuthor(author.trim());
        } else if (fileType != null && !fileType.trim().isEmpty()) {
            fileMetadataList = fileMetadataService.getFileMetadataByFileType(fileType.trim());
        } else {
            fileMetadataList = fileMetadataService.getAllFileMetadata();
        }
        
        List<FileMetadataResponseDto> response = fileMetadataList.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @PatchMapping("/{id}")
    @Operation(summary = "Update file metadata", description = "Partially update file metadata")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "File metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> updateFileMetadata(
            @Parameter(description = "File metadata ID") @PathVariable Long id,
            @RequestBody FileMetadataUpdateDto updateDto) {
        log.info("Received request to update file metadata with ID: {}", id);
        
        FileMetadata updates = mapToEntity(updateDto);
        FileMetadata updatedMetadata = fileMetadataService.updateFileMetadata(id, updates);
        FileMetadataResponseDto response = mapToResponseDto(updatedMetadata);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete file metadata", description = "Delete file metadata by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File metadata deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteFileMetadata(
            @Parameter(description = "File metadata ID") @PathVariable Long id) {
        log.info("Received request to delete file metadata with ID: {}", id);
        
        fileMetadataService.deleteFileMetadata(id);
        return ResponseEntity.noContent().build();
    }
    
    // Mapping methods - DTO to Entity and Entity to DTO
    private FileMetadata mapToEntity(FileMetadataCreateDto createDto) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFilename(createDto.getFilename());
        fileMetadata.setAuthor(createDto.getAuthor());
        fileMetadata.setFileType(createDto.getFileType());
        fileMetadata.setSize(createDto.getSize());
        return fileMetadata;
    }
    
    private FileMetadata mapToEntity(FileMetadataUpdateDto updateDto) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFilename(updateDto.getFilename());
        fileMetadata.setAuthor(updateDto.getAuthor());
        fileMetadata.setFileType(updateDto.getFileType());
        fileMetadata.setSize(updateDto.getSize());
        return fileMetadata;
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