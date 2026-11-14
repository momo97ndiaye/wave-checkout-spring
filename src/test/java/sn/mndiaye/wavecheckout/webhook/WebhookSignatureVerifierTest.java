package sn.mndiaye.wavecheckout.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

    private static final String SECRET = "wave_sn_WHS_test";
    private static final String BODY = "{\"id\":\"EV_1\",\"type\":\"checkout.session.completed\"}";
    private static final long NOW = 1_700_000_000L;

    private final WebhookSignatureVerifier verifier =
            new WebhookSignatureVerifier(SECRET, Clock.fixed(Instant.ofEpochSecond(NOW), ZoneOffset.UTC));

    private String sign(long timestamp, String body, String secret) {
        WebhookSignatureVerifier signer = new WebhookSignatureVerifier(secret, Clock.systemUTC());
        return HexFormat.of().formatHex(signer.hmac(timestamp + body));
    }

    @Test
    void acceptsValidSignature() {
        String header = "t=" + NOW + ",v1=" + sign(NOW, BODY, SECRET);
        assertThat(verifier.isValid(header, BODY)).isTrue();
    }

    @Test
    void acceptsWhenOneOfSeveralSignaturesMatches() {
        String header = "t=" + NOW + ",v1=" + sign(NOW, BODY, "ancien-secret") + ",v1=" + sign(NOW, BODY, SECRET);
        assertThat(verifier.isValid(header, BODY)).isTrue();
    }

    @Test
    void rejectsWrongSecret() {
        String header = "t=" + NOW + ",v1=" + sign(NOW, BODY, "autre-secret");
        assertThat(verifier.isValid(header, BODY)).isFalse();
    }

    @Test
    void rejectsModifiedBody() {
        String header = "t=" + NOW + ",v1=" + sign(NOW, BODY, SECRET);
        assertThat(verifier.isValid(header, BODY.replace("EV_1", "EV_2"))).isFalse();
    }

    @Test
    void rejectsTimestampOlderThanFiveMinutes() {
        long old = NOW - 301;
        String header = "t=" + old + ",v1=" + sign(old, BODY, SECRET);
        assertThat(verifier.isValid(header, BODY)).isFalse();
    }

    @Test
    void rejectsMalformedHeaders() {
        assertThat(verifier.isValid(null, BODY)).isFalse();
        assertThat(verifier.isValid("", BODY)).isFalse();
        assertThat(verifier.isValid("t=" + NOW, BODY)).isFalse();
        assertThat(verifier.isValid("t=abc,v1=00", BODY)).isFalse();
        assertThat(verifier.isValid("t=" + NOW + ",v1=pas-de-l-hexa", BODY)).isFalse();
    }
}
