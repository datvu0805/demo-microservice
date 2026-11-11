package dat.com.inventory2service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Inventory2ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(Inventory2ServiceApplication.class, args);
    }

}
