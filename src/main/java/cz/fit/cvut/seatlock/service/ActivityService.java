package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Activity;
import cz.fit.cvut.seatlock.domain.ActivityCategory;
import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.ActivityDTO;
import cz.fit.cvut.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository activityCategoryRepository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;

    public Activity createActivity(ActivityDTO dto) {
        if(activityRepository.findByName(dto.name()).isPresent()){
            throw new RuntimeException("Activity with this name is already exist");
        }
        Hall hall = hallRepository.findById(dto.hallId()).orElseThrow(() -> new RuntimeException("Hall not found"));
        ActivityCategory activityCategory = activityCategoryRepository.findById(dto.categoryId()).orElseThrow(() -> new RuntimeException("Activity category not found"));
        if(!activityRepository.availableTimeForActivity(dto.hallId(), dto.date(), dto.startTime(), dto.endTime())){
            throw new RuntimeException("This time is occupied");
        }
        List<Performer> performers = dto.performer() != null && !dto.performer().isEmpty()
                ? performerRepository.findAllByIds(dto.performer())
                : new ArrayList<>();

        Activity newActivity = new Activity(dto.name(), dto.date(), dto.startTime(), dto.endTime(), dto.description(), activityCategory, hall, performers);
        Activity saved = activityRepository.save(newActivity);

        if (!performers.isEmpty()) {
            activityRepository.savePerformers(saved.getId(), performers);
        }

        return saved;
    }
}