package dev.langchain4j.quarkus.example;

import java.util.Map;

public class TriageResponse {

    private boolean urgent;
    private double urgentProbability;
    private String department;
    private Map<String, Double> departmentProbabilities;
    private double frustrationScore;
    private String modelName;
    private long latencyMs;

    public TriageResponse() {}

    public TriageResponse(boolean urgent, double urgentProbability, String department,
                          Map<String, Double> departmentProbabilities, double frustrationScore,
                          String modelName, long latencyMs) {
        this.urgent = urgent;
        this.urgentProbability = urgentProbability;
        this.department = department;
        this.departmentProbabilities = departmentProbabilities;
        this.frustrationScore = frustrationScore;
        this.modelName = modelName;
        this.latencyMs = latencyMs;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public double getUrgentProbability() {
        return urgentProbability;
    }

    public String getDepartment() {
        return department;
    }

    public Map<String, Double> getDepartmentProbabilities() {
        return departmentProbabilities;
    }

    public double getFrustrationScore() {
        return frustrationScore;
    }

    public String getModelName() {
        return modelName;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
