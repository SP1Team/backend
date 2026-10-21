package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.dto.UpdateUserRequest;
import cz.fit.cvut.seatlock.dto.UserResponse;
import cz.fit.cvut.seatlock.repository.UserRepository;
import cz.fit.cvut.seatlock.security.SeatlockUserDetails;
import cz.fit.cvut.seatlock.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class UserResolver {

    private final UserRepository userRepository;
    private final UserService service;

    @QueryMapping
    public UserResponse me(@AuthenticationPrincipal SeatlockUserDetails details){
        return service.me(details.getUsername());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public Boolean approveOrganizer(@Argument UUID userId) {
        return userRepository.updateRoleToOrganization(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @MutationMapping
    public Boolean updateUser(
            @AuthenticationPrincipal SeatlockUserDetails details,
            @Argument @Valid UpdateUserRequest input
    ){
        return service.updateUser(details.getUsername(), input);
    }
}
