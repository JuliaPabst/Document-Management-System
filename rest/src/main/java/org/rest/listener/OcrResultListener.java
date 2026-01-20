package org.rest.listener;

import lombok.RequiredArgsConstructor;
import org.rest.service.MessageConsumerService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ listener that receives OCR results and delegates processing to MessageConsumerService
 */
@Component
@RequiredArgsConstructor
public class OcrResultListener {

    private final MessageConsumerService messageConsumerService;

    @RabbitListener(queues = "ocr-result-queue")
    public void handleOcrResult(String result) {
        messageConsumerService.processOcrResult(result);
    }
}
