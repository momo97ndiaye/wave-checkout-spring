package sn.mndiaye.wavecheckout.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import sn.mndiaye.wavecheckout.wave.WaveClient;
import sn.mndiaye.wavecheckout.webhook.WebhookSignatureVerifier;

@Configuration
public class WaveConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    WaveClient waveClient(RestClient.Builder builder, WaveProperties properties) {
        return new WaveClient(builder, properties);
    }

    @Bean
    WebhookSignatureVerifier webhookSignatureVerifier(WaveProperties properties, Clock clock) {
        return new WebhookSignatureVerifier(properties.signingSecret(), clock);
    }
}
