package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.PerformerDTO;
import cz.fit.cvut.seatlock.repository.ActivityRepository;
import cz.fit.cvut.seatlock.repository.PerformerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformerService {
    private final PerformerRepository performerRepository;
    private final ActivityRepository activityRepository;

    public Performer createPerformer(PerformerDTO input) {
        if(performerRepository.findByName(input.name()).isPresent()){
            throw new RuntimeException("Performer with this name already exist");
        }

        Performer performer = new Performer(input.name(), input.description());
        return performerRepository.save(performer);
    }
}