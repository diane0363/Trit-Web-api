package net.dima.project.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatRequestDTO(
        String sessionId,
        String question,
        @JsonProperty("chat_history") List<ChatTurnDTO> chatHistory
) {
}
