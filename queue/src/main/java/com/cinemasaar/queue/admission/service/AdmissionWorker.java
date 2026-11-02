package com.cinemasaar.queue.admission.service;

import com.cinemasaar.queue.config.BookingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdmissionWorker {

    private static final Logger log = LoggerFactory.getLogger(AdmissionWorker.class);

    private final AdmissionService admission;
    private final BookingProperties props;

    public AdmissionWorker(AdmissionService admission, BookingProperties props) {
        this.admission = admission;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${app.booking.admission-interval-ms:1000}")
    public void admitWaiting() {
        for (String showtimeId : admission.showtimesWithWaitingUsers()) {
            for (int i = 0; i < props.admissionBatchSize(); i++) {
                if (!admission.tryAdmitNext(showtimeId)) {
                    break;
                }
                log.debug("admitted next user for showtime={}", showtimeId);
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.booking.admission-expiry-interval-ms:5000}")
    public void expireAdmissions() {
        int released = admission.expireAdmissions();
        if (released > 0) {
            log.debug("expired {} admission(s) and released gate slots", released);
        }
    }
}
