package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.Activity;
import cz.fit.cvut.seatlock.domain.ActivityCategory;
import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.ActivityDTO;
import cz.fit.cvut.seatlock.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository activityCategoryRepository;
    private final HallRepository hallRepository;
    private final PerformerRepository performerRepository;

    public Activity createActivity(ActivityDTO dto) {
        if (activityRepository.findByName(dto.name()).isPresent()) {
            throw new RuntimeException("Activity with this name already exists");
        }

        Hall hall = hallRepository.findById(dto.hallId()).orElseThrow(() -> new RuntimeException("Hall not found"));

        ActivityCategory activityCategory = activityCategoryRepository.findById(dto.categoryId()).orElseThrow(() -> new RuntimeException("Activity category not found"));

        if (!activityRepository.availableTimeForActivity(dto.hallId(), dto.date(), dto.startTime(), dto.endTime(), null)) {
            throw new RuntimeException("Hall is not available at this time");
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

    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    public Activity getActivityById(UUID id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    public Boolean deleteActivity(UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        activityRepository.deletePerformers(id);
        activityRepository.delete(activity);
        return true;
    }

    public Boolean updateActivity(ActivityDTO dto, UUID id) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        if (dto.name() != null) {
            activityRepository.findByName(dto.name()).ifPresent(found -> {
                if (!found.getId().equals(id)) {
                    throw new RuntimeException("Activity with this name already exists");
                }
            });
        }

        UUID hallId = dto.hallId() != null ? dto.hallId() : activity.getHall().getId();
        LocalDate date = dto.date() != null ? dto.date() : activity.getDate();
        LocalTime startTime = dto.startTime() != null ? dto.startTime() : activity.getStartTime();
        LocalTime endTime = dto.endTime() != null ? dto.endTime() : activity.getEndTime();

        if (!activityRepository.availableTimeForActivity(hallId, date, startTime, endTime, id)) {
            throw new RuntimeException("Hall is not available at this time");
        }

        if (dto.name() != null) {
            activity.setName(dto.name());
        }
        if (dto.date() != null) {
            activity.setDate(dto.date());
        }
        if (dto.startTime() != null) {
            activity.setStartTime(dto.startTime());
        }
        if (dto.endTime() != null) {
            activity.setEndTime(dto.endTime());
        }
        if (dto.description() != null) {
            activity.setDescription(dto.description());
        }
        if (dto.categoryId() != null) {
            ActivityCategory category = activityCategoryRepository.findById(dto.categoryId()).orElseThrow(() -> new RuntimeException("Category not found"));
            activity.setCategory(category);
        }
        if (dto.hallId() != null) {
            Hall hall = hallRepository.findById(dto.hallId()).orElseThrow(() -> new RuntimeException("Hall not found"));
            activity.setHall(hall);
        }

        if (dto.performer() != null) {
            activityRepository.deletePerformers(id);
            if (!dto.performer().isEmpty()) {
                List<Performer> performers = performerRepository.findAllByIds(dto.performer());
                activityRepository.savePerformers(id, performers);
            }
        }

        activityRepository.save(activity);
        return true;
    }
}
