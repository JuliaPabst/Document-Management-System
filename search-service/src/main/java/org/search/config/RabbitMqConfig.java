package org.search.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.queue.search-indexing}")
    private String searchIndexingQueueName;

    @Bean
    public Queue searchIndexingQueue() {
        return new Queue(searchIndexingQueueName, true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        
        // Configure class mapper to map class names from REST to search-service
        DefaultClassMapper classMapper = new DefaultClassMapper();
        classMapper.setTrustedPackages("org.rest.dto", "org.search.dto");
        
        // Map REST DTOs to search-service DTOs
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("org.rest.dto.DocumentIndexDto", org.search.dto.DocumentIndexDto.class);
        idClassMapping.put("org.rest.dto.DocumentUpdateEventDto", org.search.dto.DocumentUpdateEventDto.class);
        classMapper.setIdClassMapping(idClassMapping);
        
        converter.setClassMapper(classMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
