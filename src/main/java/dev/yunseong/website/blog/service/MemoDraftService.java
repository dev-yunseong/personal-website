package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class MemoDraftService {
    public static final String PREFIX = "/private/draft";
    public static final int MAX_CONTENT_LENGTH = 100_000;

    private final MemoRepository repository;

    public Memo submit(String target, String content) {
        String name = PREFIX + normalizeTarget(target);
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content must contain 1–100000 characters.");
        }
        Memo draft = repository.findByNameForUpdate(name).orElseGet(() -> new Memo(name, content));
        draft.setContent(content);
        return repository.saveAndFlush(draft);
    }

    public Memo rename(String source, String target) {
        String sourceName = PREFIX + normalizeTarget(source);
        String normalizedTarget = normalizeTarget(target);
        String targetName = PREFIX + normalizedTarget;
        Memo draft = repository.findByNameForUpdate(sourceName).orElseThrow(MemoNotFoundException::new);
        if (sourceName.equals(targetName)) return draft;
        if (repository.findByName(targetName).isPresent() || repository.findByName(normalizedTarget).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "대상 경로에 글 또는 초안이 있습니다.");
        }
        draft.setName(targetName);
        return repository.saveAndFlush(draft);
    }

    @Transactional(readOnly = true)
    public Page<Memo> list(int page) {
        if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page.");
        return repository.findAllByNameStartingWith(PREFIX + "/",
                PageRequest.of(page, 30, Sort.by("updatedAt").descending().and(Sort.by("id"))));
    }

    @Transactional(readOnly = true)
    public Review review(long id) {
        Memo draft = requireDraft(repository.findById(id).orElseThrow(MemoNotFoundException::new));
        String target = targetOf(draft);
        Memo current = repository.findByName(target).orElse(null);
        return new Review(draft, target, current, revision(draft, current),
                MemoLineDiff.compare(current == null ? "" : current.getContent(), draft.getContent()));
    }

    public long approve(long id, String reviewedRevision) {
        Memo draft = requireDraft(repository.findByIdForUpdate(id).orElseThrow(MemoNotFoundException::new));
        String target = targetOf(draft);
        Memo current = repository.findByNameForUpdate(target).orElse(null);
        if (!Objects.equals(reviewedRevision, revision(draft, current))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "초안 또는 원문이 변경되었습니다. 다시 검토하세요.");
        }
        if (current == null) {
            current = new Memo(target, draft.getContent());
        } else {
            current.setContent(draft.getContent());
        }
        // The unique target name protects concurrent inserts; a failure retains the draft.
        repository.saveAndFlush(current);
        repository.delete(draft);
        return current.getId();
    }

    private Memo requireDraft(Memo memo) {
        if (memo.getName() == null || !memo.getName().startsWith(PREFIX + "/")) {
            throw new MemoNotFoundException();
        }
        return memo;
    }

    private String targetOf(Memo draft) {
        return normalizeTarget(draft.getName().substring(PREFIX.length()));
    }

    private String normalizeTarget(String target) {
        if (target == null || target.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target path is required.");
        }
        String normalized = target.startsWith("/") ? target : "/" + target;
        if (normalized.length() + PREFIX.length() > 255 || normalized.startsWith(Memo.PRIVATE_PREFIX)
                || normalized.contains("\\") || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid public target path.");
        }
        for (String segment : normalized.substring(1).split("/", -1)) {
            if (segment.isBlank() || segment.equals(".") || segment.equals("..")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid target path segment.");
            }
        }
        return normalized;
    }

    private String revision(Memo draft, Memo current) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Memo memo : new Memo[]{draft, current}) {
                String value = memo == null ? "absent" : memo.getId() + "\n" + memo.getName()
                        + "\n" + memo.getUpdatedAt() + "\n" + memo.getContent();
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(bytes);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Review(Memo draft, String target, Memo current, String revision,
                         java.util.List<MemoLineDiff.Line> lines) {}
}
