package com.brcsrc.yaws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.brcsrc.yaws.shell.Executor;

@SpringBootApplication
public class YawsApplication {

	public static void main(String[] args) {
		SpringApplication.run(YawsApplication.class, args);
	}

}
