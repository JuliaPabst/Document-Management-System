package org.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequestDto {
    @NotBlank(message = "Role is required")
    private String role;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private String sessionId;
}
