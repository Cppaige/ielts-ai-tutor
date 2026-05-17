package com.ielts.gateway.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisSubscriberConfig {

    private final ScoringProgressHandler scoringProgressHandler;

    public RedisSubscriberConfig(ScoringProgressHandler scoringProgressHandler) {
        this.scoringProgressHandler = scoringProgressHandler;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListenerAdapter(), new PatternTopic("scoring.progress:*"));
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter((org.springframework.data.redis.connection.MessageListener)
                (message, pattern) -> {
                    String channel = new String(message.getChannel());
                    String submissionId = channel.replace("scoring.progress:", "");
                    String body = new String(message.getBody());
                    scoringProgressHandler.sendProgress(submissionId, body);
                });
    }
}
