package com.cinemasaar.queue.admission.repository;

import com.cinemasaar.queue.admission.model.ShowtimeGate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShowtimeGateRepository extends MongoRepository<ShowtimeGate, String> {
}
