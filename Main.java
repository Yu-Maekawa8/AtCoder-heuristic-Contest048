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

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt(); // パレットの一辺(20 固定)
        int K = sc.nextInt(); // 絵の具の種類数
        int H = sc.nextInt(); // ターゲット色の数(1000 固定)
        int T = sc.nextInt(); // 最大ターン数（未使用）
        int D = sc.nextInt(); // 1グラム出すコストD（未使用）

        // 絵の具の色（各RGB成分）を読み込み
        double[][] tubes = new double[K][3];
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        // ターゲット色を読み込み
        double[][] targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // --- パレットを5×5のウェルに分割（wellSize=4, N=20で25個） ---
        int wellSize = 2; // 1ウェルの一辺の長さ
        int wellsPerRow = (N-6) / wellSize; // 1行あたりのウェル数（5）
        int wellCount = wellsPerRow * wellsPerRow; // 全ウェル数（25）

        // --- 仕切り出力（5×5分割）---
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

        // --- 各ウェルの初期化 ---
        double[][] wellColors = new double[wellCount][3]; // 各ウェルの色（RGB）
        int[] wellX = new int[wellCount]; // 各ウェルの左上x座標
        int[] wellY = new int[wellCount]; // 各ウェルの左上y座標
        double[] wellGrams = new double[wellCount]; // 各ウェルのグラム数
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

        // === 機械学習風の重み調整システム ===
        double[] operationWeights = {1.0, 1.0, 1.0, 1.0}; // [直接納品, 追加注ぎ, 混合, 混合+追加]
        int[] operationCounts = {0, 0, 0, 0};              // 各操作の実行回数
        double[] operationSuccessSum = {0.0, 0.0, 0.0, 0.0}; // 各操作の成功度合計
        double[] operationErrorSum = {0.0, 0.0, 0.0, 0.0};   // 各操作の誤差合計
        
        // チューブごとの成功率
        double[] tubeWeights = new double[K];
        int[] tubeCounts = new int[K];
        double[] tubeSuccessSum = new double[K];
        for (int i = 0; i < K; i++) {
            tubeWeights[i] = 1.0;
            tubeCounts[i] = 0;
            tubeSuccessSum[i] = 0.0;
        }

        int prevWell = -1;
        int[] wellUsed = new int[wellCount];

        for (int t = 0; t < H; t++) {
            double minDist = Double.MAX_VALUE;
            int opType = -1;
            int bestWell = -1, bestTube = -1;
            int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
            double[] bestColor = new double[3];

            // === 学習による重み更新（50ターンごと） ===
            if (t > 0 && t % 50 == 0) {
                for (int op = 0; op < 4; op++) {
                    if (operationCounts[op] > 0) {
                        double avgSuccess = operationSuccessSum[op] / operationCounts[op];
                        double avgError = operationErrorSum[op] / operationCounts[op];
                        
                        // 成功率が高い操作の重みを増加、低い操作の重みを減少
                        if (avgError < 0.1) { // 高精度
                            operationWeights[op] *= 1.1;
                        } else if (avgError > 0.3) { // 低精度
                            operationWeights[op] *= 0.95;
                        }
                        
                        // 重みの範囲制限
                        operationWeights[op] = Math.max(0.5, Math.min(2.0, operationWeights[op]));
                    }
                }
                
                // チューブ重みの更新
                for (int k = 0; k < K; k++) {
                    if (tubeCounts[k] > 0) {
                        double avgTubeSuccess = tubeSuccessSum[k] / tubeCounts[k];
                        if (avgTubeSuccess < 0.1) {
                            tubeWeights[k] *= 1.05; // 良いチューブの重みを増加
                        } else if (avgTubeSuccess > 0.3) {
                            tubeWeights[k] *= 0.98; // 悪いチューブの重みを減少
                        }
                        tubeWeights[k] = Math.max(0.7, Math.min(1.5, tubeWeights[k]));
                    }
                }
            }

            // 既存ウェルそのまま納品（重み調整適用）
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue;
                double penalty = (w == prevWell) ? 1.0 : 0.0;
                penalty += 0.02 * wellUsed[w];
                double dist = colorDist(wellColors[w], targets[t]) + penalty;
                
                // 重み調整を適用
                dist /= operationWeights[0]; // 直接納品の重み
                
                if (dist < minDist) {
                    minDist = dist;
                    opType = 0;
                    bestWell = w;
                    for (int d = 0; d < 3; d++) bestColor[d] = wellColors[w][d];
                }
            }

            // 追加注ぎ（重み調整適用）
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0 || wellGrams[w] + 1.0 > wellSize * wellSize) continue;
                for (int k = 0; k < K; k++) {
                    double total = wellGrams[w] + 1.0;
                    double[] mix = new double[3];
                    for (int d = 0; d < 3; d++)
                        mix[d] = (wellColors[w][d] * wellGrams[w] + tubes[k][d]) / total;
                    double dist = colorDist(mix, targets[t]);
                    
                    // 重み調整を適用（操作重み × チューブ重み）
                    dist /= (operationWeights[1] * tubeWeights[k]);
                    
                    if (dist < minDist) {
                        minDist = dist;
                        opType = 1;
                        bestWell = w;
                        bestTube = k;
                        for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                    }
                }
            }

            // 混合（重み調整適用）
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
                                        
                                        // 混合
                                        double[] mix = new double[3];
                                        for (int d = 0; d < 3; d++)
                                            mix[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / total;
                                        double dist = colorDist(mix, targets[t]);
                                        
                                        // 重み調整を適用
                                        dist /= operationWeights[2]; // 混合の重み
                                        
                                        if (dist < minDist) {
                                            minDist = dist;
                                            opType = 2;
                                            mixW1 = w1; mixW2 = w2;
                                            mixX1 = x1; mixY1 = y1; mixX2 = x2; mixY2 = y2;
                                            for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                                        }

                                        // 混合＋追加注ぎ
                                        for (int k = 0; k < K; k++) {
                                            if (total + 1.0 > wellSize * wellSize) continue;
                                            double[] mixAdd = new double[3];
                                            for (int d = 0; d < 3; d++)
                                                mixAdd[d] = (mix[d] * total + tubes[k][d]) / (total + 1.0);
                                            double dist2 = colorDist(mixAdd, targets[t]);
                                            
                                            // 重み調整を適用（操作重み × チューブ重み）
                                            dist2 /= (operationWeights[3] * tubeWeights[k]);
                                            
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

            // 空きウェルへの注ぎ込み
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1e-8) {
                    int bestTubeIdx = 0;
                    double bestTubeDist = Double.MAX_VALUE;
                    for (int k = 0; k < K; k++) {
                        double d = colorDist(tubes[k], targets[t]);
                        d /= tubeWeights[k]; // チューブ重み適用
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

            // === 操作実行と学習データ収集 ===
            double actualError = colorDist(bestColor, targets[t]); // 実際の誤差
            
            // 学習データを蓄積
            operationCounts[opType]++;
            operationSuccessSum[opType] += (1.0 - Math.min(1.0, actualError)); // 成功度
            operationErrorSum[opType] += actualError;
            
            if (opType == 1 || opType == 3) { // チューブを使用する操作
                tubeCounts[bestTube]++;
                tubeSuccessSum[bestTube] += actualError;
            }

            // 既存の操作実行部分はそのまま
            if (opType == 0) {
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 1) {
                System.out.println("1 " + wellX[bestWell] + " " + wellY[bestWell] + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestColor[d];
                wellGrams[bestWell] += 1.0;
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 2) {
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = bestColor[d];
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0;
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0;
                prevWell = mixW1;
                wellUsed[mixW1]++;
            } else if (opType == 3) {
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = bestColor[d];
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0;
                System.out.println("1 " + wellX[mixW1] + " " + wellY[mixW1] + " " + bestTube);
                wellGrams[mixW1] += 1.0;
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0;
                prevWell = mixW1;
                wellUsed[mixW1]++;
            }
        }
        sc.close();
    }

    // --- RGBユークリッド距離での色差計算 ---
    static double colorDist(double[] c1, double[] c2) {
        double dr = c1[0] - c2[0];
        double dg = c1[1] - c2[1];
        double db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}