package net.dima.project.service.chat;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.dima.project.dto.ChatRequestDTO;
import net.dima.project.dto.FastApiChatResponseDTO;

@Service
public class FastApiChatGatewayService {

    private final RestTemplate restTemplate;

    @Value("${fastapi.base-url}")
    private String fastApiBase;

    public FastApiChatGatewayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FastApiChatResponseDTO chat(String mode, ChatRequestDTO request) {
        String targetUrl = resolveTargetUrl(mode);

        // FastAPI는 snake_case(body: chat_history)를 기대하므로, 전송 payload도 동일하게 맞춥니다.
        // (Jackson record 직렬화 이슈를 피하기 위해 Map으로 한 번 더 명시)
        Map<String, Object> payload = Map.of(
                "sessionId", request.sessionId(),
                "question", request.question(),
                "chat_history", (request.chatHistory() == null) ? List.of() : request.chatHistory());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<FastApiChatResponseDTO> resp = restTemplate.postForEntity(
                targetUrl,
                entity,
                FastApiChatResponseDTO.class);

        return resp.getBody();
    }

    private String resolveTargetUrl(String mode) {
        return switch (mode) {
            case "hs" -> fastApiBase + "/hs";
            case "nav" -> fastApiBase + "/nav";
            case "glossary" -> fastApiBase + "/glossary";
            case "faq" -> fastApiBase + "/faq";
            case "stats" -> fastApiBase + "/chat/stats";
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        };
    }
}
