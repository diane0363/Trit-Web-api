package net.dima.project.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// Java 객체를 JSON으로 직렬화할 때, 값이 null인 필드를 JSON 응답에서 아예 제외
// FastAPI에서 데이터를 받을 때 작동X, 스프링 서버가 이 FastApiChatResponseDTO를 받아서 최종적으로 프론트엔드로 다시 반환할 때 작동
@JsonInclude(JsonInclude.Include.NON_NULL)
// record -> 불변 객체, 내부적으로 getter, 생성자 등을 자동 생성
public record FastApiChatResponseDTO(
                String reply,
                List<Map<String, Object>> sources,
                // FastAPI가 응답한 JSON의 "chat_history" 키를 Java의 chatHistory 필드에 (역직렬화)
                // 逆直列化 (ぎゃくちょくれつか)
                @JsonProperty("chat_history") List<ChatTurnDTO> chatHistory) {
}
