package org.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.dto.FileUploadDto;
import org.rest.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataMapper {

    /**
     * Maps file upload information to FileMetadata entity.
     * The filename, fileType, and size are extracted from the MultipartFile.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "lastEdited", ignore = true)
    @Mapping(target = "filename", expression = "java(file.getOriginalFilename())")
    @Mapping(target = "fileType", expression = "java(extractExtensionUpper(file.getOriginalFilename()))")
    @Mapping(target = "size", expression = "java(file.getSize())")
    @Mapping(target = "author", expression = "java(uploadDto.getAuthor())")
    FileMetadata toEntity(FileUploadDto uploadDto, MultipartFile file);

    /**
     * Helper method to extract file extension.
     */
    default String extractExtensionUpper(String filename) {
        if (filename == null) return "UNKNOWN";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "UNKNOWN";
        return filename.substring(idx + 1).toUpperCase();
    }

    /**
     * Maps FileMetadataUpdateDto to FileMetadata entity.
     * The id, uploadTime, and lastEdited fields are handled separately in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "lastEdited", ignore = true)
    @Mapping(target = "filename", expression = "java(updateDto.getFilename())")
    @Mapping(target = "author", expression = "java(updateDto.getAuthor())")
    @Mapping(target = "fileType", expression = "java(updateDto.getFileType())")
    @Mapping(target = "size", expression = "java(updateDto.getSize())")
    FileMetadata toEntity(FileMetadataUpdateDto updateDto);

    /**
     * Maps FileMetadata entity to FileMetadataResponseDto
     */
    @Mapping(target = "id", expression = "java(fileMetadata.getId())")
    @Mapping(target = "filename", expression = "java(fileMetadata.getFilename())")
    @Mapping(target = "author", expression = "java(fileMetadata.getAuthor())")
    @Mapping(target = "fileType", expression = "java(fileMetadata.getFileType())")
    @Mapping(target = "size", expression = "java(fileMetadata.getSize())")
    @Mapping(target = "uploadTime", expression = "java(fileMetadata.getUploadTime())")
    @Mapping(target = "lastEdited", expression = "java(fileMetadata.getLastEdited())")
    FileMetadataResponseDto toResponseDto(FileMetadata fileMetadata);

    /**
     * Maps a list of FileMetadata entities to a list of FileMetadataResponseDto
     */
    List<FileMetadataResponseDto> toResponseDtoList(List<FileMetadata> fileMetadataList);
}