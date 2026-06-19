package dev.yunseong.website.global.domain;

import java.util.List;

public record Profile(
        String name,
        String headline,
        String location,
        String about,
        List<Contact> contacts,
        List<Project> projects,
        List<Award> awards,
        List<Education> education,
        List<Certificate> certificates,
        List<Assessment> assessments
) {

    public Profile {
        contacts = List.copyOf(contacts);
        projects = List.copyOf(projects);
        awards = List.copyOf(awards);
        education = List.copyOf(education);
        certificates = List.copyOf(certificates);
        assessments = List.copyOf(assessments);
    }

    public record Contact(String label, String url) {
    }

    public record Period(String start, String end) {

        public boolean ongoing() {
            return end == null || end.isBlank();
        }
    }

    public record Project(
            String name,
            String url,
            String role,
            Period period,
            List<String> highlights,
            List<String> technologies
    ) {

        public Project {
            highlights = List.copyOf(highlights);
            technologies = List.copyOf(technologies);
        }
    }

    public record Award(
            String name,
            String result,
            String date,
            String organization,
            String url
    ) {
    }

    public record Education(
            String name,
            Period period,
            String description,
            String url
    ) {
    }

    public record Certificate(
            String name,
            String issuedAt,
            String issuer,
            String url
    ) {
    }

    public record Assessment(
            String name,
            String score,
            String level,
            String assessedAt,
            String url
    ) {
    }
}
