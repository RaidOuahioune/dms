package com.example.documents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableJpaRepositories
@EnableKafka
public class DocumentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentsApplication.class, args);
	}

}
