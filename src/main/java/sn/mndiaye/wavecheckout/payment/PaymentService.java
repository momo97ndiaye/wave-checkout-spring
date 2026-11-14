package sn.mndiaye.wavecheckout.payment;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.mndiaye.wavecheckout.wave.WaveCheckoutSession;
import sn.mndiaye.wavecheckout.wave.WaveClient;
import sn.mndiaye.wavecheckout.webhook.ProcessedEvent;
import sn.mndiaye.wavecheckout.webhook.ProcessedEventRepository;
import sn.mndiaye.wavecheckout.webhook.WaveEvent;

@Service
public class PaymentService {

    static final String CURRENCY = "XOF";
    static final String EVENT_COMPLETED = "checkout.session.completed";
    static final String EVENT_PAYMENT_FAILED = "checkout.session.payment_failed";

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final ProcessedEventRepository processedEvents;
    private final WaveClient waveClient;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, ProcessedEventRepository processedEvents,
                          WaveClient waveClient, Clock clock) {
        this.payments = payments;
        this.processedEvents = processedEvents;
        this.waveClient = waveClient;
        this.clock = clock;
    }

    /** Crée le paiement en base puis la session Wave. En cas d'erreur Wave, rien n'est enregistré. */
    @Transactional
    public Payment create(long amount) {
        Payment payment = payments.save(
                new Payment(UUID.randomUUID().toString(), amount, CURRENCY, clock.instant()));
        WaveCheckoutSession session =
                waveClient.createSession(amount, CURRENCY, payment.getClientReference());
        payment.attachSession(session.id(), session.waveLaunchUrl(), clock.instant());
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment get(String clientReference) {
        return payments.findByClientReference(clientReference)
                .orElseThrow(() -> new PaymentNotFoundException(clientReference));
    }

    /** Traite un événement webhook déjà authentifié. Renvoie false si l'événement a déjà été traité. */
    @Transactional
    public boolean handleEvent(WaveEvent event) {
        if (processedEvents.existsById(event.id())) {
            return false;
        }
        processedEvents.save(new ProcessedEvent(event.id(), event.type(), clock.instant()));

        WaveCheckoutSession session = event.data();
        if (session == null || session.clientReference() == null) {
            log.warn("Événement {} sans client_reference, ignoré", event.id());
            return true;
        }
        Optional<Payment> found = payments.findByClientReference(session.clientReference());
        if (found.isEmpty()) {
            log.warn("Événement {} : paiement {} inconnu", event.id(), session.clientReference());
            return true;
        }

        PaymentStatus newStatus = switch (event.type()) {
            case EVENT_COMPLETED -> statusFrom(session);
            case EVENT_PAYMENT_FAILED -> PaymentStatus.FAILED;
            default -> null;
        };
        apply(found.get(), newStatus, session.transactionId());
        return true;
    }

    /** Relit l'état chez Wave, utile si un webhook a été perdu. */
    @Transactional
    public Payment reconcile(String clientReference) {
        Payment payment = get(clientReference);
        waveClient.findByClientReference(clientReference)
                .ifPresent(session -> apply(payment, statusFrom(session), session.transactionId()));
        return payment;
    }

    static PaymentStatus statusFrom(WaveCheckoutSession session) {
        if ("succeeded".equals(session.paymentStatus())) {
            return PaymentStatus.SUCCEEDED;
        }
        if ("expired".equals(session.checkoutStatus())) {
            return PaymentStatus.EXPIRED;
        }
        return null;
    }

    private void apply(Payment payment, PaymentStatus newStatus, String transactionId) {
        // Un paiement réussi reste réussi, quel que soit l'ordre d'arrivée des événements.
        if (newStatus == null || payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        payment.updateStatus(newStatus, transactionId, clock.instant());
    }
}
