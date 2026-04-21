import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class MainTest {

    @BeforeEach
    public void setUp() {
        Map<String, Map<String, Integer>> g = new LinkedHashMap<>();

        Map<String, Integer> toEdges = new LinkedHashMap<>();
        toEdges.put("explore", 1);
        toEdges.put("seek", 1);
        g.put("to", toEdges);

        Map<String, Integer> exploreEdges = new LinkedHashMap<>();
        exploreEdges.put("new", 1);
        exploreEdges.put("strange", 1);
        g.put("explore", exploreEdges);

        Map<String, Integer> seekEdges = new LinkedHashMap<>();
        seekEdges.put("new", 1);
        g.put("seek", seekEdges);

        Map<String, Integer> newEdges = new LinkedHashMap<>();
        newEdges.put("worlds", 1);
        g.put("new", newEdges);

        g.put("strange", new LinkedHashMap<>());
        g.put("worlds", new LinkedHashMap<>());

        Main.setGraphForTest(g);
    }

    // TC1: 브릿지 단어 1개 존재
    @Test
    public void testBridgeWords_oneResult() {
        String result = Main.queryBridgeWords("seek", "worlds");
        assertEquals("The bridge words from \"seek\" to \"worlds\" is: \"new\".", result);
    }

    // TC2: word1, word2 둘 다 그래프에 없음
    @Test
    public void testBridgeWords_bothNotExist() {
        String result = Main.queryBridgeWords("apple", "banana");
        assertEquals("No \"apple\" and \"banana\" in the graph!", result);
    }

    // TC3: word1만 그래프에 없음
    @Test
    public void testBridgeWords_word1NotExist() {
        String result = Main.queryBridgeWords("apple", "new");
        assertEquals("No \"apple\" in the graph!", result);
    }

    // TC4: word2만 그래프에 없음
    @Test
    public void testBridgeWords_word2NotExist() {
        String result = Main.queryBridgeWords("to", "banana");
        assertEquals("No \"banana\" in the graph!", result);
    }

    // TC5: 브릿지 단어 복수 존재
    @Test
    public void testBridgeWords_multipleResults() {
        String result = Main.queryBridgeWords("to", "new");
        assertTrue(result.contains("explore"));
        assertTrue(result.contains("seek"));
        assertTrue(result.startsWith("The bridge words from"));
    }

    // TC6: 브릿지 단어 없음
    @Test
    public void testBridgeWords_noBridge() {
        String result = Main.queryBridgeWords("new", "to");
        assertEquals("No bridge words from \"new\" to \"to\"!", result);
    }

    // TC7: 대문자 입력 처리
    @Test
    public void testBridgeWords_upperCaseInput() {
        String result = Main.queryBridgeWords("SEEK", "WORLDS");
        assertEquals("The bridge words from \"SEEK\" to \"WORLDS\" is: \"new\".", result);
    }
}