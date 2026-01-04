package dev.yunseong.website.global.util;

/**
 * Utility class for metadata operations
 */
public class MetadataUtil {

    private static final int CONTENT_PREVIEW_LENGTH = 150;
    private static final String MARKDOWN_CHARS_REGEX = "[#*_`\\[\\]()]";
    private static final String DEFAULT_PREVIEW = "Yunseong의 블로그 포스트입니다.";

    private MetadataUtil() {
        // Utility class
    }

    /**
     * Extract preview text from markdown content
     * @param content the markdown content
     * @return preview text (first 150 characters with markdown removed)
     */
    public static String extractContentPreview(String content) {
        if (content == null || content.isEmpty()) {
            return DEFAULT_PREVIEW;
        }
        
        if (content.length() > CONTENT_PREVIEW_LENGTH) {
            return content.substring(0, CONTENT_PREVIEW_LENGTH)
                    .replaceAll(MARKDOWN_CHARS_REGEX, "") + "...";
        }
        
        return content.replaceAll(MARKDOWN_CHARS_REGEX, "");
    }
}
