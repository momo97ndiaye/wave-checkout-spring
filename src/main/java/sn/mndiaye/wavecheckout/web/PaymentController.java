package sn.mndiaye.wavecheckout.web;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import sn.mndiaye.wavecheckout.payment.Payment;
import sn.mndiaye.wavecheckout.payment.PaymentService;
import sn.mndiaye.wavecheckout.payment.PaymentStatus;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return PaymentResponse.from(service.create(request.amount()));
    }

    @GetMapping("/{clientReference}")
    public PaymentResponse get(@PathVariable String clientReference) {
        return PaymentResponse.from(service.get(clientReference));
    }

    @PostMapping("/{clientReference}/reconcile")
    public PaymentResponse reconcile(@PathVariable String clientReference) {
        return PaymentResponse.from(service.reconcile(clientReference));
    }

    /** Montant en francs CFA (XOF), sans décimales. */
    public record CreatePaymentRequest(@Min(1) long amount) {
    }

    public record PaymentResponse(String clientReference, long amount, String currency,
                                  PaymentStatus status, String launchUrl, Instant createdAt) {
        static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getClientReference(), p.getAmount(), p.getCurrency(),
                    p.getStatus(), p.getLaunchUrl(), p.getCreatedAt());
        }
    }
}
