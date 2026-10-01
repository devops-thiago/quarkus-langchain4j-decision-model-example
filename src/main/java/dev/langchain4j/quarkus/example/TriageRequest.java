package dev.langchain4j.quarkus.example;

public class TriageRequest {

    private String text;

    public TriageRequest() {}

    public TriageRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
