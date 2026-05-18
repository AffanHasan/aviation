package com.example.aviation.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = "aircraft.state.vectors")
public class CucumberSpringConfiguration {
}
