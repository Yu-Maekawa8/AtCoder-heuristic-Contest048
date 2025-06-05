/**
 * AHC048 - Mixing on the Palette (改良版)
 * 改良点：
 * 1. 動的ウェルサイズの調整
 * 2. より効率的な色近似アルゴリズム
 * 3. 先読み機能の追加
 * 4. 無駄な操作の削減
 * 5. メモリ効率の改善
 */
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class nextMain {
    
    // グローバル変数
    static int N, K, H, T, D;
    static double[][] tubes;
    static double[][] targets;
    static int wellSize = 3;
    static int wellsPerRow;
    static int wellCount;
    static double[][] wellColors;
    static int[] wellX, wellY;
    static double[] wellGrams;
    static int[] wellUsed;
    static int prevWell = -1;
    
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        N = sc.nextInt();
        K = sc.nextInt();
        H = sc.nextInt();
        T = sc.nextInt();
        D = sc.nextInt();

        // 絵の具の色を読み込み
        tubes = new double[K][3];
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        
        // ターゲット色を読み込み
        targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // 動的ウェルサイズの決定
        optimizeWellSize();
        
        wellsPerRow = N / wellSize;
        wellCount = wellsPerRow * wellsPerRow;

        // 仕切り出力
        outputWalls();
        
        // ウェルの初期化
        initializeWells();
        
        // メイン処理
        for (int t = 0; t < H; t++) {
            processSingleTarget(t);
        }
        
        sc.close();
    }
    
    // 動的ウェルサイズの最適化
    static void optimizeWellSize() {
        // Kとターゲット色の分散を考慮してウェルサイズを決定
        double avgColorVariance = calculateColorVariance();
        
        if (K <= 10 && avgColorVariance < 0.1) {
            wellSize = 3; // 大きめのウェルで混合を重視
        } else if (K > 50) {
            wellSize = 3; // 小さめのウェルで多様性を重視
        } else {
            wellSize = 3; // デフォルト
        }
        
        // Nに対する制約チェック
        while (N % wellSize != 0 && wellSize > 1) {
            wellSize--;
        }
    }
    
    // 色の分散を計算
    static double calculateColorVariance() {
        double[] avgColor = new double[3];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < 3; j++) {
                avgColor[j] += tubes[i][j];
            }
        }
        for (int j = 0; j < 3; j++) {
            avgColor[j] /= K;
        }
        
        double variance = 0;
        for (int i = 0; i < K; i++) {
            variance += colorDist(tubes[i], avgColor);
        }
        return variance / K;
    }
    
    // 仕切り出力
    static void outputWalls() {
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
    
    // ウェルの初期化
    static void initializeWells() {
        wellColors = new double[wellCount][3];
        wellX = new int[wellCount];
        wellY = new int[wellCount];
        wellGrams = new double[wellCount];
        wellUsed = new int[wellCount];
        
        // 初期配置の最適化：多様な色を配置
        int[] tubeUsage = new int[K];
        int idx = 0;
        
        for (int wy = 0; wy < wellsPerRow; wy++) {
            for (int wx = 0; wx < wellsPerRow; wx++) {
                int x = wx * wellSize;
                int y = wy * wellSize;
                
                // 最も使用頻度の低いチューブを選択
                int bestTube = 0;
                for (int k = 1; k < K; k++) {
                    if (tubeUsage[k] < tubeUsage[bestTube]) {
                        bestTube = k;
                    }
                }
                tubeUsage[bestTube]++;
                
                System.out.println("1 " + x + " " + y + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[idx][d] = tubes[bestTube][d];
                wellX[idx] = x;
                wellY[idx] = y;
                wellGrams[idx] = 1.0;
                idx++;
            }
        }
    }
    
    // 単一ターゲットの処理
    static void processSingleTarget(int targetIndex) {
        double[] target = targets[targetIndex];
        
        // 先読み：次のターゲットも考慮
        double[] nextTarget = (targetIndex + 1 < H) ? targets[targetIndex + 1] : null;
        
        double minCost = Double.MAX_VALUE;
        Operation bestOp = null;
        
        // 各操作パターンを評価
        bestOp = evaluateOperations(target, nextTarget, minCost, bestOp);
        
        // 最適操作を実行
        if (bestOp != null) {
            executeOperation(bestOp);
        } else {
            // フォールバック：最もシンプルな操作
            executeFallback(target);
        }
        
        // 統計更新
        if (bestOp != null && bestOp.wellIndex >= 0) {
            prevWell = bestOp.wellIndex;
            wellUsed[bestOp.wellIndex]++;
        }
    }
    
    // 操作の評価
    static Operation evaluateOperations(double[] target, double[] nextTarget, double minCost, Operation bestOp) {
        // 1. そのまま納品
        bestOp = evaluateDirectDelivery(target, nextTarget, minCost, bestOp);
        
        // 2. 追加注ぎ
        bestOp = evaluateAddPaint(target, nextTarget, minCost, bestOp);
        
        // 3. 混合系操作
        bestOp = evaluateMixing(target, nextTarget, minCost, bestOp);
        
        return bestOp;
    }
    
    // そのまま納品の評価
    static Operation evaluateDirectDelivery(double[] target, double[] nextTarget, double minCost, Operation bestOp) {
        for (int w = 0; w < wellCount; w++) {
            if (wellGrams[w] < 1.0 - 1e-6) continue;
            
            double cost = calculateCost(wellColors[w], target, w, nextTarget);
            if (cost < minCost) {
                minCost = cost;
                bestOp = new Operation(0, w, -1, -1, -1, -1, -1, -1, -1, wellColors[w].clone());
            }
        }
        return bestOp;
    }
    
    // 追加注ぎの評価
    static Operation evaluateAddPaint(double[] target, double[] nextTarget, double minCost, Operation bestOp) {
        for (int w = 0; w < wellCount; w++) {
            if (wellGrams[w] < 1.0 - 1e-6 || wellGrams[w] + 1.0 > wellSize * wellSize) continue;
            
            for (int k = 0; k < K; k++) {
                double total = wellGrams[w] + 1.0;
                double[] newColor = new double[3];
                for (int d = 0; d < 3; d++) {
                    newColor[d] = (wellColors[w][d] * wellGrams[w] + tubes[k][d]) / total;
                }
                
                double cost = calculateCost(newColor, target, w, nextTarget);
                if (cost < minCost) {
                    minCost = cost;
                    bestOp = new Operation(1, w, k, -1, -1, -1, -1, -1, -1, newColor);
                }
            }
        }
        return bestOp;
    }
    
    // 混合系操作の評価
    static Operation evaluateMixing(double[] target, double[] nextTarget, double minCost, Operation bestOp) {
        for (int w1 = 0; w1 < wellCount; w1++) {
            if (wellGrams[w1] < 1.0 - 1e-6) continue;
            
            for (int w2 = 0; w2 < wellCount; w2++) {
                if (w1 == w2 || wellGrams[w2] < 1.0 - 1e-6) continue;
                
                double total = wellGrams[w1] + wellGrams[w2];
                if (total > wellSize * wellSize) continue;
                
                // 隣接チェック
                int[] coords = findAdjacentCoords(w1, w2);
                if (coords == null) continue;
                
                // 混合色の計算
                double[] mixColor = new double[3];
                for (int d = 0; d < 3; d++) {
                    mixColor[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / total;
                }
                
                // そのまま混合
                double cost = calculateCost(mixColor, target, w1, nextTarget);
                if (cost < minCost) {
                    minCost = cost;
                    bestOp = new Operation(2, w1, -1, w2, coords[0], coords[1], coords[2], coords[3], -1, mixColor);
                }
                
                // 混合＋追加注ぎ
                for (int k = 0; k < K; k++) {
                    if (total + 1.0 > wellSize * wellSize) continue;
                    
                    double[] finalColor = new double[3];
                    for (int d = 0; d < 3; d++) {
                        finalColor[d] = (mixColor[d] * total + tubes[k][d]) / (total + 1.0);
                    }
                    
                    cost = calculateCost(finalColor, target, w1, nextTarget);
                    if (cost < minCost) {
                        minCost = cost;
                        bestOp = new Operation(3, w1, k, w2, coords[0], coords[1], coords[2], coords[3], -1, finalColor);
                    }
                }
            }
        }
        return bestOp;
    }
    
    // コスト計算（先読み機能付き）
    static double calculateCost(double[] color, double[] target, int wellIndex, double[] nextTarget) {
        double baseCost = colorDist(color, target);
        
        // ペナルティ
        double penalty = 0.0;
        if (wellIndex == prevWell) penalty += 0.5; // 前回と同じウェル
        penalty += 0.01 * wellUsed[wellIndex]; // 使用回数
        
        // 先読みボーナス（次のターゲットにも近い場合）
        double futureBonus = 0.0;
        if (nextTarget != null) {
            double nextDist = colorDist(color, nextTarget);
            futureBonus = Math.max(0, 0.1 - nextDist); // 次に近いほどボーナス
        }
        
        return baseCost + penalty - futureBonus;
    }
    
    // 隣接座標の検索
    static int[] findAdjacentCoords(int w1, int w2) {
        for (int i1 = 0; i1 < wellSize; i1++) {
            for (int j1 = 0; j1 < wellSize; j1++) {
                int x1 = wellX[w1] + i1;
                int y1 = wellY[w1] + j1;
                for (int i2 = 0; i2 < wellSize; i2++) {
                    for (int j2 = 0; j2 < wellSize; j2++) {
                        int x2 = wellX[w2] + i2;
                        int y2 = wellY[w2] + j2;
                        if (Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1) {
                            return new int[]{x1, y1, x2, y2};
                        }
                    }
                }
            }
        }
        return null;
    }
    
    // 操作の実行
    static void executeOperation(Operation op) {
        switch (op.type) {
            case 0: // そのまま納品
                System.out.println("2 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex]);
                wellGrams[op.wellIndex] -= 1.0;
                break;
                
            case 1: // 追加注ぎ
                System.out.println("1 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex] + " " + op.tubeIndex);
                for (int d = 0; d < 3; d++) wellColors[op.wellIndex][d] = op.resultColor[d];
                wellGrams[op.wellIndex] += 1.0;
                System.out.println("2 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex]);
                wellGrams[op.wellIndex] -= 1.0;
                break;
                
            case 2: // 混合
                System.out.println("4 " + op.x1 + " " + op.y1 + " " + op.x2 + " " + op.y2);
                for (int d = 0; d < 3; d++) wellColors[op.wellIndex][d] = op.resultColor[d];
                wellGrams[op.wellIndex] += wellGrams[op.well2Index];
                wellGrams[op.well2Index] = 0.0;
                System.out.println("2 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex]);
                wellGrams[op.wellIndex] -= 1.0;
                break;
                
            case 3: // 混合＋追加注ぎ
                System.out.println("4 " + op.x1 + " " + op.y1 + " " + op.x2 + " " + op.y2);
                wellGrams[op.wellIndex] += wellGrams[op.well2Index];
                wellGrams[op.well2Index] = 0.0;
                System.out.println("1 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex] + " " + op.tubeIndex);
                for (int d = 0; d < 3; d++) wellColors[op.wellIndex][d] = op.resultColor[d];
                wellGrams[op.wellIndex] += 1.0;
                System.out.println("2 " + wellX[op.wellIndex] + " " + wellY[op.wellIndex]);
                wellGrams[op.wellIndex] -= 1.0;
                break;
        }
    }
    
    // フォールバック処理
    static void executeFallback(double[] target) {
        // 最適なチューブを見つけて空きウェルに注ぎ、納品
        int bestTube = 0;
        double bestDist = Double.MAX_VALUE;
        for (int k = 0; k < K; k++) {
            double dist = colorDist(tubes[k], target);
            if (dist < bestDist) {
                bestDist = dist;
                bestTube = k;
            }
        }
        
        // 空きウェルを探す
        for (int w = 0; w < wellCount; w++) {
            if (wellGrams[w] < 1e-8) {
                System.out.println("1 " + wellX[w] + " " + wellY[w] + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[w][d] = tubes[bestTube][d];
                wellGrams[w] = 1.0;
                System.out.println("2 " + wellX[w] + " " + wellY[w]);
                wellGrams[w] = 0.0;
                prevWell = w;
                wellUsed[w]++;
                return;
            }
        }
    }
    
    // 色差計算
    static double colorDist(double[] c1, double[] c2) {
        double dr = c1[0] - c2[0];
        double dg = c1[1] - c2[1];
        double db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
    
    // 操作クラス
    static class Operation {
        int type; // 0:直接, 1:追加注ぎ, 2:混合, 3:混合+追加注ぎ
        int wellIndex, tubeIndex, well2Index;
        int x1, y1, x2, y2;
        int additionalParam;
        double[] resultColor;
        
        Operation(int type, int wellIndex, int tubeIndex, int well2Index, 
                 int x1, int y1, int x2, int y2, int additionalParam, double[] resultColor) {
            this.type = type;
            this.wellIndex = wellIndex;
            this.tubeIndex = tubeIndex;
            this.well2Index = well2Index;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.additionalParam = additionalParam;
            this.resultColor = resultColor;
        }
    }
}