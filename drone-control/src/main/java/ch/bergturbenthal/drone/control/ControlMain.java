package ch.bergturbenthal.drone.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class ControlMain {
	public static void main(final String[] args) {
		SpringApplication.run(ControlMain.class, args);
	}

}
