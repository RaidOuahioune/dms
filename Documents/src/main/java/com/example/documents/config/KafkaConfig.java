package com.example.documents.config;

import com.example.documents.dto.workflow.WorkflowEventDTO;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
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
    
    @Bean
    public NewTopic documentUploadedTopic() {
        return TopicBuilder.name("document-uploaded")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // Workflow event topics - only need to define these for the service to consume them
    @Bean
    public NewTopic documentFieldsExtractedTopic() {
        return TopicBuilder.name("document-fields-extracted")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentValidatedTopic() {
        return TopicBuilder.name("document-validated")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentRejectedTopic() {
        return TopicBuilder.name("document-rejected")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    @Bean
    public NewTopic documentPublishedTopic() {
        return TopicBuilder.name("document-published")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // Consumer configurations for workflow events
    @Bean
    public ConsumerFactory<String, WorkflowEventDTO> workflowEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        JsonDeserializer<WorkflowEventDTO> deserializer = new JsonDeserializer<>(WorkflowEventDTO.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);
        
        return new DefaultKafkaConsumerFactory<>(props, 
                                               new org.apache.kafka.common.serialization.StringDeserializer(), 
                                               deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkflowEventDTO> workflowKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkflowEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workflowEventConsumerFactory());
        return factory;
    }
}