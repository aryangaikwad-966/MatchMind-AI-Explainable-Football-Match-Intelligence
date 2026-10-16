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
}
