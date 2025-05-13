package com.seecoder.TomatoMall.util;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;
import java.util.List;

/* ① 读取配置 */
@Configuration
@ConfigurationProperties(prefix = "openai")
@Data
class OpenAiProps {
    private String baseUrl;
    private String apiKey;
    private String model;
    private Duration timeout = Duration.ofSeconds(30);
}

/* ② 客户端 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiUtil {

    private final OpenAiProps props;
    private RestTemplate restTemplate;

    /* 构造 RestTemplate（延迟初始化） */
    private RestTemplate rt() {
        if (restTemplate == null) {

            // ① 构造 RestTemplate
            restTemplate = new RestTemplate();
            restTemplate.setUriTemplateHandler(
                    new DefaultUriBuilderFactory(props.getBaseUrl()));

            // ② 配置超时
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) props.getTimeout().toMillis());
            factory.setReadTimeout((int) props.getTimeout().toMillis());

            restTemplate.setRequestFactory(factory);
        }
        return restTemplate;
    }

    /**
     * 同步获取 AI 回复
     * @param messages 历史消息(含最新用户输入)
     * @return assistant 的 content
     */
    public String chat(List<Message> messages) {
        ChatRequest req = new ChatRequest(props.getModel(), messages);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey().replace("Bearer ", ""));
        HttpEntity<ChatRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<ChatResponse> resp = rt()
                .postForEntity("/chat/completions", entity, ChatResponse.class);

        if (resp.getStatusCode().is2xxSuccessful()
                && resp.getBody() != null
                && !resp.getBody().choices.isEmpty()) {
            return resp.getBody().choices.get(0).message.content;
        }
        throw new RuntimeException("OpenAI 调用失败: " + resp.getStatusCode());
    }

    /* ---------- DTO ---------- */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ChatRequest {
        private final String model;
        private final List<Message> messages;
        private Double temperature = 0.8;
    }

    @Data
    @NoArgsConstructor                 // ← 新增
    @AllArgsConstructor                // ← 保留有参方便你构造请求
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;           // 取消 final
        private String content;        // 取消 final
    }

    @Data
    static class ChatResponse {
        List<Choice> choices;

        @Data
        static class Choice {
            Message message;
            @JsonAlias("finish_reason")
            String finishReason;
        }
    }

}
