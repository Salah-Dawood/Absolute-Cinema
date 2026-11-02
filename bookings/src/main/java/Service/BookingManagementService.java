package Service;

import Repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class BookingManagementService {

    private final BookingRepository bookingRepository;
    private final AddonRepository addonRepository;

}
