package eu.sakarah.tool.moulinette;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by proussel on 25/09/2017.
 */
@SpringBootApplication
@EnableScheduling
public class Application
{
    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }
}
