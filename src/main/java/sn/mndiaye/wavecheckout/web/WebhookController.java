package sn.mndiaye.wavecheckout.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import sn.mndiaye.wavecheckout.payment.PaymentService;
import sn.mndiaye.wavecheckout.webhook.WaveEvent;
import sn.mndiaye.wavecheckout.webhook.WebhookSignatureVerifier;

@RestController
public class WebhookController {

    private final WebhookSignatureVerifier verifier;
    private final PaymentService service;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookSignatureVerifier verifier, PaymentService service,
                             ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /** Le corps est lu en String : la signature porte sur les octets reçus, pas sur un JSON re-sérialisé. */
    @PostMapping("/webhooks/wave")
    public ResponseEntity<Void> receive(
            @RequestHeader(name = "Wave-Signature", required = false) String signature,
            @RequestBody String rawBody) throws JsonProcessingException {
        if (!verifier.isValid(signature, rawBody)) {
            return ResponseEntity.status(401).build();
        }
        WaveEvent event = objectMapper.readValue(rawBody, WaveEvent.class);
        if (event.id() == null || event.type() == null) {
            return ResponseEntity.badRequest().build();
        }
        service.handleEvent(event);
        return ResponseEntity.ok().build();
    }
}
