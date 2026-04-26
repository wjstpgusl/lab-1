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
    // ========== 백박스 테스트: calcShortestPath ==========

    // P1: src, dst 둘 다 그래프에 없음
    @Test
    public void testShortestPath_bothNotExist() {
        String result = Main.calcShortestPath("apple", "banana");
        assertEquals("No \"apple\" and \"banana\" in the graph!", result);
    }

    // P2: src만 그래프에 없음
    @Test
    public void testShortestPath_srcNotExist() {
        String result = Main.calcShortestPath("apple", "new");
        assertEquals("No \"apple\" in the graph!", result);
    }

    // P3: dst만 그래프에 없음
    @Test
    public void testShortestPath_dstNotExist() {
        String result = Main.calcShortestPath("to", "banana");
        assertEquals("No \"banana\" in the graph!", result);
    }

    // P4: 둘 다 있지만 경로 없음
    @Test
    public void testShortestPath_noPath() {
        String result = Main.calcShortestPath("strange", "worlds");
        assertEquals("No path from \"strange\" to \"worlds\"!", result);
    }


    // P5: 정상 경로 존재
    @Test
    public void testShortestPath_normalPath() {
        List<String> path = Main.calcShortestPathList("to", "worlds");

        // 1. 경로가 존재하는지
        assertNotNull(path);

        // 2. 노드 수 검증 (길이 3 = 노드 4개)
        assertEquals(4, path.size());

        // 3. 출발점/도착점 검증
        assertEquals("to", path.get(0));
        assertEquals("worlds", path.get(path.size() - 1));

        // 4. 유효한 경로인지 검증
        boolean pathA = path.equals(
            Arrays.asList("to", "explore", "new", "worlds"));
        boolean pathB = path.equals(
            Arrays.asList("to", "seek", "new", "worlds"));
        assertTrue(pathA || pathB);
    }
    // ===== 백박스 추가 테스트: calcShortestPath ==========

    // P6: N7=T - 중복 노드가 큐에 들어가는 케이스
    // 그래프: src->A(10), src->B(2), B->A(1)
    // → A가 큐에 A(10), A(3) 두 번 들어감
    // → A(3) 먼저 처리 후 A(10) 뽑힐 때 N7=T 발생
    @Test
    public void testShortestPath_visitedNodeRevisit() {
        Map<String, Map<String, Integer>> g = new LinkedHashMap<>();

        Map<String, Integer> srcEdges = new LinkedHashMap<>();
        srcEdges.put("a", 10);
        srcEdges.put("b", 2);
        g.put("src", srcEdges);

        Map<String, Integer> bEdges = new LinkedHashMap<>();
        bEdges.put("a", 1);
        bEdges.put("dst", 100);
        g.put("b", bEdges);

        g.put("a", new LinkedHashMap<>());
        g.put("dst", new LinkedHashMap<>());

        Main.setGraphForTest(g);

        String result = Main.calcShortestPath("src", "dst");
        assertTrue(result.contains("length = 102"));
    }

    // P8: N10=F - 출도 0인 노드에서 출발 (for 루프 미진입)
    // strange는 출도 0 → for문 진입 못하고 큐 고갈 → 경로 없음
    @Test
    public void testShortestPath_sinkNodeNoPath() {
        Map<String, Map<String, Integer>> g = new LinkedHashMap<>();
        g.put("isolated", new LinkedHashMap<>()); // 출도 0
        g.put("dst", new LinkedHashMap<>());
        Main.setGraphForTest(g);

        String result = Main.calcShortestPath("isolated", "dst");
        assertEquals("No path from \"isolated\" to \"dst\"!", result);
    }

    // P9: N11=F - 더 긴 우회 경로 존재 (거리 갱신 스킵)
    // 그래프: src->dst(1), src->mid(1), mid->dst(5)
    // → src->dst(1) 먼저 처리 후 mid->dst(6) 발견 시 N11=F
    @Test
    public void testShortestPath_distanceUpdateSkipped() {
        Map<String, Map<String, Integer>> g = new LinkedHashMap<>();

        Map<String, Integer> srcEdges = new LinkedHashMap<>();
        srcEdges.put("mid", 1);    // mid가 큐에서 먼저 뽑히도록 최소 가중치 부여
        srcEdges.put("dst", 10);   // dst는 나중에 뽑히도록 높은 가중치 부여
        g.put("src", srcEdges);

        Map<String, Integer> midEdges = new LinkedHashMap<>();
        midEdges.put("dst", 15);   // mid를 거쳐가는 거리가 기존 dst 거리(10)보다 크도록 설정
        g.put("mid", midEdges);

        g.put("dst", new LinkedHashMap<>());

        Main.setGraphForTest(g);

        String result = Main.calcShortestPath("src", "dst");
        // 최단 경로: src->dst (거리 1), mid->dst(거리 6)는 스킵
        assertTrue(result.contains("src -> dst"));
        assertTrue(result.contains("length = 1"));
    }

    // P11: N8=T 즉시 - src==dst
    // → 첫 poll에서 바로 u.equals(dst)=T → break → dist[dst]=0
    @Test
    public void testShortestPath_srcEqualsDst() {
        String result = Main.calcShortestPath("to", "to");
        assertTrue(result.contains("length = 0"));
    }
    // P_enableGraphImage: enableGraphImage=F 분기 커버
    @Test
    public void testShortestPath_disableGraphImage() {
        Main.enableGraphImage = false;
        String result = Main.calcShortestPath("to", "worlds");
        assertTrue(result.contains("Shortest path"));
        Main.enableGraphImage = true; // 테스트 후 복원
    }
}