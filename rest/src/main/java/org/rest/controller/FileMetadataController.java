package org.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.dto.FileUploadDto;
import org.rest.mapper.FileMetadataMapper;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Metadata Management", description = "API for managing file metadata in the paperless document system")
public class FileMetadataController {

    private final FileMetadataService fileMetadataService;
    private final FileMetadataMapper fileMetadataMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file with metadata", description = "Upload a file and create metadata entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or file"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Author of the document") @RequestParam("author") String author) throws IOException {

        log.info("Received request to upload file: {} by author: {}", file.getOriginalFilename(), author);

        if (file.isEmpty()) {
            log.error("Uploaded file is empty");
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Create DTO and use MapStruct to map to entity
        FileUploadDto uploadDto = new FileUploadDto(author);
        FileMetadata fileMetadata = fileMetadataMapper.toEntity(uploadDto, file);

        // Save metadata and file (file storage logic to be implemented)
        FileMetadata savedMetadata = fileMetadataService.createFileMetadata(fileMetadata);

        // TODO: Store the actual file bytes (file.getBytes()) to a storage system
        log.info("File metadata created with ID: {}. File storage not yet implemented.", savedMetadata.getId());

        FileMetadataResponseDto response = fileMetadataMapper.toResponseDto(savedMetadata);
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
        FileMetadataResponseDto response = fileMetadataMapper.toResponseDto(fileMetadata);
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

        List<FileMetadataResponseDto> response = fileMetadataMapper.toResponseDtoList(fileMetadataList);

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

        FileMetadata updates = fileMetadataMapper.toEntity(updateDto);
        FileMetadata updatedMetadata = fileMetadataService.updateFileMetadata(id, updates);
        FileMetadataResponseDto response = fileMetadataMapper.toResponseDto(updatedMetadata);
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
}