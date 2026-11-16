package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.dto.AssistantAnswer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssistantServiceTest {

    @Test
    void reportsMissingConfigurationInsteadOfFailing() {
        AssistantService service = new AssistantService(
                providerWithoutChatClient(), mock(AssistantTools.class), "");

        AssistantAnswer answer = service.ask("Ahoj, co se hraje v Praze?", "conversation-1", null);

        assertThat(answer.conversationId()).isEqualTo("conversation-1");
        assertThat(answer.answer()).contains("not switched on");
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatClient.Builder> providerWithoutChatClient() {
        ObjectProvider<ChatClient.Builder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
