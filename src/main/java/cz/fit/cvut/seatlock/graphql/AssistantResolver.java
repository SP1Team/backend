package cz.fit.cvut.seatlock.graphql;

import cz.fit.cvut.seatlock.dto.AssistantAnswer;
import cz.fit.cvut.seatlock.security.SeatlockUserDetails;
import cz.fit.cvut.seatlock.service.AssistantService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@RequiredArgsConstructor
public class AssistantResolver {

    private final AssistantService assistantService;

    @MutationMapping
    public AssistantAnswer askAssistant(
            @Argument @NotBlank(message = "Question must not be blank")
            @Size(max = 2000, message = "Question must not exceed 2000 characters") String question,
            @Argument @NotBlank(message = "Conversation ID must not be blank")
            @Size(max = 64, message = "Conversation ID must not exceed 64 characters") String conversationId,
            @AuthenticationPrincipal SeatlockUserDetails user) {
        return assistantService.ask(question, conversationId, user == null ? null : user.getId());
    }
}
