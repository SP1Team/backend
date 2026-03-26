package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Activity;
import cz.fit.cvut.seatlock.dto.ActivityDTO;
import cz.fit.cvut.seatlock.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ActivityResolver {
    private final ActivityService activityService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Activity createActivity(@Argument ActivityDTO input){
        return activityService.createActivity(input);
    }
}
