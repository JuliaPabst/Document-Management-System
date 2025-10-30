package org.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.ChatCompletionRequestDto;
import org.rest.dto.ChatCompletionResponseDto;
import org.rest.service.OpenAIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for chat completion using OpenAI
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat completion API using OpenAI")
public class ChatController {
    
    private final OpenAIService openAIService;
    
    @PostMapping
    @Operation(summary = "Generate chat completion", 
               description = "Generate a chat response using OpenAI with document database context")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat response generated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ChatCompletionResponseDto> generateChatCompletion(
            @RequestBody ChatCompletionRequestDto request) {
        
        log.info("Received chat completion request with message: {}", request.getMessage());
        
        try {
            String response = openAIService.generateChatCompletion(request);
            log.info("Chat completion generated successfully");
            
            return ResponseEntity.ok(new ChatCompletionResponseDto(response));
        } catch (Exception e) {
            log.error("Error generating chat completion", e);
            throw e;
        }
    }
}
