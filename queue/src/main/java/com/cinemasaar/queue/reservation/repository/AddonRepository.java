package com.cinemasaar.queue.reservation.repository;

import com.cinemasaar.queue.reservation.model.Addon;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AddonRepository extends MongoRepository<Addon, String> {

    List<Addon> findByActiveTrue();
}
