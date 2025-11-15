package com.concrete.buildup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class BuildupApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuildupApplication.class, args);
	}

}
