package com.matchmind.backend.analysis;

import com.matchmind.backend.service.DatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class MatchAnalysisServiceTest {
    private MatchAnalysisService service;
    @BeforeEach void setup(){DatasetLoader loader=new DatasetLoader();ReflectionTestUtils.setField(loader,"datasetPath","../dataset");loader.loadDataset();service=new MatchAnalysisService(loader);}
    @Test void turningPointsReferenceRealEvents(){var points=service.calculateTurningPoints("SYN-M009");assertFalse(points.isEmpty());assertTrue(points.stream().flatMap(p->p.getEventIds().stream()).allMatch(id->id.startsWith("SYN-M009-E")));}
    @Test void statisticsReconstructCanonicalScore(){var stats=service.calculateStatistics("SYN-M011");assertEquals(0,stats.getHomeScore());assertEquals(2,stats.getAwayScore());}
}
