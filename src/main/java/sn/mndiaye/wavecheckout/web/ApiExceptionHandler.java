package sn.mndiaye.wavecheckout.web;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import sn.mndiaye.wavecheckout.payment.PaymentNotFoundException;
import sn.mndiaye.wavecheckout.wave.WaveApiException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail notFound(PaymentNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(WaveApiException.class)
    ProblemDetail waveError(WaveApiException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
    }

    @ExceptionHandler(JsonProcessingException.class)
    ProblemDetail badJson(JsonProcessingException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "JSON invalide");
    }
}
