package dev.yunseong.website.ai.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuggestedQuestionServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private MemoService memoService;

    private final ParameterizedTypeReference<List<String>> stringList = new ParameterizedTypeReference<>() {
    };

    @Test
    void refresh_ShouldReplaceQuestionsWithGeneratedOnes() {
        SuggestedQuestionService service = new SuggestedQuestionService(chatClient, memoService);
        givenRecentMemos("/blog/kubernetes-ingress");
        givenGenerated(List.of("인그레스는 어떻게 구성했나요?", "왜 그 선택을 했나요?", "운영에서 뭐가 어려웠나요?"));

        service.refresh();

        assertThat(service.getQuestions())
                .containsExactly("인그레스는 어떻게 구성했나요?", "왜 그 선택을 했나요?", "운영에서 뭐가 어려웠나요?");
    }

    @Test
    void refresh_ShouldKeepAtMostThreeQuestions() {
        SuggestedQuestionService service = new SuggestedQuestionService(chatClient, memoService);
        givenRecentMemos("/blog/kubernetes-ingress");
        givenGenerated(List.of("첫째?", "둘째?", "셋째?", "넷째?"));

        service.refresh();

        assertThat(service.getQuestions()).containsExactly("첫째?", "둘째?", "셋째?");
    }

    @Test
    void refresh_ShouldKeepFallbackWhenGenerationFails() {
        SuggestedQuestionService service = new SuggestedQuestionService(chatClient, memoService);
        givenRecentMemos("/blog/kubernetes-ingress");
        when(chatClient.prompt().user(anyString()).call().entity(any(ParameterizedTypeReference.class)))
                .thenThrow(new IllegalStateException("no api key"));

        service.refresh();

        assertThat(service.getQuestions()).isEqualTo(SuggestedQuestionService.FALLBACK_QUESTIONS);
    }

    @Test
    void refresh_ShouldKeepFallbackWhenModelReturnsNothing() {
        SuggestedQuestionService service = new SuggestedQuestionService(chatClient, memoService);
        givenRecentMemos("/blog/kubernetes-ingress");
        givenGenerated(List.of());

        service.refresh();

        assertThat(service.getQuestions()).isEqualTo(SuggestedQuestionService.FALLBACK_QUESTIONS);
    }

    @Test
    void refresh_ShouldNotCallTheModelWithoutPublicMemos() {
        SuggestedQuestionService service = new SuggestedQuestionService(chatClient, memoService);
        when(memoService.getRecentMemos(anyInt())).thenReturn(List.of());

        service.refresh();

        verify(chatClient, never()).prompt();
        assertThat(service.getQuestions()).isEqualTo(SuggestedQuestionService.FALLBACK_QUESTIONS);
    }

    private void givenRecentMemos(String... names) {
        List<Memo> memos = java.util.Arrays.stream(names)
                .map(name -> new Memo(1L, name, "content", null, LocalDateTime.now()))
                .toList();
        when(memoService.getRecentMemos(anyInt())).thenReturn(memos);
    }

    private void givenGenerated(List<String> questions) {
        when(chatClient.prompt().user(anyString()).call().entity(any(ParameterizedTypeReference.class)))
                .thenReturn(questions);
    }
}
