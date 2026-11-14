package sn.mndiaye.wavecheckout.wave;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import sn.mndiaye.wavecheckout.config.WaveProperties;

/**
 * Client de l'API Wave Checkout (https://docs.wave.com/checkout).
 */
public class WaveClient {

    private final RestClient restClient;
    private final WaveProperties properties;

    public WaveClient(RestClient.Builder builder, WaveProperties properties) {
        this.properties = properties;
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    /** POST /v1/checkout/sessions */
    public WaveCheckoutSession createSession(long amount, String currency, String clientReference) {
        Map<String, String> body = Map.of(
                "amount", String.valueOf(amount),
                "currency", currency,
                "client_reference", clientReference,
                "success_url", properties.successUrl(),
                "error_url", properties.errorUrl());
        try {
            return restClient.post()
                    .uri("/v1/checkout/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(WaveCheckoutSession.class);
        } catch (RestClientException e) {
            throw new WaveApiException("Création de la session Wave impossible", e);
        }
    }

    /** GET /v1/checkout/sessions/search?client_reference=... */
    public Optional<WaveCheckoutSession> findByClientReference(String clientReference) {
        try {
            SearchResult result = restClient.get()
                    .uri(uri -> uri.path("/v1/checkout/sessions/search")
                            .queryParam("client_reference", clientReference)
                            .build())
                    .retrieve()
                    .body(SearchResult.class);
            if (result == null || result.result() == null) {
                return Optional.empty();
            }
            return result.result().stream().findFirst();
        } catch (RestClientException e) {
            throw new WaveApiException("Recherche de la session Wave impossible", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResult(List<WaveCheckoutSession> result) {
    }
}
