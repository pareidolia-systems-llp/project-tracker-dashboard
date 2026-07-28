package com.pareidolia.projecttracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProjectTrackerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectTrackerServiceApplication.class, args);
	}

}