package org.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.model.FileMetadata;

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
