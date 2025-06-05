/**
 * AHC048 - Mixing on the Palette
 * 実装方針：
 * 1. 基本的な操作の最適化
 *    - そのまま納品：使用回数の重み付けで均等使用を促進
 *    - 追加注ぎ：容量チェックを厳密に行い、重み付き平均で色を計算
 *    - 混合：隣接するウェルのみを考慮し、効率的な混合を実現
 *    - 混合＋追加注ぎ：混合後の色をさらに改善
 * 
 * 2. ウェル使用の効率化
 *    - 空きウェルの積極的な活用
 *    - 使用回数に基づくペナルティで均等使用を促進
 *    - 前回使用したウェルへのペナルティで多様な使用を促進
 * 
 * 3. 安定性の確保
 *    - 1グラム未満のウェルは使用不可
 *    - 容量オーバーを防ぐ厳密なチェック
 *    - フォールバック処理で必ず操作を実行
 */
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.ArrayDeque;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt();
        int K = sc.nextInt();
        int H = sc.nextInt();
        int T = sc.nextInt();
        int D = sc.nextInt();

        double[][] tubes = readTubes(sc, K);
        double[][] targets = readTargets(sc, H);

        int wellSize = 3;
        int wellsPerRow = N / wellSize;
        int wellCount = wellsPerRow * wellsPerRow;

        printInitialWalls(N, wellSize);

        double[][] wellColors = new double[wellCount][3];
        int[] wellX = new int[wellCount];
        int[] wellY = new int[wellCount];
        double[] wellGrams = new double[wellCount];
        initializeWells(wellColors, wellX, wellY, wellGrams, tubes, K, wellSize, wellsPerRow);

        int prevWell = -1;
        int[] wellUsed = new int[wellCount];
        int[][] removedWalls = new int[H * 4][4];
        int removedWallCount = 0;
        double[][] wellColorGrams = new double[wellCount][K];
        double[][] wellCurrentColor = new double[wellCount][3];
        initializeWellColorGrams(wellColorGrams, wellCurrentColor, tubes, K, wellCount);

        UnionFind uf = new UnionFind(wellCount);

        for (int t = 0; t < H; t++) {
            OperationResult op = selectOperation(
                wellColors, wellGrams, wellUsed, prevWell, tubes, targets[t], K, wellSize, wellsPerRow, wellX, wellY, uf, t, H, wellCount
            );
            prevWell = executeOperation(op, wellColors, wellGrams, wellUsed, wellX, wellY);

            if (removedWallCount > 0) {
                removedWallCount--;
                int[] wall = removedWalls[removedWallCount];
                int w1 = getWellIndex(wall[0], wall[1], wellSize, wellsPerRow);
                int w2 = getWellIndex(wall[2], wall[3], wellSize, wellsPerRow);
                if (uf.getSize(w1) >= 2) {
                    System.out.println("4 " + wall[0] + " " + wall[1] + " " + wall[2] + " " + wall[3]);
                    uf.separate(w1, w2, wellsPerRow);
                }
            }

            updateWellCurrentColor(wellColorGrams, wellCurrentColor, tubes, K, wellCount);

            if (t % 500 == 0) {
                separateLargeGroups(uf, wellCount, wellsPerRow, wellX, wellY);
            }
        }
        sc.close();
    }

    static double[][] readTubes(Scanner sc, int K) {
        double[][] tubes = new double[K][3];
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        return tubes;
    }

    static double[][] readTargets(Scanner sc, int H) {
        double[][] targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }
        return targets;
    }

    static void printInitialWalls(int N, int wellSize) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                System.out.print(((j + 1) % wellSize == 0) ? "1" : "0");
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(((i + 1) % wellSize == 0) ? "1" : "0");
                if (j < N - 1) System.out.print(" ");
            }
            System.out.println();
        }
    }

    static void initializeWells(double[][] wellColors, int[] wellX, int[] wellY, double[] wellGrams, double[][] tubes, int K, int wellSize, int wellsPerRow) {
        int idx = 0;
        for (int wy = 0; wy < wellsPerRow; wy++) {
            for (int wx = 0; wx < wellsPerRow; wx++) {
                int x = wx * wellSize;
                int y = wy * wellSize;
                int tubeIdx = idx % K;
                System.out.println("1 " + x + " " + y + " " + tubeIdx);
                for (int d = 0; d < 3; d++) wellColors[idx][d] = tubes[tubeIdx][d];
                wellX[idx] = x;
                wellY[idx] = y;
                wellGrams[idx] = 1.0;
                idx++;
            }
        }
    }

    static void initializeWellColorGrams(double[][] wellColorGrams, double[][] wellCurrentColor, double[][] tubes, int K, int wellCount) {
        for (int w = 0; w < wellCount; w++) {
            int tubeIdx = w % K;
            wellColorGrams[w][tubeIdx] = 1.0;
            for (int d = 0; d < 3; d++) wellCurrentColor[w][d] = tubes[tubeIdx][d];
        }
    }

    static void updateWellCurrentColor(double[][] wellColorGrams, double[][] wellCurrentColor, double[][] tubes, int K, int wellCount) {
        for (int w = 0; w < wellCount; w++) {
            double total = 0.0;
            for (int k = 0; k < K; k++) total += wellColorGrams[w][k];
            if (total > 1e-8) {
                for (int d = 0; d < 3; d++) {
                    double sum = 0.0;
                    for (int k = 0; k < K; k++) sum += tubes[k][d] * wellColorGrams[w][k];
                    wellCurrentColor[w][d] = sum / total;
                }
            } else {
                for (int d = 0; d < 3; d++) wellCurrentColor[w][d] = 0.0;
            }
        }
    }

    static void separateLargeGroups(UnionFind uf, int wellCount, int wellsPerRow, int[] wellX, int[] wellY) {
        boolean[] checked = new boolean[wellCount];
        for (int w = 0; w < wellCount; w++) {
            int root = uf.find(w);
            if (checked[root]) continue;
            checked[root] = true;
            int groupSize = uf.getSize(w);
            if (groupSize >= 3) {
                List<Integer> group = uf.groupMembers[root];
                outer:
                for (int i = 0; i < group.size(); i++) {
                    int wi = group.get(i);
                    for (int j = i + 1; j < group.size(); j++) {
                        int wj = group.get(j);
                        for (int adj : uf.getAdjacentWells(wi, wellsPerRow)) {
                            if (adj == wj) {
                                int x1 = wellX[wi], y1 = wellY[wi];
                                int x2 = wellX[wj], y2 = wellY[wj];
                                System.out.println("4 " + x1 + " " + y1 + " " + x2 + " " + y2);
                                uf.separate(wi, wj, wellsPerRow);
                                break outer;
                            }
                        }
                    }
                }
            }
        }
    }

    static OperationResult selectOperation(
        double[][] wellColors, double[] wellGrams, int[] wellUsed, int prevWell,
        double[][] tubes, double[] target, int K, int wellSize, int wellsPerRow,
        int[] wellX, int[] wellY, UnionFind uf, int t, int H, int wellCount
    ) {
        double minDist = Double.MAX_VALUE;
        int opType = -1;
        int bestWell = -1, bestTube = -1;
        int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
        double[] bestColor = new double[3];

        // 既存ウェルそのまま納品
        for (int w = 0; w < wellCount; w++) {
            if (wellGrams[w] < 1.0) continue;
            double penalty = (w == prevWell) ? 1.0 : 0.0;
            penalty += 0.02 * wellUsed[w];
            double dist = colorDist(wellColors[w], target) + penalty; // 修正
            if (dist < minDist) {
                minDist = dist;
                opType = 0;
                bestWell = w;
                for (int d = 0; d < 3; d++) bestColor[d] = wellColors[w][d];
            }
        }

        // 追加注ぎ（全ウェル・全チューブ）
        for (int w = 0; w < wellCount; w++) {
            if (wellGrams[w] < 1.0 || wellGrams[w] + 1.0 > wellSize * wellSize) continue;
            for (int k = 0; k < K; k++) {
                double total = wellGrams[w] + 1.0;
                double[] mix = new double[3];
                for (int d = 0; d < 3; d++)
                    mix[d] = (wellColors[w][d] * wellGrams[w] + tubes[k][d]) / total;
                double dist = colorDist(mix, target); // 修正
                if (dist < minDist) {
                    minDist = dist;
                    opType = 1;
                    bestWell = w;
                    bestTube = k;
                    for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                }
            }
        }

        // 混合・混合＋追加注ぎ
        for (int w1 = 0; w1 < wellCount; w1++) {
            if (wellGrams[w1] < 1.0) continue;
            for (int w2 = 0; w2 < wellCount; w2++) {
                if (w1 == w2 || wellGrams[w2] < 1.0) continue;
                double total = wellGrams[w1] + wellGrams[w2];
                if (total > wellSize * wellSize) continue;

                boolean isAdjacent = false;
                for (int i1 = 0; i1 < wellSize && !isAdjacent; i1++) {
                    for (int j1 = 0; j1 < wellSize && !isAdjacent; j1++) {
                        int x1 = wellX[w1] + i1;
                        int y1 = wellY[w1] + j1;
                        for (int i2 = 0; i2 < wellSize && !isAdjacent; i2++) {
                            for (int j2 = 0; j2 < wellSize && !isAdjacent; j2++) {
                                int x2 = wellX[w2] + i2;
                                int y2 = wellY[w2] + j2;
                                if (Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1) {
                                    isAdjacent = true;
                                    double[] mix = new double[3];
                                    for (int d = 0; d < 3; d++)
                                        mix[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / total;
                                    double dist = colorDist(mix, target); // 修正
                                    if (dist < minDist) {
                                        minDist = dist;
                                        opType = 2;
                                        mixW1 = w1; mixW2 = w2;
                                        mixX1 = x1; mixY1 = y1; mixX2 = x2; mixY2 = y2;
                                        for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                                    }

                                    for (int k = 0; k < K; k++) {
                                        if (total + 1.0 > wellSize * wellSize) continue;
                                        double[] mixAdd = new double[3];
                                        for (int d = 0; d < 3; d++)
                                            mixAdd[d] = (mix[d] * total + tubes[k][d]) / (total + 1.0);
                                        double dist2 = colorDist(mixAdd, target); // 修正
                                        if (dist2 < minDist) {
                                            minDist = dist2;
                                            opType = 3;
                                            bestTube = k;
                                            mixW1 = w1; mixW2 = w2;
                                            mixX1 = x1; mixY1 = y1; mixX2 = x2; mixY2 = y2;
                                            for (int d = 0; d < 3; d++) bestColor[d] = mixAdd[d];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 空きウェルの活用
        if (opType == -1) {
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1e-8) {
                    int bestTubeIdx = 0;
                    double bestTubeDist = Double.MAX_VALUE;
                    for (int k = 0; k < K; k++) {
                        double d = colorDist(tubes[k], target); // 修正
                        if (d < bestTubeDist) {
                            bestTubeDist = d;
                            bestTubeIdx = k;
                        }
                    }
                    if (bestTubeDist < minDist) {
                        minDist = bestTubeDist;
                        opType = 1;
                        bestWell = w;
                        bestTube = bestTubeIdx;
                        for (int d = 0; d < 3; d++) bestColor[d] = tubes[bestTubeIdx][d];
                    }
                }
            }
        }

        return new OperationResult(opType, bestWell, bestTube, mixW1, mixW2, mixX1, mixY1, mixX2, mixY2, bestColor);
    }

    static int executeOperation(OperationResult op, double[][] wellColors, double[] wellGrams, int[] wellUsed, int[] wellX, int[] wellY) {
        int prevWell = -1;
        // 操作の実行
        if (op.opType == 0) {
            // そのまま納品
            System.out.println("2 " + wellX[op.bestWell] + " " + wellY[op.bestWell]);
            wellGrams[op.bestWell] -= 1.0;
            prevWell = op.bestWell;
            wellUsed[op.bestWell]++;
        } else if (op.opType == 1) {
            // 追加注ぎ
            System.out.println("1 " + wellX[op.bestWell] + " " + wellY[op.bestWell] + " " + op.bestTube);
            for (int d = 0; d < 3; d++) wellColors[op.bestWell][d] = op.bestColor[d];
            wellGrams[op.bestWell] += 1.0;
            System.out.println("2 " + wellX[op.bestWell] + " " + wellY[op.bestWell]);
            wellGrams[op.bestWell] -= 1.0;
            prevWell = op.bestWell;
            wellUsed[op.bestWell]++;
        } else if (op.opType == 2) {
            // 混合
            System.out.println("4 " + op.mixX1 + " " + op.mixY1 + " " + op.mixX2 + " " + op.mixY2);
            for (int d = 0; d < 3; d++) wellColors[op.mixW1][d] = op.bestColor[d];
            wellGrams[op.mixW1] += wellGrams[op.mixW2];
            wellGrams[op.mixW2] = 0.0;
            System.out.println("2 " + wellX[op.mixW1] + " " + wellY[op.mixW1]);
            wellGrams[op.mixW1] -= 1.0;
            prevWell = op.mixW1;
            wellUsed[op.mixW1]++;
        } else if (op.opType == 3) {
            // 混合＋追加注ぎ
            System.out.println("4 " + op.mixX1 + " " + op.mixY1 + " " + op.mixX2 + " " + op.mixY2);
            for (int d = 0; d < 3; d++) wellColors[op.mixW1][d] = op.bestColor[d];
            wellGrams[op.mixW1] += wellGrams[op.mixW2];
            wellGrams[op.mixW2] = 0.0;
            System.out.println("1 " + wellX[op.mixW1] + " " + wellY[op.mixW1] + " " + op.bestTube);
            wellGrams[op.mixW1] += 1.0;
            System.out.println("2 " + wellX[op.mixW1] + " " + wellY[op.mixW1]);
            wellGrams[op.mixW1] -= 1.0;
            prevWell = op.mixW1;
            wellUsed[op.mixW1]++;
        }
        return prevWell;
    }

    static double colorDist(double[] c1, double[] c2) {
        double dr = c1[0] - c2[0];
        double dg = c1[1] - c2[1];
        double db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    static int getWellIndex(int x, int y, int wellSize, int wellsPerRow) {
        int wx = x / wellSize;
        int wy = y / wellSize;
        return wy * wellsPerRow + wx;
    }

    // --- 操作結果をまとめるクラス ---
    static class OperationResult {
        int opType, bestWell, bestTube, mixW1, mixW2, mixX1, mixY1, mixX2, mixY2;
        double[] bestColor;
        OperationResult(int opType, int bestWell, int bestTube, int mixW1, int mixW2, int mixX1, int mixY1, int mixX2, int mixY2, double[] bestColor) {
            this.opType = opType;
            this.bestWell = bestWell;
            this.bestTube = bestTube;
            this.mixW1 = mixW1;
            this.mixW2 = mixW2;
            this.mixX1 = mixX1;
            this.mixY1 = mixY1;
            this.mixX2 = mixX2;
            this.mixY2 = mixY2;
            this.bestColor = bestColor;
        }
    }

    // --- Union-Find構造体（分離対応） ---
    static class UnionFind {
        int[] parent, size;
        List<Integer>[] groupMembers;

        UnionFind(int n) {
            parent = new int[n];
            size = new int[n];
            groupMembers = new ArrayList[n];
            for (int i = 0; i < n; i++) {
                parent[i] = i;
                size[i] = 1;
                groupMembers[i] = new ArrayList<>();
                groupMembers[i].add(i);
            }
        }
        int find(int x) {
            if (parent[x] == x) return x;
            return parent[x] = find(parent[x]);
        }
        void unite(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry) return;
            if (size[rx] < size[ry]) {
                parent[rx] = ry;
                size[ry] += size[rx];
                groupMembers[ry].addAll(groupMembers[rx]);
                groupMembers[rx].clear();
            } else {
                parent[ry] = rx;
                size[rx] += size[ry];
                groupMembers[rx].addAll(groupMembers[ry]);
                groupMembers[ry].clear();
            }
        }
        boolean same(int x, int y) {
            return find(x) == find(y);
        }
        int getSize(int x) {
            return size[find(x)];
        }
        void separate(int x, int y, int wellsPerRow) {
            int rx = find(x);
            if (find(y) != rx) return;
            int n = parent.length;
            boolean[] visited = new boolean[n];
            List<Integer> groupX = new ArrayList<>();
            List<Integer> groupY = new ArrayList<>();
            Queue<Integer> q = new ArrayDeque<>();
            q.add(x);
            visited[x] = true;
            while (!q.isEmpty()) {
                int v = q.poll();
                groupX.add(v);
                for (int u : getAdjacentWells(v, wellsPerRow)) {
                    if (!visited[u] && same(u, x) && !((v == x && u == y) || (v == y && u == x))) {
                        visited[u] = true;
                        q.add(u);
                    }
                }
            }
            for (int v : groupMembers[rx]) {
                if (!visited[v]) groupY.add(v);
            }
            for (int v : groupY) {
                parent[v] = v;
                size[v] = 1;
                groupMembers[v].clear();
                groupMembers[v].add(v);
            }
            for (int v : groupX) {
                parent[v] = x;
            }
            size[x] = groupX.size();
            groupMembers[x].clear();
            groupMembers[x].addAll(groupX);
        }
        List<Integer> getAdjacentWells(int w, int wellsPerRow) {
            List<Integer> res = new ArrayList<>();
            int[] dx = {1, -1, 0, 0};
            int[] dy = {0, 0, 1, -1};
            int wx = w % wellsPerRow;
            int wy = w / wellsPerRow;
            for (int d = 0; d < 4; d++) {
                int nx = wx + dx[d];
                int ny = wy + dy[d];
                if (0 <= nx && nx < wellsPerRow && 0 <= ny && ny < wellsPerRow) {
                    res.add(ny * wellsPerRow + nx);
                }
            }
            return res;
        }
    }
}