import java.io.*;
import java.util.*;


/**
 * Lab1: 基于大模型的编程与Git实战
 * 有向词图（Directed Word Graph）程序
 * 
 * 功能：
 * 1. 读入文本文件生成有向图
 * 2. 展示有向图
 * 3. 查询桥接词
 * 4. 根据桥接词生成新文本
 * 5. 计算最短路径（Dijkstra）
 * 6. 计算PageRank
 * 7. 随机游走
 */

// diff

// B2

//commit
public class Main {

    // 图的邻接表表示: word -> {neighbor -> weight}
    private static Map<String, Map<String, Integer>> graph = new LinkedHashMap<>();
    private static Random random = new Random();
    private static boolean enableGraphImage = true;

    // ==================== 主程序 ====================
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String filePath;

        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.print("请输入文本文件路径: ");
            filePath = scanner.nextLine().trim();
        }

        // 读入文件并构建图
        if (!buildGraph(filePath)) {
            System.out.println("文件读取失败，程序退出。");
            return;
        }
        System.out.println("图构建成功！节点数: " + graph.size());

        // 主菜单循环
        while (true) {
            System.out.println("\n========== 功能菜单 ==========");
            System.out.println("1. 展示有向图");
            System.out.println("2. 查询桥接词");
            System.out.println("3. 生成新文本");
            System.out.println("4. 计算最短路径");
            System.out.println("5. 计算PageRank");
            System.out.println("6. 随机游走");
            System.out.println("0. 退出");
            System.out.print("请选择功能: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    showDirectedGraph(graph);
                    break;
                case "2":
                    System.out.print("输入word1: ");
                    String w1 = scanner.nextLine().trim();
                    System.out.print("输入word2: ");
                    String w2 = scanner.nextLine().trim();
                    System.out.println(queryBridgeWords(w1, w2));
                    break;
                case "3":
                    System.out.print("输入新文本: ");
                    String inputText = scanner.nextLine().trim();
                    System.out.println("生成结果: " + generateNewText(inputText));
                    break;
                case "4":
                    System.out.print("输入word1: ");
                    String sw1 = scanner.nextLine().trim();
                    System.out.print("输入word2 (可留空，计算到所有词的最短路径): ");
                    String sw2 = scanner.nextLine().trim();
                    if (sw2.isEmpty()) {
                        String src = sw1.toLowerCase();
                        if (!graph.containsKey(src)) {
                            System.out.println("No \"" + sw1 + "\" in the graph!");
                        } else {
                            enableGraphImage = false;
                            for (String target : graph.keySet()) {
                                if (!target.equals(src)) {
                                    System.out.println(calcShortestPath(sw1, target));
                                }
                            }
                            enableGraphImage = true;
                        }
                    } else {
                        System.out.println(calcShortestPath(sw1, sw2));
                    }
                    break;
                case "5":
                    System.out.print("输入单词 (输入all计算所有节点): ");
                    String prWord = scanner.nextLine().trim();
                    if (prWord.equalsIgnoreCase("all")) {
                        // 计算所有节点的PR值并排序输出
                        Map<String, Double> allPR = new HashMap<>();
                        for (String node : graph.keySet()) {
                            allPR.put(node, calPageRank(node));
                        }
                        allPR.entrySet().stream()
                            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                            .forEach(e -> System.out.printf("  %-20s PR = %.6f%n", e.getKey(), e.getValue()));
                    } else {
                        Double pr = calPageRank(prWord);
                        if (pr != null) {
                            System.out.printf("PageRank(\"%s\") = %.6f%n", prWord.toLowerCase(), pr);
                        } else {
                            System.out.println("No \"" + prWord + "\" in the graph!");
                        }
                    }
                    break;
                case "6":
                    System.out.println("随机游走开始 (按Enter继续, 输入q停止):");
                    String startNode = Main.randomWalkInit();
                    if (startNode == null) {
                        System.out.println("图为空, 无法游走。");
                    } else {
                        System.out.print(startNode);
                        while (!Main.isWalkFinished()) {
                            System.out.print("  [Enter继续/q停止]: ");
                            String cmd = scanner.nextLine().trim();
                            if (cmd.equalsIgnoreCase("q")) {
                                System.out.println("(用户停止游走)");
                                break;
                            }
                            String nextNode = Main.randomWalkStep();
                            if (nextNode != null) {
                                System.out.print(" " + nextNode);
                            }
                            if (Main.isWalkFinished()) {
                                System.out.println("\n(" + Main.getWalkStopReason() + ")");
                            }
                        }
                        String walkResult = Main.randomWalk();
                        System.out.println("游走结果: " + walkResult);
                        System.out.println("结果已保存到 random_walk.txt");
                    }
                    break;
                case "0":
                    System.out.println("程序退出。");
                    scanner.close();
                    return;
                default:
                    System.out.println("无效选择，请重新输入。");
            }
        }
    }

    // ==================== 构建图 ====================
    private static boolean buildGraph(String filePath) {
        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(" ");
            }
            reader.close();

            // 处理文本：替换非字母为空格，转小写
            String text = sb.toString().toLowerCase();
            StringBuilder cleaned = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c >= 'a' && c <= 'z') {
                    cleaned.append(c);
                } else {
                    cleaned.append(' ');
                }
            }

            // 分词
            String[] words = cleaned.toString().trim().split("\\s+");
            if (words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
                System.out.println("文本为空！");
                return false;
            }

            // 构建有向图
            graph.clear();
            for (String w : words) {
                if (!w.isEmpty()) {
                    graph.putIfAbsent(w, new LinkedHashMap<>());
                }
            }

            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].isEmpty() || words[i + 1].isEmpty()) continue;
                String from = words[i];
                String to = words[i + 1];
                graph.get(from).merge(to, 1, Integer::sum);
            }

            return true;
        } catch (IOException e) {
            System.out.println("读取文件出错: " + e.getMessage());
            return false;
        }
    }

    // ==================== 功能1：展示有向图 ====================
    /**
     * 在命令行展示有向图，同时生成Graphviz DOT文件和PNG图片
     */
    public static void showDirectedGraph(Map<String, Map<String, Integer>> G) {
        // CLI展示部分 - 构建展示字符串
        StringBuilder display = new StringBuilder();
        display.append("\n===== 有向图 =====\n");
        for (Map.Entry<String, Map<String, Integer>> entry : G.entrySet()) {
            String from = entry.getKey();
            Map<String, Integer> edges = entry.getValue();
            for (Map.Entry<String, Integer> edge : edges.entrySet()) {
                display.append(String.format("  %s -> %s [weight=%d]%n", from, edge.getKey(), edge.getValue()));
            }
        }
        display.append("  无出边的节点: ");
        List<String> sinks = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : G.entrySet()) {
            if (entry.getValue().isEmpty()) sinks.add(entry.getKey());
        }
        display.append(sinks.isEmpty() ? "(无)" : String.join(", ", sinks));
        System.out.println(display);

        // 生成Graphviz DOT文件（网络图样式）
        try {
            StringBuilder dot = new StringBuilder();
            dot.append("digraph WordGraph {\n");
            dot.append("  layout=neato;\n");
            dot.append("  overlap=false;\n");
            dot.append("  splines=true;\n");
            dot.append("  sep=\"+5\";\n");
            dot.append("  node [shape=circle, style=filled, fillcolor=\"#FFB6C1\",\n");
            dot.append("        fontsize=11, fontname=\"Arial\", width=0.6, fixedsize=false];\n");
            dot.append("  edge [fontsize=9, fontname=\"Arial\", color=\"#2E8B57\",\n");
            dot.append("        fontcolor=\"#333333\", arrowsize=0.7];\n");
            for (Map.Entry<String, Map<String, Integer>> entry : G.entrySet()) {
                String from = entry.getKey();
                for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                    int w = edge.getValue();
                    // 边越粗表示权重越大
                    double penwidth = 1.0 + (w - 1) * 0.8;
                    dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\", penwidth=%.1f];\n",
                        from, edge.getKey(), w, penwidth));
                }
            }
            dot.append("}\n");

            // 写DOT文件
            String dotFile = "graph.dot";
            String pngFile = "graph.png";
            BufferedWriter writer = new BufferedWriter(new FileWriter(dotFile));
            writer.write(dot.toString());
            writer.close();
            System.out.println("DOT文件已保存为 " + dotFile);

            // 调用Graphviz生成PNG（如果已安装）
            runDot(dotFile, pngFile);
        } catch (Exception e) {
            System.out.println("生成图形文件时出错: " + e.getMessage());
        }
    }

    /**
     * 调用Graphviz命令生成PNG图片
     * 自动检测DOT文件中的layout引擎（neato/dot）
     * 如果Graphviz未安装则给出提示，不抛出异常
     */
    private static void runDot(String dotFile, String pngFile) {
        // 检测DOT文件中指定的layout引擎
        String engine = "dot";
        try (BufferedReader check = new BufferedReader(new FileReader(dotFile))) {
            String line;
            while ((line = check.readLine()) != null) {
                if (line.contains("layout=neato")) { engine = "neato"; break; }
                if (line.contains("layout=fdp")) { engine = "fdp"; break; }
                if (line.contains("layout=sfdp")) { engine = "sfdp"; break; }
                if (line.contains("layout=circo")) { engine = "circo"; break; }
            }
        } catch (IOException ignored) {}

        try {
            ProcessBuilder pb = new ProcessBuilder(engine, "-Tpng", dotFile, "-o", pngFile);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (br.readLine() != null) { /* consume */ }
            }
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                System.out.println("图片已保存为 " + pngFile);
            } else {
                System.out.println("Graphviz生成图片失败(exit=" + exitCode + ")，DOT文件可用其他工具打开。");
            }
        } catch (IOException e) {
            System.out.println("[提示] Graphviz未安装，无法自动生成PNG图片。");
            System.out.println("  请安装Graphviz: https://graphviz.org/download/");
            System.out.println("  安装后可手动执行: " + engine + " -Tpng " + dotFile + " -o " + pngFile);
            System.out.println("  或将DOT文件内容粘贴到在线工具: https://dreampuf.github.io/GraphvizOnline/");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 功能2：查询桥接词 ====================
    /**
     * 查询word1和word2之间的桥接词
     * 桥接词word3: 存在 word1->word3 和 word3->word2 的边
     */
    public static String queryBridgeWords(String word1, String word2) {
        String w1 = word1.toLowerCase();
        String w2 = word2.toLowerCase();

        boolean w1Exists = graph.containsKey(w1);
        boolean w2Exists = graph.containsKey(w2);

        if (!w1Exists && !w2Exists) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        }
        if (!w1Exists) {
            return "No \"" + word1 + "\" in the graph!";
        }
        if (!w2Exists) {
            return "No \"" + word2 + "\" in the graph!";
        }

        // 查找桥接词
        List<String> bridgeWords = new ArrayList<>();
        Map<String, Integer> w1Neighbors = graph.get(w1);
        if (w1Neighbors != null) {
            for (String mid : w1Neighbors.keySet()) {
                Map<String, Integer> midNeighbors = graph.get(mid);
                if (midNeighbors != null && midNeighbors.containsKey(w2)) {
                    bridgeWords.add(mid);
                }
            }
        }

        if (bridgeWords.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        if (bridgeWords.size() == 1) {
            return "The bridge words from \"" + word1 + "\" to \"" + word2 + "\" is: \"" + bridgeWords.get(0) + "\".";
        }

        // 多个桥接词
        StringBuilder sb = new StringBuilder();
        sb.append("The bridge words from \"").append(word1).append("\" to \"").append(word2).append("\" are: ");
        for (int i = 0; i < bridgeWords.size(); i++) {
            if (i == bridgeWords.size() - 1) {
                sb.append("and \"").append(bridgeWords.get(i)).append("\".");
            } else {
                sb.append("\"").append(bridgeWords.get(i)).append("\", ");
            }
        }
        return sb.toString();
    }

    // ==================== 功能3：根据桥接词生成新文本 ====================
    /**
     * 在输入文本的相邻词对之间插入桥接词
     */
    public static String generateNewText(String inputText) {
        // 提取单词（保留原始大小写用于输出）
        String[] words = inputText.split("\\s+");
        if (words.length <= 1) {
            return inputText;
        }

        StringBuilder result = new StringBuilder();
        result.append(words[0]);

        for (int i = 0; i < words.length - 1; i++) {
            String w1 = words[i].toLowerCase().replaceAll("[^a-z]", "");
            String w2 = words[i + 1].toLowerCase().replaceAll("[^a-z]", "");

            // 查找桥接词
            List<String> bridges = getBridgeWordsList(w1, w2);
            if (!bridges.isEmpty()) {
                // 随机选一个
                String bridge = bridges.get(random.nextInt(bridges.size()));
                result.append(" ").append(bridge);
            }
            result.append(" ").append(words[i + 1]);
        }

        return result.toString();
    }

    /**
     * 获取桥接词列表（辅助函数）
     */
    private static List<String> getBridgeWordsList(String w1, String w2) {
        List<String> bridgeWords = new ArrayList<>();
        if (!graph.containsKey(w1) || !graph.containsKey(w2)) {
            return bridgeWords;
        }
        Map<String, Integer> w1Neighbors = graph.get(w1);
        if (w1Neighbors != null) {
            for (String mid : w1Neighbors.keySet()) {
                Map<String, Integer> midNeighbors = graph.get(mid);
                if (midNeighbors != null && midNeighbors.containsKey(w2)) {
                    bridgeWords.add(mid);
                }
            }
        }
        return bridgeWords;
    }

    // ==================== 功能4：最短路径（Dijkstra） ====================
    /**
     * 计算word1到word2的最短路径
     * 返回路径描述和长度
     */
    public static String calcShortestPath(String word1, String word2) {
        String src = word1.toLowerCase();
        String dst = word2.toLowerCase();

        if (!graph.containsKey(src) && !graph.containsKey(dst)) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        }
        if (!graph.containsKey(src)) {
            return "No \"" + word1 + "\" in the graph!";
        }
        if (!graph.containsKey(dst)) {
            return "No \"" + word2 + "\" in the graph!";
        }

        // Dijkstra
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<String[]> pq = new PriorityQueue<>((a, b) -> 
            Integer.parseInt(a[1]) - Integer.parseInt(b[1]));

        for (String node : graph.keySet()) {
            dist.put(node, Integer.MAX_VALUE);
        }
        dist.put(src, 0);
        pq.offer(new String[]{src, "0"});

        while (!pq.isEmpty()) {
            String[] curr = pq.poll();
            String u = curr[0];
            int d = Integer.parseInt(curr[1]);

            if (visited.contains(u)) continue;
            visited.add(u);

            if (u.equals(dst)) break;

            Map<String, Integer> neighbors = graph.get(u);
            if (neighbors == null) continue;
            for (Map.Entry<String, Integer> edge : neighbors.entrySet()) {
                String v = edge.getKey();
                int weight = edge.getValue();
                int newDist = d + weight;
                if (newDist < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.offer(new String[]{v, String.valueOf(newDist)});
                }
            }
        }

        if (dist.getOrDefault(dst, Integer.MAX_VALUE) == Integer.MAX_VALUE) {
            return "No path from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        // 回溯路径
        List<String> path = new ArrayList<>();
        String current = dst;
        while (current != null) {
            path.add(current);
            current = prev.get(current);
        }
        Collections.reverse(path);

        int length = dist.get(dst);
        String pathStr = String.join(" -> ", path);

        // 生成高亮图
        if (enableGraphImage) {
            generatePathGraph(path);
        }
        return "Shortest path: " + pathStr + " (length = " + length + ")";
    }

    /**
     * 生成带有高亮路径的Graphviz图片
     */
    private static void generatePathGraph(List<String> path) {
        try {
            Set<String> pathNodes = new HashSet<>(path);
            Set<String> pathEdges = new HashSet<>();
            for (int i = 0; i < path.size() - 1; i++) {
                pathEdges.add(path.get(i) + "->" + path.get(i + 1));
            }

            StringBuilder dot = new StringBuilder();
            dot.append("digraph WordGraph {\n");
            dot.append("  layout=neato;\n");
            dot.append("  overlap=false;\n");
            dot.append("  splines=true;\n");
            dot.append("  sep=\"+5\";\n");
            dot.append("  node [shape=circle, style=filled, fillcolor=\"#FFB6C1\",\n");
            dot.append("        fontsize=11, fontname=\"Arial\", width=0.6, fixedsize=false];\n");
            dot.append("  edge [fontsize=9, fontname=\"Arial\", color=\"#2E8B57\",\n");
            dot.append("        fontcolor=\"#333333\", arrowsize=0.7];\n");

            // 高亮路径节点（黄色）
            for (String node : pathNodes) {
                dot.append(String.format("  \"%s\" [fillcolor=\"#FFD700\"];\n", node));
            }

            for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
                String from = entry.getKey();
                for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                    String edgeKey = from + "->" + edge.getKey();
                    int w = edge.getValue();
                    if (pathEdges.contains(edgeKey)) {
                        dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\", color=\"#FF0000\", penwidth=2.5, fontcolor=\"#FF0000\"];\n",
                            from, edge.getKey(), w));
                    } else {
                        double penwidth = 1.0 + (w - 1) * 0.8;
                        dot.append(String.format("  \"%s\" -> \"%s\" [label=\"%d\", penwidth=%.1f];\n",
                            from, edge.getKey(), w, penwidth));
                    }
                }
            }
            dot.append("}\n");

            String dotFile = "shortest_path.dot";
            String pngFile = "shortest_path.png";
            BufferedWriter writer = new BufferedWriter(new FileWriter(dotFile));
            writer.write(dot.toString());
            writer.close();
            System.out.println("最短路径DOT文件已保存为 " + dotFile);

            runDot(dotFile, pngFile);
        } catch (Exception e) {
            System.out.println("生成路径图时出错: " + e.getMessage());
        }
    }

    // ==================== 功能5：PageRank ====================
    /**
     * 计算指定单词的PageRank值
     * d = 0.85, 迭代直到收敛
     */
    public static Double calPageRank(String word) {
        String w = word.toLowerCase();
        if (!graph.containsKey(w)) {
            return null;
        }

        double d = 0.85;
        int N = graph.size();
        int maxIter = 200;
        double epsilon = 1e-8;

        // 初始化PR值
        Map<String, Double> pr = new HashMap<>();
        for (String node : graph.keySet()) {
            pr.put(node, 1.0 / N);
        }

        // 预计算每个节点的出度
        Map<String, Integer> outDegree = new HashMap<>();
        List<String> danglingNodes = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            int deg = entry.getValue().size();
            outDegree.put(entry.getKey(), deg);
            if (deg == 0) {
                danglingNodes.add(entry.getKey());
            }
        }

        // 预计算每个节点的入边
        Map<String, List<String>> inEdges = new HashMap<>();
        for (String node : graph.keySet()) {
            inEdges.put(node, new ArrayList<>());
        }
        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            String from = entry.getKey();
            for (String to : entry.getValue().keySet()) {
                inEdges.get(to).add(from);
            }
        }

        // 迭代计算
        for (int iter = 0; iter < maxIter; iter++) {
            Map<String, Double> newPR = new HashMap<>();

            // 计算dangling节点的PR总和
            double danglingSum = 0;
            for (String dn : danglingNodes) {
                danglingSum += pr.get(dn);
            }

            double maxDiff = 0;
            for (String node : graph.keySet()) {
                double sum = 0;
                for (String inNode : inEdges.get(node)) {
                    sum += pr.get(inNode) / outDegree.get(inNode);
                }
                // 加上dangling节点均分的贡献
                double newVal = (1.0 - d) / N + d * (sum + danglingSum / N);
                newPR.put(node, newVal);
                maxDiff = Math.max(maxDiff, Math.abs(newVal - pr.get(node)));
            }

            pr = newPR;
            if (maxDiff < epsilon) {
                break;
            }
        }

        return pr.get(w);
    }

    // ==================== 功能6：随机游走 ====================
    /**
     * 从随机节点开始，沿出边进行一步随机游走
     * 返回null表示游走结束（重复边或无出边）
     * 首次调用时初始化，后续调用继续游走
     */
    private static String walkCurrent = null;
    private static List<String> walkVisitedNodes = new ArrayList<>();
    private static Set<String> walkVisitedEdges = new HashSet<>();
    private static boolean walkFinished = false;
    private static String walkStopReason = "";

    /**
     * 初始化随机游走，返回起始节点
     */
    public static String randomWalkInit() {
        if (graph.isEmpty()) {
            walkFinished = true;
            walkStopReason = "图为空！";
            return null;
        }
        List<String> nodeList = new ArrayList<>(graph.keySet());
        walkCurrent = nodeList.get(random.nextInt(nodeList.size()));
        walkVisitedNodes.clear();
        walkVisitedEdges.clear();
        walkFinished = false;
        walkStopReason = "";
        walkVisitedNodes.add(walkCurrent);
        return walkCurrent;
    }

    /**
     * 执行一步随机游走，返回下一个节点
     * 如果游走结束返回null，停止原因存在walkStopReason中
     */
    public static String randomWalkStep() {
        if (walkFinished || walkCurrent == null) return null;

        Map<String, Integer> neighbors = graph.get(walkCurrent);
        if (neighbors == null || neighbors.isEmpty()) {
            walkFinished = true;
            walkStopReason = "节点 \"" + walkCurrent + "\" 无出边，游走结束";
            return null;
        }

        List<String> neighborList = new ArrayList<>(neighbors.keySet());
        String next = neighborList.get(random.nextInt(neighborList.size()));
        String edge = walkCurrent + " -> " + next;

        if (walkVisitedEdges.contains(edge)) {
            walkVisitedNodes.add(next);
            walkFinished = true;
            walkStopReason = "边 \"" + edge + "\" 重复，游走结束";
            return next;
        }

        walkVisitedEdges.add(edge);
        walkVisitedNodes.add(next);
        walkCurrent = next;
        return next;
    }

    /**
     * 随机游走：从随机节点出发，自动遍历到结束，将结果写入文件并返回。
     * 本函数可独立调用（不依赖randomWalkInit/randomWalkStep）。
     * 同时更新walkVisitedNodes供后续查询。
     */
    public static String randomWalk() {
        if (graph.isEmpty()) {
            return "";
        }
        // 若当前游走状态为空（未通过init/step调用），则执行完整游走
        if (walkVisitedNodes.isEmpty()) {
            randomWalkInit();
            while (!walkFinished) {
                randomWalkStep();
            }
        }
        String result = String.join(" ", walkVisitedNodes);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("random_walk.txt"));
            writer.write(result);
            writer.close();
        } catch (IOException e) {
            // 写入失败时静默处理，由main输出错误信息
        }
        return result;
    }

    public static boolean isWalkFinished() { return walkFinished; }
    public static String getWalkStopReason() { return walkStopReason; }
    public static void setGraphForTest(Map<String, Map<String, Integer>> testGraph) {
    graph = testGraph;
    }
}