package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.PerformerDTO;
import cz.fit.cvut.seatlock.repository.ActivityRepository;
import cz.fit.cvut.seatlock.repository.PerformerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerformerService {
    private final PerformerRepository performerRepository;
    private final ActivityRepository activityRepository;

    public Performer createPerformer(PerformerDTO input) {
        Performer performer = new Performer(input.name(), input.description());
        return performerRepository.save(performer);
    }

    public Boolean updatePerformer(PerformerDTO dto, UUID id) {
        Performer performerToUpdate = performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));

        if(dto.name() != null){
            performerToUpdate.setName(dto.name());
        }

        if(dto.description() != null){
            performerToUpdate.setDescription(dto.description());
        }

        performerRepository.save(performerToUpdate);
        return true;
    }

    public Boolean deletePerformer(UUID id) {
        Performer performerToDelete = performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));
        activityRepository.deletePerformerFromActivities(id);
        performerRepository.delete(performerToDelete);
        return true;
    }

    public List<Performer> getAllPerformers() {
        return performerRepository.findAll();
    }

    public Performer getPerformerById(UUID id) {
        return performerRepository.findById(id).orElseThrow(() -> new RuntimeException("Performer not found"));
    }
}