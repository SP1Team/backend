package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Activity;
import cz.fit.cvut.seatlock.dto.ActivityDTO;
import cz.fit.cvut.seatlock.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ActivityResolver {
    private final ActivityService activityService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Activity createActivity(@Argument ActivityDTO input) {
        return activityService.createActivity(input);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Boolean deleteActivity(@Argument UUID id) {
        return activityService.deleteActivity(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Boolean updateActivity(@Argument ActivityDTO input, @Argument UUID id) {
        return activityService.updateActivity(input, id);
    }

    @QueryMapping
    public List<Activity> getAllActivities() {
        return activityService.getAllActivities();
    }

    @QueryMapping
    public Activity getActivityById(@Argument UUID id) {
        return activityService.getActivityById(id);
    }
}
