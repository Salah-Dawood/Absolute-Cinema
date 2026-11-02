package com.cinemasaar.queue.admission.exception;

public class AdmissionExpiredException extends RuntimeException {

    public AdmissionExpiredException() {
        super("Admission expired; rejoin the queue");
    }
}
