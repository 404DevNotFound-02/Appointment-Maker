package com.appointments.appoinment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AppoinmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppoinmentServiceApplication.class, args);
	}

}
