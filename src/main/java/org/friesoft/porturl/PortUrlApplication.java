package org.friesoft.porturl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PortUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortUrlApplication.class, args);
    }

}
