package com.concrete.buildup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BuildupApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuildupApplication.class, args);
	}

}
