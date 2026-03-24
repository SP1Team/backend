package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.domain.Hall;
import cz.fit.cvut.seatlock.dto.HallDTO;
import cz.fit.cvut.seatlock.service.HallService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class HallResolver {
    private final HallService hallService;

    @MutationMapping
    public Hall createHall(@Argument HallDTO input){
        return hallService.createHall(input);
    }
}
