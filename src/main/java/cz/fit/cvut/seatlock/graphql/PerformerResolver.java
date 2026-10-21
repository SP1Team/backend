package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Performer;
import cz.fit.cvut.seatlock.dto.PerformerPage;
import cz.fit.cvut.seatlock.dto.PerformerRequest;
import cz.fit.cvut.seatlock.service.PerformerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class PerformerResolver {
    private final PerformerService performerService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Boolean createPerformer(@Argument @Valid PerformerRequest input) {
        return performerService.createPerformer(input);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Boolean updatePerformer(@Argument @Valid PerformerRequest input, @Argument UUID id) {
        return performerService.updatePerformer(input, id);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @MutationMapping
    public Boolean deletePerformer(@Argument UUID id){
        return performerService.deletePerformer(id);
    }

    @QueryMapping
    public PerformerPage getAllPerformers(
            @Argument @Min(value = 0, message = "Page number must not be negative") int page,
            @Argument @Min(value = 1, message = "Page size must be greater than zero") int size) {
        return performerService.getAllPerformers(page, size);
    }

    @QueryMapping
    public Performer getPerformerById(@Argument UUID id){
        return performerService.getPerformerById(id);
    }

    @QueryMapping
    public List<Performer> search(@Argument String text){
        return performerService.search(text);
    }
}
