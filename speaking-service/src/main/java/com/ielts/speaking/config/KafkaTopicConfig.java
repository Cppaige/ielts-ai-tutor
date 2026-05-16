package com.ielts.speaking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic speakingReportRequestTopic() {
        return TopicBuilder.name("speaking.report.request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic speakingSessionResultTopic() {
        return TopicBuilder.name("speaking.session.result")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
