package com.ielts.writing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic writingScoringRequestTopic() {
        return TopicBuilder.name("writing.scoring.request")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic writingScoringResultTopic() {
        return TopicBuilder.name("writing.scoring.result")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
