package org.rest.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rest.dto.FileMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class MessageProducerServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageProducerService messageProducerService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        // Set queue names using reflection to simulate Spring @Value injection
        // ReflectionTestUtils.setField(object, "fieldName", value);
        ReflectionTestUtils.setField(messageProducerService, "ocrQueueName", "ocr-worker-queue");
        ReflectionTestUtils.setField(messageProducerService, "genaiQueueName", "genai-worker-queue");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testSendToOcrQueue() {
        // arrange
        FileMessageDto message = new FileMessageDto(
                1L,
                "test.pdf",
                "Author",
                "PDF",
                1024L,
                Instant.now()
        );

        // act
        messageProducerService.sendToOcrQueue(message);

        // assert
        verify(rabbitTemplate).convertAndSend(eq("ocr-worker-queue"), eq(message));
    }

    @Test
    void testSendToGenAiQueue() {
        // arrange
        FileMessageDto message = new FileMessageDto(
                2L,
                "document.pdf",
                "Test Author",
                "PDF",
                2048L,
                Instant.now()
        );

        // act
        messageProducerService.sendToGenAiQueue(message);

        // assert
        verify(rabbitTemplate).convertAndSend(eq("genai-worker-queue"), eq(message));
    }
}