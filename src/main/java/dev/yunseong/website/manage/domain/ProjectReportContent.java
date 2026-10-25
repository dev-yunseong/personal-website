package dev.yunseong.website.manage.domain;

import java.util.List;

public record ProjectReportContent(String name, String agent, Status status, String summary,
                                   int staleAfterMinutes, List<Widget> widgets) {
    public enum Status {
        RUNNING("진행 중"), BLOCKED("확인 필요"), COMPLETED("완료"), IDLE("대기");

        private final String label;
        Status(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum WidgetType { METRIC, TABLE, LINE }

    public record Widget(WidgetType type, String title, String value, List<String> columns,
                         List<List<String>> rows, List<Point> points) {}

    public record Point(String label, Double value) {}
}
