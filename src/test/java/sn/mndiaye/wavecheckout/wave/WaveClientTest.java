package sn.mndiaye.wavecheckout.wave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import sn.mndiaye.wavecheckout.config.WaveProperties;

class WaveClientTest {

    private MockRestServiceServer server;
    private WaveClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new WaveClient(builder, new WaveProperties(
                "https://api.wave.com", "wave_sn_prod_test", "secret",
                "https://exemple.sn/succes", "https://exemple.sn/erreur"));
    }

    @Test
    void createsCheckoutSession() {
        server.expect(requestTo("https://api.wave.com/v1/checkout/sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer wave_sn_prod_test"))
                .andExpect(jsonPath("$.amount").value("1000"))
                .andExpect(jsonPath("$.currency").value("XOF"))
                .andExpect(jsonPath("$.client_reference").value("ref-1"))
                .andExpect(jsonPath("$.success_url").value("https://exemple.sn/succes"))
                .andRespond(withSuccess("""
                        {"id":"cos-18qq25rgr100a","amount":"1000","currency":"XOF",
                         "client_reference":"ref-1","checkout_status":"open",
                         "payment_status":"processing",
                         "wave_launch_url":"https://pay.wave.com/c/cos-18qq25rgr100a"}
                        """, MediaType.APPLICATION_JSON));

        WaveCheckoutSession session = client.createSession(1000, "XOF", "ref-1");

        assertThat(session.id()).isEqualTo("cos-18qq25rgr100a");
        assertThat(session.waveLaunchUrl()).isEqualTo("https://pay.wave.com/c/cos-18qq25rgr100a");
        server.verify();
    }

    @Test
    void findsSessionByClientReference() {
        server.expect(requestTo("https://api.wave.com/v1/checkout/sessions/search?client_reference=ref-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"result":[{"id":"cos-1","client_reference":"ref-1",
                         "checkout_status":"complete","payment_status":"succeeded",
                         "transaction_id":"TCN4Y4ZC3FM"}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.findByClientReference("ref-1"))
                .hasValueSatisfying(s -> assertThat(s.paymentStatus()).isEqualTo("succeeded"));
    }

    @Test
    void wrapsApiErrors() {
        server.expect(requestTo("https://api.wave.com/v1/checkout/sessions"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.createSession(1000, "XOF", "ref-1"))
                .isInstanceOf(WaveApiException.class);
    }
}
