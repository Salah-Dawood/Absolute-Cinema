package Repository;

import model.Addon;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AddonRepository extends MongoRepository<Addon, String> {
    List<Addon> findByActiveTrue();
}
