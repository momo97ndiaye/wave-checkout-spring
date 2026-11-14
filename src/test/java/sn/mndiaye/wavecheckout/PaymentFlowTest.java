package sn.mndiaye.wavecheckout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import sn.mndiaye.wavecheckout.wave.WaveCheckoutSession;
import sn.mndiaye.wavecheckout.wave.WaveClient;
import sn.mndiaye.wavecheckout.webhook.ProcessedEventRepository;
import sn.mndiaye.wavecheckout.webhook.WebhookSignatureVerifier;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired WebhookSignatureVerifier verifier;
    @Autowired ProcessedEventRepository processedEvents;
    @MockBean WaveClient waveClient;

    @Test
    void paymentBecomesSucceededAfterSignedWebhook() throws Exception {
        String ref = createPayment();

        String event = completedEvent("EV_ok_" + ref, ref);
        mvc.perform(webhook(event, sign(event))).andExpect(status().isOk());
        // Même événement renvoyé par Wave : accepté mais traité une seule fois.
        mvc.perform(webhook(event, sign(event))).andExpect(status().isOk());

        mvc.perform(get("/api/payments/" + ref))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"));
        assertThat(processedEvents.existsById("EV_ok_" + ref)).isTrue();
    }

    @Test
    void rejectsWebhookWithInvalidSignature() throws Exception {
        String ref = createPayment();
        String event = completedEvent("EV_bad_" + ref, ref);

        mvc.perform(webhook(event, "t=" + Instant.now().getEpochSecond() + ",v1=00ff"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/webhooks/wave").contentType(MediaType.APPLICATION_JSON).content(event))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/payments/" + ref))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void rejectsInvalidAmount() throws Exception {
        mvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownPaymentReturns404() throws Exception {
        mvc.perform(get("/api/payments/inconnu")).andExpect(status().isNotFound());
    }

    private String createPayment() throws Exception {
        when(waveClient.createSession(anyLong(), eq("XOF"), anyString()))
                .thenAnswer(inv -> new WaveCheckoutSession("cos-test", "5000", "XOF", inv.getArgument(2),
                        "open", "processing", "https://pay.wave.com/c/cos-test", null));

        String json = mvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":5000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.launchUrl").value("https://pay.wave.com/c/cos-test"))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("clientReference").asText();
    }

    private static String completedEvent(String eventId, String ref) {
        return """
                {"id":"%s","type":"checkout.session.completed",
                 "data":{"id":"cos-test","amount":"5000","currency":"XOF",
                  "client_reference":"%s","checkout_status":"complete",
                  "payment_status":"succeeded","transaction_id":"TCN4Y4ZC3FM"}}
                """.formatted(eventId, ref);
    }

    private String sign(String body) {
        long t = Instant.now().getEpochSecond();
        return "t=" + t + ",v1=" + HexFormat.of().formatHex(verifier.hmac(t + body));
    }

    private static org.springframework.test.web.servlet.RequestBuilder webhook(String body, String signature) {
        return post("/webhooks/wave")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Wave-Signature", signature)
                .content(body);
    }
}
