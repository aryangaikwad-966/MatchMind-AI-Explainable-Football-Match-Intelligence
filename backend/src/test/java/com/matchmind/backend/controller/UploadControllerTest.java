package com.matchmind.backend.controller;

import com.matchmind.backend.service.DatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

class UploadControllerTest {
    private UploadController controller;
    private DatasetLoader loader;
    @BeforeEach void setup(){loader=new DatasetLoader();ReflectionTestUtils.setField(loader,"datasetPath","../dataset");loader.loadDataset();controller=new UploadController(loader);}
    private String csv(String homeScore){return "event_id,match_id,event_sequence,minute,second,timestamp_seconds,event_type,description,is_goal,is_disallowed,score_home_after_event,score_away_after_event,home_team_id,away_team_id,home_team_name,away_team_name,team_id,credited_team_id\nU-E1,U1,1,0,0,0,KICK_OFF,Match kicks off,false,false,0,0,UH,UA,Upload Home,Upload Away,,\nU-E2,U1,2,10,0,600,GOAL,Goal,true,false,"+homeScore+",0,UH,UA,Upload Home,Upload Away,UH,\n";}
    @Test void validUploadBecomesAvailable(){var response=controller.uploadMatch(new MockMultipartFile("file","upload.csv","text/csv",csv("1").getBytes()));assertEquals(200,response.getStatusCode().value());assertEquals("U1",response.getBody().get("matchId"));assertTrue((Boolean) response.getBody().get("analysisAvailable"));assertEquals(1,loader.getMatch("U1").getHomeScore());assertEquals(2,loader.getMatchEvents("U1").size());}
    @Test void contradictoryUploadIsRejected(){var response=controller.uploadMatch(new MockMultipartFile("file","upload.csv","text/csv",csv("2").getBytes()));assertEquals(400,response.getStatusCode().value());assertEquals(false,response.getBody().get("success"));}
}
