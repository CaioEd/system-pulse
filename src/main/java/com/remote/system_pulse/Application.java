package com.remote.system_pulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(Application.class);
		builder.bannerMode(Banner.Mode.OFF);	// Desativa o banner do Spring
		builder.run(args);

		System.out.println("Aplicação inicializada com sucesso! ");
	}

}
