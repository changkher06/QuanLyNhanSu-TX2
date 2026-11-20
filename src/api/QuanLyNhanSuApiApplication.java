package api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** API-only: component scanning deliberately excludes view; no JFrame is created. */
@SpringBootApplication
public class QuanLyNhanSuApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuanLyNhanSuApiApplication.class, args);
    }
}
