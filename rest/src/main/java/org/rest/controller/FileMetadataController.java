package org.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileUploadDto;
import org.rest.mapper.FileMetadataMapper;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.rest.service.FileStorage;
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
    private final FileStorage fileStorage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file with metadata", description = "Upload a file and create metadata entry")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or file"),
            @ApiResponse(responseCode = "409", description = "File with same name already exists for this author"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> uploadFile(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Author of the document") @RequestParam("author") String author) {

        log.info("Received request to upload file: {} by author: {}", file.getOriginalFilename(), author);

        if (file.isEmpty()) {
            log.error("Uploaded file is empty");
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            // Create DTO and use MapStruct to map to entity
            FileUploadDto uploadDto = new FileUploadDto();
            uploadDto.setAuthor(author);
            FileMetadata fileMetadata = fileMetadataMapper.toEntity(uploadDto, file);

            // Generate unique object key for MinIO (using timestamp + filename for uniqueness)
            String objectKey = String.format("%d-%s", System.currentTimeMillis(), file.getOriginalFilename());
            fileMetadata.setObjectKey(objectKey);

            // Upload file to MinIO
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            fileStorage.upload(objectKey, fileBytes, contentType);
            log.info("File uploaded to MinIO with object key: {}", objectKey);

            // Save metadata and notify workers
            FileMetadata savedMetadata = fileMetadataService.createFileMetadataWithWorkerNotification(fileMetadata);
            log.info("File metadata created with ID: {}, objectKey: {}", savedMetadata.getId(), savedMetadata.getObjectKey());

            FileMetadataResponseDto response = fileMetadataMapper.toResponseDto(savedMetadata);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IOException e) {
            log.error("Failed to read file bytes: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process file upload", e);
        }
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

    @GetMapping("/{id}/download")
    @Operation(summary = "Download file content", description = "Download the actual file content from MinIO storage")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "File or file metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "File metadata ID") @PathVariable Long id) {
        log.info("Received request to download file with ID: {}", id);

        FileMetadata fileMetadata = fileMetadataService.getFileMetadataById(id);
        byte[] fileContent = fileStorage.download(fileMetadata.getObjectKey());

        return ResponseEntity.ok()
                .header("Content-Type", fileMetadata.getFileType())
                .header("Content-Disposition", "attachment; filename=\"" + fileMetadata.getFilename() + "\"")
                .body(fileContent);
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

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update file metadata", description = "Partially update file metadata and optionally replace the file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "File metadata not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<FileMetadataResponseDto> updateFileMetadata(
            @Parameter(description = "File metadata ID") @PathVariable Long id,
            @Parameter(description = "New file to replace existing one (optional)") @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(description = "Author of the document (optional)") @RequestParam(value = "author", required = false) String author) {
        log.info("Received request to update file metadata with ID: {}", id);

        try {
            FileMetadata updates = new FileMetadata();
            boolean fileReplaced = false;
            
            // If a new file is uploaded, replace it in MinIO
            if (file != null && !file.isEmpty()) {
                log.info("Replacing file with new upload: {}", file.getOriginalFilename());
                
                // Get existing metadata to retrieve old objectKey
                FileMetadata existingMetadata = fileMetadataService.getFileMetadataById(id);
                String oldObjectKey = existingMetadata.getObjectKey();
                
                // Generate new unique object key
                String newObjectKey = String.format("%d-%s", System.currentTimeMillis(), file.getOriginalFilename());
                
                // Upload new file to MinIO
                byte[] fileBytes = file.getBytes();
                String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
                fileStorage.upload(newObjectKey, fileBytes, contentType);
                log.info("New file uploaded to MinIO with object key: {}", newObjectKey);
                
                // Delete old file from MinIO
                fileStorage.delete(oldObjectKey);
                log.info("Old file deleted from MinIO: {}", oldObjectKey);
                
                // Update metadata fields
                updates.setFilename(file.getOriginalFilename());
                updates.setFileType(fileMetadataMapper.extractExtensionUpper(file.getOriginalFilename()));
                updates.setSize(file.getSize());
                updates.setObjectKey(newObjectKey);
                fileReplaced = true;
            }
            
            // Update author if provided
            if (author != null && !author.trim().isEmpty()) {
                updates.setAuthor(author.trim());
            }
            
            // Update metadata and notify workers if file was replaced
            FileMetadata updatedMetadata = fileMetadataService.updateFileMetadataWithWorkerNotification(id, updates, fileReplaced);
            
            FileMetadataResponseDto response = fileMetadataMapper.toResponseDto(updatedMetadata);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read file bytes during update: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process file update", e);
        }
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