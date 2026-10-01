# Quarkus LangChain4j System 1 Decision Model Example

[![Quarkus](https://img.shields.io/badge/Quarkus-3.17+-blue.svg)](https://quarkus.io/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.21+-orange.svg)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![GraalVM](https://img.shields.io/badge/GraalVM-Native%20Ready-purple.svg)](https://www.graalvm.org/)

An end-to-end example demonstrating **System 1 Decision Models** in **Quarkus** using **LangChain4j**.

This repository showcases both:
1. **Local Open-Source Model:** [`devops-thiago/classone-gemma4-e2b`](https://huggingface.co/devops-thiago/classone-gemma4-e2b) (fine-tuned on Gemma 4 with parallel decision heads, running on your GPU/CPU via the `classone` SDK).
2. **Cloud Model:** **TypeSafe Jev** (`jev-latest`) hosted on the TypeSafe AI System One platform.

Both models run in standard **JVM JAR mode** and as an ultra-fast **GraalVM Native Executable** (sub-40ms startup, 12ms inference latency).

---

## What is a System 1 Decision Model?

Traditional Large Language Models (LLMs) operate like Daniel Kahneman's **System 2** (slow, deliberate, autoregressive token-by-token generation).

**System 1 Decision Models** (such as Jev and ClassOne) operate in a single non-autoregressive forward pass to evaluate an input context against typed, bounded questions:

| Primitive | LangChain4j API | Output | Typical Use Case |
|---|---|---|---|
| **Noul** | `YesNoQuestion` | `YesNoAnswer` (`probability 0..1`) | Spam detection, urgency gating, policy compliance |
| **Choice** | `ChoiceQuestion` | `ChoiceAnswer` (winner, distribution, confidence) | Department routing, intent classification |
| **Score** | `ScaleQuestion` | `ScaleAnswer` (mean score, level probabilities) | Customer frustration, risk score, quality grading |

---

## Architecture & CDI Injection

The application uses the `quarkus-langchain4j-typesafe` extension to register multiple `@ApplicationScoped DecisionModel` beans directly into Quarkus's CDI container without manual boilerplate:

```java
@ApplicationScoped
public class DecisionTriageResource {

    @Inject
    DecisionModel localModel; // Default: devops-thiago/classone-gemma4-e2b

    @Inject
    @ModelName("jev")
    DecisionModel jevModel;   // Named: TypeSafe Jev (jev-latest)

    @POST
    @Path("/local")
    public TriageResponse triageLocal(TriageRequest request) {
        DecisionRequest decisionRequest = DecisionRequest.builder()
                .input(request.getText())
                .question("is_urgent", YesNoQuestion.of("Does this ticket require urgent resolution?"))
                .question("department", ChoiceQuestion.builder()
                        .text("Which department should handle this request?")
                        .option("billing", "Payment failures, credit card charges, refunds")
                        .option("support", "Technical bugs, error codes")
                        .option("sales", "Enterprise plan upgrades and contracts")
                        .build())
                .question("customer_frustration", ScaleQuestion.builder()
                        .text("Assess the customer frustration level:")
                        .level("Calm").level("Neutral").level("Frustrated").level("Angry")
                        .build())
                .build();

        DecisionResponse response = localModel.decide(decisionRequest);
        return new TriageResponse(...);
    }
}
```

---

## Quickstart

### 1. Prerequisites
- **Java 17+** (or GraalVM JDK 21/25 for native image compilation)
- **Maven 3.9+**
- **Python 3.10+** (with PyTorch and CUDA for local model execution)

---

### 2. Start the Local ClassOne Inference Server

Serve your local model directly using the official `classone` SDK:

```bash
# Install ClassOne SDK and Uvicorn
pip install classone uvicorn

# Option A: Lightweight standalone mode (instant startup, zero weight download)
CLASSONE_BASE_MODEL=standalone uvicorn classone.server.app:app --port 8000

# Option B: Full GPU acceleration with devops-thiago/classone-gemma4-e2b
CLASSONE_BASE_MODEL="devops-thiago/classone-gemma4-e2b" uvicorn classone.server.app:app --port 8000
```

The server exposes `POST /v1/systemone` on `http://127.0.0.1:8000/`.

---

### 3. Configure Credentials (Optional for Cloud Jev)

To test the cloud **TypeSafe Jev** model alongside your local model, export your `JEV_TOKEN`:

```bash
export JEV_TOKEN="your_typesafe_api_key_here"
```

Or configure it in `src/main/resources/application.properties`:
```properties
quarkus.langchain4j.typesafe.jev.api-key=${JEV_TOKEN}
```

---

### 4. Run in Development Mode

```bash
mvn quarkus:dev
```

Upon startup, `StartupRunner` will automatically execute decision queries against both models and print results to the terminal.

---

### 5. Package and Run as JVM JAR

```bash
# Build the JAR
mvn clean package -DskipTests

# Run the JAR
java -jar target/quarkus-app/quarkus-run.jar
```

---

### 6. Build and Run as GraalVM Native Image

Compile ahead-of-time (AOT) to an ultra-fast native binary:

```bash
# Compile native binary (requires GraalVM or Mandrel)
mvn clean package -Dnative -DskipTests

# Run the native binary
./target/quarkus-langchain4j-decision-model-example-1.0.0-SNAPSHOT-runner
```

**Native Performance:**
- **Startup Time:** ~38 ms
- **Local Decision Latency:** ~12 ms
- **Binary Size:** ~52 MB

---

## Testing the REST Endpoints

Once the application is running (on port `8081`):

### 1. Triage Ticket with Local Model (`classone-gemma4-e2b`)
```bash
curl -X POST http://localhost:8081/triage/local \
  -H "Content-Type: application/json" \
  -d '{"text":"I was charged twice for order #4120 and need a refund immediately!"}'
```

**Response:**
```json
{
  "urgent": false,
  "urgentProbability": 0.3878,
  "department": "billing",
  "departmentProbabilities": {
    "billing": 0.3333,
    "support": 0.3333,
    "sales": 0.3333
  },
  "frustrationScore": 1.5,
  "modelName": "devops-thiago/classone-gemma4-e2b",
  "latencyMs": 12
}
```

### 2. Triage Ticket with Cloud Model (TypeSafe Jev)
```bash
curl -X POST http://localhost:8081/triage/jev \
  -H "Content-Type: application/json" \
  -d '{"text":"I was charged twice for order #4120 and need a refund immediately!"}'
```

**Response:**
```json
{
  "urgent": true,
  "urgentProbability": 0.88,
  "department": "billing",
  "departmentProbabilities": {
    "billing": 0.94,
    "support": 0.06,
    "sales": 0.0
  },
  "frustrationScore": 2.79,
  "modelName": "jev-1.13.0",
  "latencyMs": 348
}
```

---

## Contributing & Upstream Status

- **LangChain4j:** The `DecisionModel` abstraction was introduced in [PR #6469](https://github.com/langchain4j/langchain4j/pull/6469).
- **Quarkus:** Extension support is proposed in [Draft PR #2905](https://github.com/quarkiverse/quarkus-langchain4j/pull/2905).

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
