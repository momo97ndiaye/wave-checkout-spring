package sn.mndiaye.wavecheckout.payment;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String clientReference) {
        super("Paiement introuvable : " + clientReference);
    }
}
