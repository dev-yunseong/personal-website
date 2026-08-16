package dev.yunseong.website.briefing.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.briefing.service.BriefingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Intake for the briefings an external agent generates on a schedule.
 *
 * <p>Everything is {@code text/plain}, in and out. A briefing body <em>is</em> markdown, so
 * wrapping it in JSON would mean the publishing side escapes newlines, quotes and backticks
 * before sending and this side unescapes them — for a payload with exactly one field. Plain
 * text keeps the client at {@code curl --data-binary @-} and keeps the response something a
 * shell script can print.
 *
 * <p>The kind is a path segment and never appears in a registry: {@code POST
 * /api/agent/briefings/jobs} is all it takes for {@code jobs} to start existing.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/agent/briefings",
        produces = AgentBriefingApiController.TEXT_PLAIN_UTF8_VALUE)
@RequiredArgsConstructor
public class AgentBriefingApiController {

    /**
     * Spelt out with the charset because {@code text/plain} alone means ISO-8859-1 to
     * {@link org.springframework.http.converter.StringHttpMessageConverter}, which turns a
     * Korean briefing into question marks on the way out.
     */
    public static final String TEXT_PLAIN_UTF8_VALUE = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8";

    private static final MediaType TEXT_PLAIN_UTF8 =
            new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private final BriefingService briefingService;

    @Value("${app.base-url:}")
    private String baseUrl;

    /**
     * Publishes one briefing, replacing that kind's entry for the same date.
     *
     * @param date optional {@code yyyy-MM-dd}; today when omitted
     * @return the URL of the digest page for that day
     */
    @PostMapping(value = "/{kind}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> publish(
            @PathVariable String kind,
            @RequestParam(required = false) String date,
            HttpEntity<byte[]> request) {
        LocalDate briefingDate = date == null || date.isBlank()
                ? LocalDate.now()
                : BriefingService.parseRequiredDate(date);

        Memo memo = briefingService.publish(kind, briefingDate, decodeBody(request));
        log.info("Published briefing {}", memo.getName());

        return ResponseEntity.ok()
                .contentType(TEXT_PLAIN_UTF8)
                .body(baseUrl + "/briefing?date="
                        + briefingDate.format(BriefingService.DATE_FORMATTER) + "\n");
    }

    /**
     * Takes the body as bytes so the charset is this method's decision rather than
     * {@code StringHttpMessageConverter}'s ISO-8859-1 default: a client that sends UTF-8
     * markdown without spelling the charset out is the common case, not an error.
     * An explicit charset in the request still wins.
     */
    private static String decodeBody(HttpEntity<byte[]> request) {
        byte[] body = request.getBody();
        if (body == null) {
            return "";
        }
        MediaType contentType = request.getHeaders().getContentType();
        Charset charset = contentType == null ? null : contentType.getCharset();
        return new String(body, charset == null ? StandardCharsets.UTF_8 : charset);
    }

    /** One briefing selected by kind and publication date, as stored. */
    @GetMapping("/{kind}/{date}")
    public ResponseEntity<String> historical(
            @PathVariable String kind,
            @PathVariable String date) {
        LocalDate briefingDate = BriefingService.parseRequiredDate(date);
        return briefingService.findByKindAndDate(kind, briefingDate)
                .map(memo -> ResponseEntity.ok(memo.getContent()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(TEXT_PLAIN_UTF8)
                        .body("No briefing published for kind '" + kind
                                + "' on " + briefingDate.format(BriefingService.DATE_FORMATTER) + ".\n"));
    }

    /** The most recent briefing of one kind, as stored. */
    @GetMapping("/{kind}/latest")
    public ResponseEntity<String> latest(@PathVariable String kind) {
        return briefingService.findLatest(kind)
                .map(memo -> ResponseEntity.ok(memo.getContent()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(TEXT_PLAIN_UTF8)
                        .body("No briefing published for kind '" + kind + "'.\n"));
    }

    /**
     * Kinds that have been published recently, one per line.
     *
     * <p>Not a collision with {@code /{kind}/latest}: that pattern needs two segments and
     * this one has a single literal segment.
     */
    @GetMapping("/kinds")
    public ResponseEntity<String> kinds() {
        String kinds = String.join("\n", briefingService.listKinds());
        return ResponseEntity.ok(kinds.isEmpty() ? "" : kinds + "\n");
    }

    /**
     * A rejected kind, date, or body is the caller's mistake, so it reads as 400 with the
     * reason in the body — the agent logs the response verbatim.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException e) {
        log.warn("Rejected briefing request: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .contentType(TEXT_PLAIN_UTF8)
                .body(e.getMessage() + "\n");
    }
}
