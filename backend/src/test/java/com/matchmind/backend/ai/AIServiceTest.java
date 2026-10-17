package com.matchmind.backend.ai;

import com.matchmind.backend.analysis.MatchAnalysisService;
import com.matchmind.backend.dto.QuestionRequest;
import com.matchmind.backend.service.DatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AIServiceTest {
    private AIService service;
    @BeforeEach void setup() { DatasetLoader loader=new DatasetLoader(); ReflectionTestUtils.setField(loader,"datasetPath","../dataset"); loader.loadDataset(); service=new AIService(loader,new MatchAnalysisService(loader)); ReflectionTestUtils.setField(service,"aiMode","DEMO"); }
    @Test void homeWinRecapUsesHomeWinLanguage() { assertTrue(service.generateMatchRecap("SYN-M001","FAN").getRecap().contains("Northbridge FC won 3-2")); }
    @Test void awayWinRecapUsesAwayWinLanguage() { assertTrue(service.generateMatchRecap("SYN-M011","FAN").getRecap().contains("Cresswick Rangers won 2-0")); }
    @Test void drawRecapUsesDrawLanguage() { var recap=service.generateMatchRecap("SYN-M004","FAN"); assertTrue(recap.getRecap().contains("ended in a 0-0 draw")); assertTrue(recap.getFanExplanation().contains("draw")); assertTrue(!recap.getFanExplanation().contains("victory")); }
    @Test void deterministicQuestionOnlyCitesRelevantEvidence() { var request=new QuestionRequest(); request.setQuestion("How many fouls were recorded?"); request.setMode("FAN"); var response=service.answerQuestion("SYN-M001",request); assertTrue(response.getEvidenceEventIds().stream().allMatch(id -> id.contains("E") && !id.equals("SYN-M001-E0025"))); }
    @Test void invalidQuestionIsRejected() { var request=new QuestionRequest(); request.setQuestion(" "); request.setMode("FAN"); assertThrows(IllegalArgumentException.class, () -> service.answerQuestion("SYN-M001",request)); }

    @Test void answersWinnerQuestionAccurately() {
        var request = new QuestionRequest();
        request.setQuestion("Who won the match?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Northbridge FC"));
        assertTrue(response.getAnswer().contains("3-2"));
        assertTrue(!response.getEvidenceEventIds().isEmpty());
    }

    @Test void answersSpecificTeamWinQuestion() {
        var request = new QuestionRequest();
        request.setQuestion("Did Northbridge win?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Yes, Northbridge FC won"));
    }

    @Test void answersGoalScorersChronologically() {
        var request = new QuestionRequest();
        request.setQuestion("Who scored the goals in this match?");
        request.setMode("ANALYST");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Goal Registry"));
        assertTrue(response.getAnswer().contains("Kieran Wynovan"));
        assertTrue(response.getEvidenceEventIds().size() == 5); // 5 goals in 3-2 match
    }

    @Test void answersPlayerSpecificQuestionWithEvidence() {
        var request = new QuestionRequest();
        request.setQuestion("Did Kieran Wynovan score?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Kieran Wynovan"));
        assertTrue(response.getAnswer().contains("Scored 1 goal(s)"));
        assertTrue(!response.getEvidenceEventIds().isEmpty());
    }

    @Test void answersTurningPointWithSignificanceRationale() {
        var request = new QuestionRequest();
        request.setQuestion("What was the key turning point?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("turning point was at"));
        assertTrue(response.getAnswer().contains("Why it mattered:"));
        assertTrue(!response.getEvidenceEventIds().isEmpty());
    }

    @Test void answersHalvesProgression() {
        var request = new QuestionRequest();
        request.setQuestion("What happened in the second half?");
        request.setMode("ANALYST");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Match State Progression Breakdown"));
        assertTrue(response.getAnswer().contains("Second Half delta:"));
    }

    @Test void answersRedCardQuestionWhenNoneOccurred() {
        var request = new QuestionRequest();
        request.setQuestion("Were there any red cards?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("No red cards were shown"));
        assertTrue(response.getEvidenceEventIds().isEmpty());
    }

    @Test void answersRedCardQuestionWhenRedCardOccurred() {
        var request = new QuestionRequest();
        request.setQuestion("Was anyone sent off?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M003", request); // SYN-M003 is red card scenario
        assertTrue(response.getAnswer().contains("Red card shown:"));
        assertTrue(!response.getEvidenceEventIds().isEmpty());
    }

    @Test void answersShotAnalyticsInAnalystMode() {
        var request = new QuestionRequest();
        request.setQuestion("How many shots were taken?");
        request.setMode("ANALYST");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Shot Generation & Goalkeeping Analytics"));
        assertTrue(response.getAnswer().contains("on target"));
        assertTrue(response.getAnswer().contains("accuracy"));
    }

    @Test void answersTeamPerformanceWithPreference() {
        var request = new QuestionRequest();
        request.setQuestion("How did the team perform?");
        request.setPreference("Kingsford United");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("Kingsford United scored"));
        assertTrue(response.getAnswer().contains("shot(s)"));
    }

    @Test void providesHelpfulFallbackForUnrelatedQuestions() {
        var request = new QuestionRequest();
        request.setQuestion("What is the weather tomorrow in Paris?");
        request.setMode("FAN");
        var response = service.answerQuestion("SYN-M001", request);
        assertTrue(response.getAnswer().contains("MatchMind AI specializes in match intelligence"));
        assertTrue(response.getAnswer().contains("You can ask about:"));
        assertTrue(response.getEvidenceEventIds().isEmpty());
    }
}
