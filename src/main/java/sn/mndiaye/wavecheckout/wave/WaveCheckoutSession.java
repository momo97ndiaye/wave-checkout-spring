package sn.mndiaye.wavecheckout.wave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Session de paiement renvoyée par Wave (champs utilisés par l'application).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WaveCheckoutSession(
        String id,
        String amount,
        String currency,
        @JsonProperty("client_reference") String clientReference,
        @JsonProperty("checkout_status") String checkoutStatus,
        @JsonProperty("payment_status") String paymentStatus,
        @JsonProperty("wave_launch_url") String waveLaunchUrl,
        @JsonProperty("transaction_id") String transactionId) {
}
