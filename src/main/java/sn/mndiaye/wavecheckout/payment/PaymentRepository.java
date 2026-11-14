package sn.mndiaye.wavecheckout.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByClientReference(String clientReference);
}
