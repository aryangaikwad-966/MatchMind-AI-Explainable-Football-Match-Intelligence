package com.matchmind.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasetIntegrityTest {
    @Test
    void canonicalDatasetLoadsWithValidatedCounts() {
        DatasetLoader loader = new DatasetLoader();
        ReflectionTestUtils.setField(loader, "datasetPath", "../dataset");
        loader.loadDataset();
        assertEquals(12, loader.getAllMatches().size());
        assertEquals(1262, loader.getMatchEvents("SYN-M001").size()
                + loader.getMatchEvents("SYN-M002").size()
                + loader.getMatchEvents("SYN-M003").size()
                + loader.getMatchEvents("SYN-M004").size()
                + loader.getMatchEvents("SYN-M005").size()
                + loader.getMatchEvents("SYN-M006").size()
                + loader.getMatchEvents("SYN-M007").size()
                + loader.getMatchEvents("SYN-M008").size()
                + loader.getMatchEvents("SYN-M009").size()
                + loader.getMatchEvents("SYN-M010").size()
                + loader.getMatchEvents("SYN-M011").size()
                + loader.getMatchEvents("SYN-M012").size());
    }
}
