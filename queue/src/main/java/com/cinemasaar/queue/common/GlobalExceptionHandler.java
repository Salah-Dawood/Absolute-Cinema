package com.cinemasaar.queue.common;

import com.cinemasaar.queue.admission.exception.AdmissionExpiredException;
import com.cinemasaar.queue.admission.exception.AdmissionRequiredException;
import com.cinemasaar.queue.common.exception.NotFoundException;
import com.cinemasaar.queue.payment.exception.PaymentException;
import com.cinemasaar.queue.reservation.exception.BookingBusyException;
import com.cinemasaar.queue.reservation.exception.CartExpiredException;
import com.cinemasaar.queue.reservation.exception.CartStateException;
import com.cinemasaar.queue.reservation.exception.SeatUnavailableException;
import com.cinemasaar.queue.reservation.exception.SeatsLostException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatUnavailableException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError seatUnavailable(SeatUnavailableException e) {
        return new ApiError("SEAT_UNAVAILABLE", e.getMessage(), e.getSeatId());
    }

    @ExceptionHandler(SeatsLostException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError seatsLost(SeatsLostException e) {
        return new ApiError("SEATS_LOST",
                "Some seats are no longer available; remove them and try again",
                e.getSeatIds());
    }

    @ExceptionHandler(CartExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiError cartExpired(CartExpiredException e) {
        return new ApiError("CART_EXPIRED", e.getMessage());
    }

    @ExceptionHandler(CartStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError cartState(CartStateException e) {
        return new ApiError("CART_STATE", e.getMessage());
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError payment(PaymentException e) {
        return new ApiError(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({OptimisticLockingFailureException.class,
            MongoTransactionException.class,
            TransactionSystemException.class,
            DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError retry(Exception e) {
        return new ApiError("RETRY", "Another request changed the same data; retry");
    }

    @ExceptionHandler(BookingBusyException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError busy(BookingBusyException e) {
        return new ApiError("BOOKING_BUSY", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(NotFoundException e) {
        return new ApiError("NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler(AdmissionRequiredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError admissionRequired(AdmissionRequiredException e) {
        return new ApiError("ADMISSION_REQUIRED", e.getMessage());
    }

    @ExceptionHandler(AdmissionExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public ApiError admissionExpired(AdmissionExpiredException e) {
        return new ApiError("ADMISSION_EXPIRED", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError == null ? "Invalid request" : fieldError.getDefaultMessage();
        return new ApiError("VALIDATION", message);
    }
}
