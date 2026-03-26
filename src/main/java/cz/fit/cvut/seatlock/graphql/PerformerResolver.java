package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.PerformerDTO;
import cz.fit.cvut.seatlock.service.PerformerService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PerformerResolver {
    private final PerformerService performerService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Performer createPerformer(@Argument PerformerDTO input){
        return performerService.createPerformer(input);
    }
}
