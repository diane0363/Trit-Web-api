package net.dima.project.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatApiResponseDTO(
        String reply,
        ChatMetaDTO meta,
        @JsonProperty("chat_history") List<ChatTurnDTO> chatHistory
) {
}
