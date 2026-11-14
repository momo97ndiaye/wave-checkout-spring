package sn.mndiaye.wavecheckout.payment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_reference", nullable = false, unique = true)
    private String clientReference;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "wave_session_id")
    private String waveSessionId;

    @Column(name = "launch_url")
    private String launchUrl;

    @Column(name = "wave_transaction_id")
    private String waveTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    public Payment(String clientReference, long amount, String currency, Instant now) {
        this.clientReference = clientReference;
        this.amount = amount;
        this.currency = currency;
        this.status = PaymentStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachSession(String waveSessionId, String launchUrl, Instant now) {
        this.waveSessionId = waveSessionId;
        this.launchUrl = launchUrl;
        this.updatedAt = now;
    }

    public void updateStatus(PaymentStatus status, String waveTransactionId, Instant now) {
        this.status = status;
        if (waveTransactionId != null) {
            this.waveTransactionId = waveTransactionId;
        }
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getClientReference() { return clientReference; }
    public long getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getWaveSessionId() { return waveSessionId; }
    public String getLaunchUrl() { return launchUrl; }
    public String getWaveTransactionId() { return waveTransactionId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
