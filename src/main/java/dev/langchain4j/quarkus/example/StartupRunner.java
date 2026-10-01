package dev.langchain4j.quarkus.example;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.request.ChoiceQuestion;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.request.ScaleQuestion;
import dev.langchain4j.model.decision.request.YesNoQuestion;
import dev.langchain4j.model.decision.response.ChoiceAnswer;
import dev.langchain4j.model.decision.response.DecisionResponse;
import dev.langchain4j.model.decision.response.ScaleAnswer;
import dev.langchain4j.model.decision.response.YesNoAnswer;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class StartupRunner {

    @Inject
    DecisionModel localModel;

    @Inject
    @ModelName("jev")
    DecisionModel jevModel;

    @ConfigProperty(name = "quarkus.langchain4j.typesafe.jev.api-key")
    Optional<String> jevToken;

    void onStart(@Observes StartupEvent ev) {
        System.out.println("\n=================================================================");
        System.out.println(" Quarkus LangChain4j System 1 Decision Model Runner");
        System.out.println(" Using Extension: io.quarkiverse.langchain4j:quarkus-langchain4j-typesafe");
        System.out.println("=================================================================");

        String ticket = "Customer #4120: I was charged twice for order #9821 and the server returned a 500 error!";

        DecisionRequest request = DecisionRequest.builder()
                .input(ticket)
                .question("is_urgent", YesNoQuestion.of("Does this require immediate attention?"))
                .question("department", ChoiceQuestion.builder()
                        .text("Assign ticket to:")
                        .option("billing", "Billing and payment issues")
                        .option("support", "Technical bugs")
                        .option("sales", "Upgrades and leads")
                        .build())
                .question("frustration", ScaleQuestion.builder()
                        .text("Customer frustration:")
                        .level("Calm")
                        .level("Neutral")
                        .level("Frustrated")
                        .level("Angry")
                        .build())
                .build();

        // 1. Run local model (devops-thiago/classone-gemma4-e2b)
        System.out.println("\n[TEST 1] Executing with LOCAL model (devops-thiago/classone-gemma4-e2b)...");
        try {
            long startLocal = System.currentTimeMillis();
            DecisionResponse localResponse = localModel.decide(request);
            long latencyLocal = System.currentTimeMillis() - startLocal;

            YesNoAnswer urgent = localResponse.yesNo("is_urgent");
            ChoiceAnswer dept = localResponse.choice("department");
            ScaleAnswer frustration = localResponse.scale("frustration");

            System.out.println(">> LOCAL DECISION SUCCESS (" + latencyLocal + " ms)");
            System.out.println(" - Model: " + localResponse.modelName());
            System.out.println(" - Is Urgent? P(yes)=" + String.format("%.4f", urgent.probability()) + " -> " + (urgent.probability() > 0.5));
            System.out.println(" - Department: " + dept.value() + " " + dept.probabilities());
            System.out.println(" - Frustration (0-3): " + String.format("%.2f", frustration.mean()));
        } catch (Exception e) {
            System.err.println(">> LOCAL DECISION FAILED: " + e.getMessage());
        }

        // 2. Run cloud model (jev-latest) if token available
        if (jevToken.isPresent() && !jevToken.get().isBlank() && !jevToken.get().equalsIgnoreCase("none")) {
            System.out.println("\n[TEST 2] Executing with CLOUD model (TypeSafe Jev: jev-latest)...");
            try {
                long startJev = System.currentTimeMillis();
                DecisionResponse jevResponse = jevModel.decide(request);
                long latencyJev = System.currentTimeMillis() - startJev;

                YesNoAnswer urgent = jevResponse.yesNo("is_urgent");
                ChoiceAnswer dept = jevResponse.choice("department");
                ScaleAnswer frustration = jevResponse.scale("frustration");

                System.out.println(">> JEV CLOUD DECISION SUCCESS (" + latencyJev + " ms)");
                System.out.println(" - Model: " + jevResponse.modelName());
                System.out.println(" - Is Urgent? P(yes)=" + String.format("%.4f", urgent.probability()) + " -> " + (urgent.probability() > 0.5));
                System.out.println(" - Department: " + dept.value() + " " + dept.probabilities());
                System.out.println(" - Frustration (0-3): " + String.format("%.2f", frustration.mean()));
            } catch (Exception e) {
                System.err.println(">> JEV CLOUD DECISION FAILED: " + e.getMessage());
            }
        } else {
            System.out.println("\n[TEST 2] Skipping JEV cloud test: JEV_TOKEN is not configured.");
        }
        System.out.println("=================================================================\n");
    }
}
