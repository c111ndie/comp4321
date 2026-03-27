package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import IRUtilities.StopStem;
import java.util.List;
import org.junit.jupiter.api.Test;

public class StopStemTest {
    @Test
    void stopwordsAndStemming_work_fromResourceList() throws Exception {
        StopStem ss = StopStem.fromClasspathResource("/stopwords.txt");
        List<String> out = ss.processTokens(List.of("the", "computing", "computed", "COMPUTATION", "and"));
        assertEquals(List.of("comput", "comput", "comput"), out);
    }
}

