package com.tm.tsm_atelier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.data.web.config.EnableSpringDataWebSupport(pageSerializationMode = org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class TsmAtelierApplication {

	public static void main(String[] args) {
		SpringApplication.run(TsmAtelierApplication.class, args);
	}

}
