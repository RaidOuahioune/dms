package com.example.documents.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    
    // Document event topics
    @Bean
    public NewTopic documentCreatedTopic() {
        return TopicBuilder.name("document-created")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentUpdatedTopic() {
        return TopicBuilder.name("document-updated")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentDeletedTopic() {
        return TopicBuilder.name("document-deleted")
                .partitions(3)
                .replicas(1)
                .build();
    }
}