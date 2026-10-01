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
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/triage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DecisionTriageResource {

    @Inject
    DecisionModel localModel;

    @Inject
    @ModelName("jev")
    DecisionModel jevModel;

    @POST
    public TriageResponse triageLocal(TriageRequest request) {
        return triage(localModel, request);
    }

    @POST
    @Path("/local")
    public TriageResponse triageLocalExplicit(TriageRequest request) {
        return triage(localModel, request);
    }

    @POST
    @Path("/jev")
    public TriageResponse triageJev(TriageRequest request) {
        return triage(jevModel, request);
    }

    private TriageResponse triage(DecisionModel model, TriageRequest request) {
        String input = request != null && request.getText() != null
                ? request.getText()
                : "Customer inquiry without details";

        DecisionRequest decisionRequest = DecisionRequest.builder()
                .input(input)
                .question("is_urgent", YesNoQuestion.of("Does this ticket require urgent resolution?"))
                .question("department", ChoiceQuestion.builder()
                        .text("Which department should handle this request?")
                        .option("billing", "Payment failures, credit card charges, refunds")
                        .option("support", "Technical bugs, error codes")
                        .option("sales", "Enterprise plan upgrades and contracts")
                        .build())
                .question("customer_frustration", ScaleQuestion.builder()
                        .text("Assess the customer frustration level:")
                        .level("Calm")
                        .level("Neutral")
                        .level("Frustrated")
                        .level("Angry")
                        .build())
                .build();

        long start = System.currentTimeMillis();
        DecisionResponse decisionResponse = model.decide(decisionRequest);
        long latency = System.currentTimeMillis() - start;

        YesNoAnswer urgent = decisionResponse.yesNo("is_urgent");
        ChoiceAnswer dept = decisionResponse.choice("department");
        ScaleAnswer frustration = decisionResponse.scale("customer_frustration");

        return new TriageResponse(
                urgent.probability() > 0.5,
                urgent.probability(),
                dept.value(),
                dept.probabilities(),
                frustration.mean(),
                decisionResponse.modelName(),
                latency
        );
    }
}
