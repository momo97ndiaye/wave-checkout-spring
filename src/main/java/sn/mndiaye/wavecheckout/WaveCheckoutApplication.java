package sn.mndiaye.wavecheckout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WaveCheckoutApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaveCheckoutApplication.class, args);
    }
}
