package org.rest.listener;

import lombok.RequiredArgsConstructor;
import org.rest.service.MessageConsumerService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GenAIResultListener {

    private final MessageConsumerService messageConsumerService;

    @RabbitListener(queues = "genai-result-queue")
    public void handleGenAiResult(String result) {
        messageConsumerService.processGenAiResult(result);
    }
}
