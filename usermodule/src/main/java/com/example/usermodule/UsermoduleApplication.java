package com.example.usermodule;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UsermoduleApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.directory("D:/Project/usermodule/usermodule") // nơi .env
				.load();
		System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID"));
		System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET"));
		SpringApplication.run(UsermoduleApplication.class, args);
	}

}
