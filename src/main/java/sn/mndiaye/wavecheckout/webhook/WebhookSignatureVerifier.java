package sn.mndiaye.wavecheckout.webhook;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Vérifie l'en-tête Wave-Signature : t=TIMESTAMP,v1=HEX[,v1=HEX...].
 * La signature est un HMAC-SHA256 de (timestamp + corps brut) avec le secret de signature.
 */
public class WebhookSignatureVerifier {

    static final Duration TOLERANCE = Duration.ofMinutes(5);

    private final byte[] secret;
    private final Clock clock;

    public WebhookSignatureVerifier(String signingSecret, Clock clock) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalArgumentException("Le secret de signature Wave est obligatoire");
        }
        this.secret = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    /**
     * @param header  valeur de l'en-tête Wave-Signature
     * @param rawBody corps de la requête, tel que reçu (sans re-sérialisation)
     */
    public boolean isValid(String header, String rawBody) {
        if (header == null || rawBody == null) {
            return false;
        }
        String timestamp = null;
        List<String> signatures = new ArrayList<>();
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if (kv[0].equals("t")) {
                timestamp = kv[1];
            } else if (kv[0].equals("v1")) {
                signatures.add(kv[1]);
            }
        }
        if (timestamp == null || signatures.isEmpty()) {
            return false;
        }

        long seconds;
        try {
            seconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }
        long age = clock.instant().getEpochSecond() - seconds;
        if (Math.abs(age) > TOLERANCE.toSeconds()) {
            return false;
        }

        byte[] expected = hmac(timestamp + rawBody);
        for (String signature : signatures) {
            byte[] received;
            try {
                received = HexFormat.of().parseHex(signature);
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (MessageDigest.isEqual(expected, received)) {
                return true;
            }
        }
        return false;
    }

    /** HMAC-SHA256 brut ; public pour que les tests puissent signer un événement. */
    public byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        }
    }
}
