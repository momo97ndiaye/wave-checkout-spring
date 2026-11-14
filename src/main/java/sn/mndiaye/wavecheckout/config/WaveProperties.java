package sn.mndiaye.wavecheckout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration Wave, lue depuis application.yml / variables d'environnement.
 *
 * @param baseUrl       URL de l'API (https://api.wave.com)
 * @param apiKey        clé API Wave Business (Bearer)
 * @param signingSecret secret de signature des webhooks (différent de la clé API)
 * @param successUrl    redirection après paiement réussi
 * @param errorUrl      redirection après échec
 */
@ConfigurationProperties(prefix = "wave")
public record WaveProperties(
        String baseUrl,
        String apiKey,
        String signingSecret,
        String successUrl,
        String errorUrl) {
}
