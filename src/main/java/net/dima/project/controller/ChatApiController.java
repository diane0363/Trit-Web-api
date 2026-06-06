package net.dima.project.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.dima.project.dto.ChatApiResponseDTO;
import net.dima.project.dto.ChatMetaDTO;
import net.dima.project.dto.ChatRequestDTO;
import net.dima.project.dto.FastApiChatResponseDTO;
import net.dima.project.dto.SessionIdResponseDTO;
import net.dima.project.service.chat.FastApiChatGatewayService;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // 필요 시 프론트 도메인으로 제한
public class ChatApiController {

    private final FastApiChatGatewayService fastApiChatGatewayService;

    public ChatApiController(FastApiChatGatewayService fastApiChatGatewayService) {
        this.fastApiChatGatewayService = fastApiChatGatewayService;
    }

    /** 세션 생성 */
    @PostMapping("/{mode}/new-session")
    public SessionIdResponseDTO newSession(@PathVariable("mode") String mode) {
        // 현재 구조에서는 서버에 세션 상태를 저장하지 않고, 클라이언트가 sessionId+chat_history를 들고 다니는 형태
        return new SessionIdResponseDTO(UUID.randomUUID().toString());
    }

    /** 챗봇 요청 */
    @PostMapping("/{mode}")
    public ChatApiResponseDTO chat(
            @PathVariable("mode") String mode,
            @RequestBody ChatRequestDTO body) {
        try {
            FastApiChatResponseDTO resp = fastApiChatGatewayService.chat(mode, body);

            String reply = (resp != null && resp.reply() != null) ? resp.reply() : "(응답 없음)";

            return new ChatApiResponseDTO(
                    reply,
                    new ChatMetaDTO(mode, false),
                    (resp != null) ? resp.chatHistory() : null);

        } catch (IllegalArgumentException e) {
            return new ChatApiResponseDTO(
                    "알 수 없는 모드입니다.",
                    new ChatMetaDTO(mode, true),
                    null);

        } catch (Exception e) {
            return new ChatApiResponseDTO(
                    "연동 오류: " + e.getMessage(),
                    new ChatMetaDTO(mode, true),
                    null);
        }
    }
}
