package org.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.ChatMessageRequestDto;
import org.rest.dto.ChatMessageResponseDto;
import org.rest.model.ChatMessage;
import org.rest.service.ChatMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing chat message history and conversation sessions
 */
@RestController
@RequestMapping("/api/v1/chat-messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat Message Management", description = "API for managing chat conversation history")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    @Operation(summary = "Save chat message", description = "Save a chat message to the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Chat message saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ChatMessageResponseDto> saveChatMessage(
            @Valid @RequestBody ChatMessageRequestDto requestDto) {
        log.info("Received request to save chat message with role: {}", requestDto.getRole());

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setRole(requestDto.getRole());
        chatMessage.setContent(requestDto.getContent());
        chatMessage.setSessionId(requestDto.getSessionId());
        chatMessage.setTimestamp(Instant.now());

        ChatMessage saved = chatMessageService.saveChatMessage(chatMessage);

        ChatMessageResponseDto response = mapToResponseDto(saved);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all chat messages", description = "Retrieve all chat messages ordered by timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat messages retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ChatMessageResponseDto>> getAllChatMessages(
            @Parameter(description = "Filter by session ID (optional)") 
            @RequestParam(required = false) String sessionId) {
        log.info("Received request to get chat messages. Session ID: {}", sessionId);

        List<ChatMessage> messages;
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            messages = chatMessageService.getChatMessagesBySession(sessionId.trim());
        } else {
            messages = chatMessageService.getAllChatMessages();
        }

        List<ChatMessageResponseDto> response = messages.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get chat messages by session", description = "Retrieve all chat messages for a specific session ordered by timestamp")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat messages retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ChatMessageResponseDto>> getChatMessagesBySession(
            @Parameter(description = "Session ID to filter by") 
            @PathVariable String sessionId) {
        log.info("Received request to get chat messages for session: {}", sessionId);

        List<ChatMessage> messages = chatMessageService.getChatMessagesBySession(sessionId);

        List<ChatMessageResponseDto> response = messages.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "Delete chat messages", description = "Delete all chat messages or by session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chat messages deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteChatMessages(
            @Parameter(description = "Session ID to delete (optional, deletes all if not provided)") 
            @RequestParam(required = false) String sessionId) {
        log.info("Received request to delete chat messages. Session ID: {}", sessionId);

        if (sessionId != null && !sessionId.trim().isEmpty()) {
            chatMessageService.deleteChatMessagesBySession(sessionId.trim());
        } else {
            chatMessageService.deleteAllChatMessages();
        }

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "Delete chat messages by session", description = "Delete all chat messages for a specific session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chat messages deleted successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteChatMessagesBySession(
            @Parameter(description = "Session ID to delete") 
            @PathVariable String sessionId) {
        log.info("Received request to delete chat messages for session: {}", sessionId);

        chatMessageService.deleteChatMessagesBySession(sessionId);

        return ResponseEntity.noContent().build();
    }

    private ChatMessageResponseDto mapToResponseDto(ChatMessage chatMessage) {
        return ChatMessageResponseDto.builder()
                .id(chatMessage.getId())
                .role(chatMessage.getRole())
                .content(chatMessage.getContent())
                .sessionId(chatMessage.getSessionId())
                .timestamp(chatMessage.getTimestamp())
                .build();
    }
}
