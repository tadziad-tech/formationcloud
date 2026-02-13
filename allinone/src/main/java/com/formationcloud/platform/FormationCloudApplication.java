package com.formationcloud.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FormationCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(FormationCloudApplication.class, args);
		System.out.println("\n" + "=======================================================\n"
				+ "   FormationCloud Platform démarrée avec succès!\n"
				+ "   Accédez à l'application: http://localhost:8080\n"
				+ "=======================================================\n");
	}
}