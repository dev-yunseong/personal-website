package dev.yunseong.website.blog.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class MemoTest {

    @Test
    public void testCodeBlockWithLanguageClass() {
        // Given: A memo with a fenced code block with language info
        String markdownContent = "```java\npublic class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The code tag should have the language-java class
        assertThat(html).contains("class=\"language-java\"");
        assertThat(html).contains("<code");
        assertThat(html).contains("public class Hello");
    }

    @Test
    public void testCodeBlockWithJavaScriptLanguage() {
        // Given: A memo with a JavaScript code block
        String markdownContent = "```javascript\nconst x = 10;\nconsole.log(x);\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The code tag should have the language-javascript class
        assertThat(html).contains("class=\"language-javascript\"");
        assertThat(html).contains("const x = 10");
    }

    @Test
    public void testCodeBlockWithPythonLanguage() {
        // Given: A memo with a Python code block
        String markdownContent = "```python\ndef hello():\n    print('Hello')\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The code tag should have the language-python class
        assertThat(html).contains("class=\"language-python\"");
        assertThat(html).contains("def hello()");
    }

    @Test
    public void testCodeBlockWithoutLanguage() {
        // Given: A memo with a code block without language info
        String markdownContent = "```\nsome code\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The code tag should still be present (but without language class)
        assertThat(html).contains("<code");
        assertThat(html).contains("some code");
    }

    @Test
    public void testRegularMarkdownContent() {
        // Given: A memo with regular markdown (no code blocks)
        String markdownContent = "# Title\n\nThis is a paragraph with **bold** text.";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: Regular HTML elements should be present
        assertThat(html).contains("<h1>Title</h1>");
        assertThat(html).contains("<strong>bold</strong>");
    }

    @Test
    public void testCodeBlockWithMaliciousLanguageInfo() {
        // Given: A memo with a code block containing potentially malicious language info
        String markdownContent = "```javascript<script>alert('xss')</script>\nconst x = 10;\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The language info should be sanitized (only alphanumeric allowed)
        assertThat(html).contains("class=\"language-javascriptscriptalertxssscript\"");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("const x = 10");
    }

    @Test
    public void testCodeBlockWithSpecialCharactersInLanguageInfo() {
        // Given: A memo with a code block containing special characters in language info
        String markdownContent = "```c++\nint main() { return 0; }\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: Special characters should be removed, keeping only alphanumeric, hyphens, and underscores
        assertThat(html).contains("class=\"language-c\"");
        assertThat(html).contains("int main()");
    }
}
