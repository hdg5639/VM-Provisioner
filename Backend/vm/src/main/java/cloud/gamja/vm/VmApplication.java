package cloud.gamja.vm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.util.TimeZone;

@SpringBootApplication
@EnableR2dbcRepositories(basePackages = "cloud.gamja")
public class VmApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.setProperty("user.timezone", "Asia/Seoul");
        SpringApplication.run(VmApplication.class, args);
    }

}
