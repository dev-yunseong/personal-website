package dev.yunseong.website.ai.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Keeps the curator's example questions in step with what the archive actually holds.
 * The questions are generated once a day from recent public memo titles and served
 * from memory, so a page view never waits on the model.
 */
@Slf4j
@Service
public class SuggestedQuestionService {

    static final List<String> FALLBACK_QUESTIONS = List.of(
            "어떤 기술 스택으로 일해 왔나요?",
            "최근에 쓴 글 중 인프라 관련 내용을 요약해 주세요.",
            "이 사이트는 어떤 구조로 만들어졌나요?"
    );

    private static final int TITLE_SAMPLE_SIZE = 20;
    private static final int QUESTION_COUNT = 3;

    private static final String PROMPT = """
            아래는 개인 블로그의 최근 공개 글 제목 목록이다.
            방문자가 이 블로그의 AI 큐레이터에게 던질 만한 한국어 질문 %d개를 만들어라.

            규칙:
            - 목록에 실제로 있는 주제만 다룬다. 없는 내용을 지어내지 않는다.
            - 제목을 그대로 옮기지 말고, 사람이 물어보듯 문장으로 쓴다.
            - 각 질문은 40자 이내이며 물음표로 끝난다.
            - 서로 다른 주제를 고른다.

            제목 목록:
            %s
            """;

    private final ChatClient chatClient;
    private final MemoService memoService;

    private volatile List<String> questions = FALLBACK_QUESTIONS;

    public SuggestedQuestionService(ChatClient chatClient, MemoService memoService) {
        this.chatClient = chatClient;
        this.memoService = memoService;
    }

    public List<String> getQuestions() {
        return questions;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    public void refresh() {
        List<String> titles = memoService.getRecentMemos(TITLE_SAMPLE_SIZE).stream()
                .map(Memo::getTitle)
                .toList();

        if (titles.isEmpty()) {
            log.info("[Suggested Questions] No public memos to derive questions from, keeping current questions.");
            return;
        }

        try {
            List<String> generated = chatClient.prompt()
                    .user(PROMPT.formatted(QUESTION_COUNT, String.join("\n", titles)))
                    .call()
                    .entity(new ParameterizedTypeReference<List<String>>() {
                    });

            if (generated == null || generated.isEmpty()) {
                log.warn("[Suggested Questions] Model returned no questions, keeping current questions.");
                return;
            }

            questions = List.copyOf(generated.subList(0, Math.min(QUESTION_COUNT, generated.size())));
            log.info("[Suggested Questions] Refreshed from {} memo titles.", titles.size());
        } catch (Exception e) {
            log.warn("[Suggested Questions] Refresh failed, keeping current questions.", e);
        }
    }
}
