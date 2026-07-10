package com.patient.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class PatientServiceApplicationTests {

	@MockitoBean
	private KafkaTemplate<String, byte[]> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
