package dev.yunseong.website.manage.domain;

import java.util.List;

public record ProjectReportContent(String name, String agent, Status status, String summary,
                                   int staleAfterMinutes, List<Widget> widgets, List<Section> sections,
                                   List<Action> actions) {
    public ProjectReportContent {
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        sections = sections == null ? List.of() : List.copyOf(sections);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public ProjectReportContent(String name, String agent, Status status, String summary,
                                int staleAfterMinutes, List<Widget> widgets) {
        this(name, agent, status, summary, staleAfterMinutes, widgets, List.of(), List.of());
    }
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

    public record Section(String key, String title, List<Widget> widgets, List<Link> links,
                          List<String> actionKeys) {
        public Section {
            widgets = widgets == null ? List.of() : List.copyOf(widgets);
            links = links == null ? List.of() : List.copyOf(links);
            actionKeys = actionKeys == null ? List.of() : List.copyOf(actionKeys);
        }
    }

    public record Link(String label, String url) {}

    public record Action(String key, String label, String description, boolean requiresConfirmation) {}
}
