package com.vbank.logging_service.kafka;



import com.vbank.logging_service.dto.LogMessage;
import com.vbank.logging_service.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogConsumer {

    private final LogService logService;

    @KafkaListener(
            topics = "${app.kafka.log-topic}"
    )
    public void consume(LogMessage message) {

        log.info("Received log from {}", message.getServiceName());
        System.out.println("Received from Kafka: " + message);

        logService.save(message);
        log.info("Log saved successfully");
    }
}