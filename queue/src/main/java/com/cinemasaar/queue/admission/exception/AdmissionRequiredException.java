package com.cinemasaar.queue.admission.exception;

public class AdmissionRequiredException extends RuntimeException {

    public AdmissionRequiredException() {
        super("Admission token required");
    }

    public AdmissionRequiredException(String message) {
        super(message);
    }
}
