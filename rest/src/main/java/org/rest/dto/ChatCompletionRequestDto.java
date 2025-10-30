package org.rest.dto;

import java.util.List;

/**
 * DTO for chat completion request
 */
public class ChatCompletionRequestDto {
    private String message;
    private List<ConversationMessage> conversationHistory;

    public static class ConversationMessage {
        private String role;
        private String content;

        public ConversationMessage() {}

        public ConversationMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public ChatCompletionRequestDto() {}

    public ChatCompletionRequestDto(String message, List<ConversationMessage> conversationHistory) {
        this.message = message;
        this.conversationHistory = conversationHistory;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ConversationMessage> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ConversationMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
}
