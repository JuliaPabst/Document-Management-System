package org.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.dto.FileUploadDto;
import org.rest.model.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FileMetadataMapper {

    /**
     * Maps FileMetadataCreateDto to FileMetadata entity.
     * The id, uploadTime, and lastEdited fields are set automatically by JPA.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "lastEdited", ignore = true)
    FileMetadata toEntity(FileMetadataCreateDto createDto);

    /**
     * Maps file upload information to FileMetadata entity.
     * The filename, fileType, and size are extracted from the MultipartFile.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "lastEdited", ignore = true)
    @Mapping(target = "filename", expression = "java(file.getOriginalFilename())")
    @Mapping(target = "fileType", expression = "java(getFileExtension(file.getOriginalFilename()))")
    @Mapping(target = "size", expression = "java(file.getSize())")
    @Mapping(target = "author", source = "uploadDto.author")
    FileMetadata toEntity(FileUploadDto uploadDto, MultipartFile file);

    /**
     * Helper method to extract file extension.
     */
    default String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Maps FileMetadataUpdateDto to FileMetadata entity.
     * The id, uploadTime, and lastEdited fields are handled separately in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "lastEdited", ignore = true)
    FileMetadata toEntity(FileMetadataUpdateDto updateDto);

    /**
     * Maps FileMetadata entity to FileMetadataResponseDto
     */
    FileMetadataResponseDto toResponseDto(FileMetadata fileMetadata);

    /**
     * Maps a list of FileMetadata entities to a list of FileMetadataResponseDto
     */
    List<FileMetadataResponseDto> toResponseDtoList(List<FileMetadata> fileMetadataList);
}