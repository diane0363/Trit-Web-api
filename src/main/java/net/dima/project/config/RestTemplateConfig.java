package net.dima.project.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateConfig.class);

    /**
     * FastAPI 연동용 RestTemplate
     * - connect/read timeout을 명시해 장애 시 스레드가 장시간 묶이는 것을 방지
     * - (디버깅용) 요청/응답 바디 로깅 인터셉터 추가
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestInterceptor loggingInterceptor = (request, body, execution) -> {
            String reqBody = safeTruncate(new String(body, StandardCharsets.UTF_8), 2000);
            log.info("[FASTAPI REQ] {} {} headers={} body={}",
                    request.getMethod(),
                    request.getURI(),
                    request.getHeaders(),
                    reqBody);

            // .execute() 실 서버의 HTTP 응답결과 리턴
            // 이걸 써야 인터셉트 가능!!
            var response = execution.execute(request, body);

            // 코드가 response body를 소비하면 스트림의 포인터가 EOF에 도달 -> 1회성 소비
            // 역직렬화를 깨지 않기 위해 BufferingClientHttpRequestFactory를 사용해 복사
            // 응답 데이터를 byte[] 배열(메모리)에 데이터를 복사 => OOM 가능성
            byte[] respBytes = StreamUtils.copyToByteArray(response.getBody());
            String respBody = safeTruncate(new String(respBytes, StandardCharsets.UTF_8), 4000);

            log.info("[FASTAPI RES] {} {} status={} headers={} body={}",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    response.getHeaders(),
                    respBody);

            // Service로
            return response;
        };

        return builder
                // HTTP 응답 바디는 Stream 형태 -> 로그를 찍기 위해 한 번 읽으면 실제 애플리케이션에서 읽을 데이터 소실
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .additionalInterceptors(loggingInterceptor)
                // TOMCAT의 스레드풀 고갈 (장애 전파) 방지
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(30))
                .build();
    }

    // OOM(Out Of Memory) 방지 ; fastapi에서 1gb의 데이터 응답 가능성 고려..
    private static String safeTruncate(String s, int maxLen) {
        if (s == null)
            return null;
        if (s.length() <= maxLen)
            return s;
        return s.substring(0, maxLen) + "... (truncated)";
    }
}
