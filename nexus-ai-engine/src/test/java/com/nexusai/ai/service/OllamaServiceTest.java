package com.nexusai.ai.service;

import com.nexusai.ai.dto.ChatRequest;
import com.nexusai.ai.dto.ChatResponse;
import com.nexusai.core.enums.MessageRole;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaService Tests")
class OllamaServiceTest {

    private MockWebServer mockWebServer;
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String baseUrl = mockWebServer.url("/").toString();
        ollamaService = new OllamaService(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("Chat Tests")
    class ChatTests {

        @Test
        @DisplayName("Should return chat response successfully")
        void shouldReturnChatResponseSuccessfully() {
            String responseJson = """
                {
                    "model": "llama3",
                    "message": {
                        "role": "assistant",
                        "content": "Hello! How can I help you today?"
                    },
                    "done": true,
                    "eval_count": 15
                }
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .addHeader("Content-Type", "application/json"));

            ChatRequest request = ChatRequest.builder()
                    .model("llama3")
                    .messages(List.of(
                            ChatRequest.Message.builder()
                                    .role(MessageRole.USER)
                                    .content("Hello!")
                                    .build()
                    ))
                    .build();

            ChatResponse response = ollamaService.chat(request);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEqualTo("Hello! How can I help you today?");
            assertThat(response.getTokensUsed()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should handle empty response gracefully")
        void shouldHandleEmptyResponseGracefully() {
            String responseJson = """
                {
                    "model": "llama3",
                    "message": null,
                    "done": true
                }
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .addHeader("Content-Type", "application/json"));

            ChatRequest request = ChatRequest.builder()
                    .model("llama3")
                    .messages(List.of(
                            ChatRequest.Message.builder()
                                    .role(MessageRole.USER)
                                    .content("Hello!")
                                    .build()
                    ))
                    .build();

            ChatResponse response = ollamaService.chat(request);

            assertThat(response).isNotNull();
            assertThat(response.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should use default model when not specified")
        void shouldUseDefaultModelWhenNotSpecified() {
            String responseJson = """
                {
                    "model": "llama3",
                    "message": {
                        "role": "assistant",
                        "content": "Response"
                    },
                    "done": true
                }
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseJson)
                    .addHeader("Content-Type", "application/json"));

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            ChatRequest.Message.builder()
                                    .role(MessageRole.USER)
                                    .content("Hello!")
                                    .build()
                    ))
                    .build();

            ChatResponse response = ollamaService.chat(request);

            assertThat(response).isNotNull();
        }
    }

    @Nested
    @DisplayName("ChatStream Tests")
    class ChatStreamTests {

        @Test
        @DisplayName("Should stream chat response")
        void shouldStreamChatResponse() {
            String chunk1 = """
                {"message":{"content":"Hello"},"done":false}
                """;
            String chunk2 = """
                {"message":{"content":" World"},"done":false}
                """;
            String chunk3 = """
                {"message":{"content":"!"},"done":true}
                """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(chunk1 + chunk2 + chunk3)
                    .addHeader("Content-Type", "application/x-ndjson"));

            ChatRequest request = ChatRequest.builder()
                    .model("llama3")
                    .messages(List.of(
                            ChatRequest.Message.builder()
                                    .role(MessageRole.USER)
                                    .content("Hello!")
                                    .build()
                    ))
                    .build();

            Flux<String> stream = ollamaService.chatStream(request);

            StepVerifier.create(stream)
                    .expectNextMatches(s -> !s.isEmpty())
                    .thenConsumeWhile(s -> true)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Provider Info Tests")
    class ProviderInfoTests {

        @Test
        @DisplayName("Should return correct provider name")
        void shouldReturnCorrectProviderName() {
            assertThat(ollamaService.getProviderName()).isEqualTo("ollama");
        }

        @Test
        @DisplayName("Should return available when server responds")
        void shouldReturnAvailableWhenServerResponds() {
            mockWebServer.enqueue(new MockResponse()
                    .setBody("{\"models\":[]}")
                    .addHeader("Content-Type", "application/json"));

            assertThat(ollamaService.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("Should return unavailable when server fails")
        void shouldReturnUnavailableWhenServerFails() {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));

            assertThat(ollamaService.isAvailable()).isFalse();
        }
    }
}
