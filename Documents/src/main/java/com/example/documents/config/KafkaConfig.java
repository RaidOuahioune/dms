package com.example.documents.config;

import com.example.documents.dto.workflow.DocumentStatusEvent;
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
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

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
    
    // Medical document extraction topic
    @Bean
    public NewTopic medicalDocumentForExtractionTopic() {
        return TopicBuilder.name("medical-document-for-extraction")
                .partitions(3)
                .replicas(1)
                .build();
    }
    
    // Extraction response topic
    @Bean
    public NewTopic extractionResponseTopic() {
        return TopicBuilder.name("extraction-response")
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
    
    // Consumer configurations for general use
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.documents.dto.workflow.WorkflowEventDTO");
        return new DefaultKafkaConsumerFactory<>(props);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // Consumer configurations for workflow events (using specific type)
    @Bean
    public ConsumerFactory<String, WorkflowEventDTO> workflowEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        JsonDeserializer<WorkflowEventDTO> deserializer = new JsonDeserializer<>(WorkflowEventDTO.class);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeHeaders(false);
        
        return new DefaultKafkaConsumerFactory<>(props, 
                                               new StringDeserializer(), 
                                               deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, WorkflowEventDTO> workflowKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, WorkflowEventDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(workflowEventConsumerFactory());
        return factory;
    }

    // Consumer configurations for DocumentStatusEvent
    @Bean
    public ConsumerFactory<String, DocumentStatusEvent> documentStatusEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.documents.dto.workflow.DocumentStatusEvent");
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DocumentStatusEvent> documentStatusKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, DocumentStatusEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(documentStatusEventConsumerFactory());
        return factory;
    }

    // Producer configurations
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
