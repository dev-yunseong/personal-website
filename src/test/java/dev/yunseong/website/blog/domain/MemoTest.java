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
    public void testMarkdownTableRendersAsTable() {
        // Given: A memo with a GFM table written in Korean
        String markdownContent = """
                | 항목 | 설명 |
                | --- | --- |
                | 배포 | 매주 화요일 |
                | 롤백 | 즉시 가능 |
                """;
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: A real table should be emitted with header and body cells
        assertThat(html).contains("<table>");
        assertThat(html).contains("<thead>");
        assertThat(html).contains("<th>항목</th>");
        assertThat(html).contains("<th>설명</th>");
        assertThat(html).contains("<tbody>");
        assertThat(html).contains("<td>배포</td>");
        assertThat(html).contains("<td>매주 화요일</td>");
        assertThat(html).contains("<td>롤백</td>");
        assertThat(html).contains("<td>즉시 가능</td>");
        assertThat(html).doesNotContain("<p>| 항목");
    }

    @Test
    public void testTableCellsKeepInlineMarkdown() {
        // Given: A memo whose table cells contain inline markdown
        String markdownContent = "| name | value |\n| --- | --- |\n| **bold** | `code` |";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: Inline markdown inside the cells should still be rendered
        assertThat(html).contains("<td><strong>bold</strong></td>");
        assertThat(html).contains("<td><code>code</code></td>");
    }

    @Test
    public void testContentWithoutTableIsUnaffected() {
        // Given: A memo with prose that uses pipes but is not a table
        String markdownContent = "# 제목\n\n로그는 `grep` | `sort` 순서로 흘러간다.\n\n- 첫째 | 둘째\n";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The pipes stay literal text and no table is produced
        assertThat(html).doesNotContain("<table>");
        assertThat(html).contains("<h1>제목</h1>");
        assertThat(html).contains("<li>첫째 | 둘째</li>");
    }

    @Test
    public void testPipesInInlineCodeDoNotBecomeTable() {
        // Given: A memo with a shell pipeline inside inline code
        String markdownContent = "명령은 `ps aux | grep java` 로 확인한다.";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The pipe should remain inside the code span
        assertThat(html).doesNotContain("<table>");
        assertThat(html).contains("<code>ps aux | grep java</code>");
    }

    @Test
    public void testPipesInFencedCodeBlockDoNotBecomeTable() {
        // Given: A memo with table-looking lines inside a fenced code block
        String markdownContent = "```sql\nSELECT id | flag FROM t;\n| id | name |\n| --- | --- |\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: The block stays code, keeps its language class, and is not a table
        assertThat(html).doesNotContain("<table>");
        assertThat(html).contains("class=\"language-sql\"");
        assertThat(html).contains("SELECT id | flag FROM t;");
        assertThat(html).contains("| --- | --- |");
    }

    @Test
    public void testCodeBlockKeepsLanguageClassAlongsideTable() {
        // Given: A memo that mixes a table and a fenced code block
        String markdownContent = "| 단계 | 명령 |\n| --- | --- |\n| 빌드 | gradle |\n\n```java\nint x = 1;\n```";
        Memo memo = new Memo("test", markdownContent);

        // When: Converting to HTML
        String html = memo.getHtml();

        // Then: Both the table and the language class should be present
        assertThat(html).contains("<th>단계</th>");
        assertThat(html).contains("<td>빌드</td>");
        assertThat(html).contains("class=\"language-java\"");
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
