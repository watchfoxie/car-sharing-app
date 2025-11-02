package com.services.car_sharing_app;

import org.springframework.boot.SpringApplication;

public class TestCarSharingAppApplication {

	public static void main(String[] args) {
		SpringApplication.from(CarSharingAppApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
