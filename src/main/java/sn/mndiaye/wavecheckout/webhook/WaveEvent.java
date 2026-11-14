package sn.mndiaye.wavecheckout.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import sn.mndiaye.wavecheckout.wave.WaveCheckoutSession;

/** Événement webhook Wave : checkout.session.completed ou checkout.session.payment_failed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WaveEvent(String id, String type, WaveCheckoutSession data) {
}
