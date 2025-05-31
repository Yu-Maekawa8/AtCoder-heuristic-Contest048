/**
 * AHC048 - Mixing on the Palette
 *
 * このプログラムは、20×20のパレットを5×5のウェル（小部屋、各4×4マス）に分割し、
 * 各ウェルに絵の具（チューブ色）を1gずつ初期配置した後、ターゲット色に近い色を作って納品する問題の解法です。
 *
 * 【全体の流れ】
 * 1. パレットの仕切りを出力し、5×5のウェル構造を作る。
 * 2. 各ウェルにチューブ色を順番に1gずつ入れる（初期化）。
 * 3. 各ターゲット色ごとに、以下の操作を最適な組み合わせで選択し納品する：
 *    - そのまま納品（既存ウェルの色が近い場合）
 *    - 追加注ぎ（既存ウェルにチューブ色を加えて混ぜる）
 *    - 混合（隣接する2つのウェルを混ぜる）
 *    - 混合＋追加注ぎ（混合後さらにチューブ色を加えて混ぜる）
 *    - 使えるウェルがない場合は空きウェルに新たに色を追加
 *
 * 【工夫点】
 * ・納品・追加注ぎ・混合・混合＋追加注ぎの全組み合わせを全探索し、ターゲット色に最も近づく操作を選択します。
 * ・各ウェルの色（RGB）と残量（グラム数）を管理し、1g未満のウェルは使わないようにしています。
 * ・混合はマス単位で隣接判定し、正しく混ぜられるようにしています。
 * ・納品後や混合後のウェルの状態も正しく更新します。
 * ・
 *
 * 【注意点】
 * ・動作1: 色を追加（追加注ぎ）、動作2: 納品、動作4: 混合（仕切りを外して混ぜる）を出力します。
 * ・動作3（破棄）は本実装では使用していません。
 * ・最大ターン数TやコストDは本実装では未使用です。
 */

import java.util.Scanner;

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
        int wellSize = 4; // 1ウェルの一辺の長さ
        int wellsPerRow = N / wellSize; // 1行あたりのウェル数（5）
        int wellCount = wellsPerRow * wellsPerRow; // 全ウェル数（25）

        // --- 仕切り出力（5×5分割）---
        // 各マスの間に仕切りを出力（動作0/1）
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                // 4,8,12,16列目に仕切り
                System.out.print(((j + 1) % wellSize == 0) ? "1" : "0");
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                // 4,8,12,16行目に仕切り
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
        // 各ウェルに初期色（チューブ色を順番に）を1gずつ入れる
        for (int wy = 0; wy < wellsPerRow; wy++) {
            for (int wx = 0; wx < wellsPerRow; wx++) {
                int x = wx * wellSize;
                int y = wy * wellSize;
                int tubeIdx = idx % K; // チューブ色を順番に割り当て
                System.out.println("1 " + x + " " + y + " " + tubeIdx); // 動作1: 色を入れる
                for (int d = 0; d < 3; d++) wellColors[idx][d] = tubes[tubeIdx][d];
                wellX[idx] = x;
                wellY[idx] = y;
                wellGrams[idx] = 1.0; // 1g入れる
                idx++;
            }
        }

        // --- 各ターゲット色ごとに処理 ---
        int prevWell = -1; // 直前に納品したウェル
        int[] wellUsed = new int[wellCount]; // 各ウェルの使用回数（ローテーション用）

        for (int t = 0; t < H; t++) {
            // 1. 全ウェル・全操作の全組み合わせで最小色差を探索
            double minDist = Double.MAX_VALUE;
            int opType = -1; // 0:そのまま納品, 1:追加注ぎ, 2:混合, 3:混合+追加注ぎ, 4:新規
            int bestWell = -1, bestTube = -1;
            int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
            double[] bestColor = new double[3];

            // 既存ウェルそのまま納品
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue;
                double penalty = (w == prevWell) ? 1.0 : 0.0;
                penalty += 0.01 * wellUsed[w];
                double dist = colorDist(wellColors[w], targets[t]) + penalty;
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
                    double dist = colorDist(mix, targets[t]);
                    if (dist < minDist) {
                        minDist = dist;
                        opType = 1;
                        bestWell = w;
                        bestTube = k;
                        for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                    }
                }
            }

            // 混合・混合＋追加注ぎ（全ウェルペア・全チューブ・全マス隣接）
            for (int w1 = 0; w1 < wellCount; w1++) {
                if (wellGrams[w1] < 1.0) continue;
                for (int w2 = 0; w2 < wellCount; w2++) {
                    if (w1 == w2 || wellGrams[w2] < 1.0) continue;
                    double total = wellGrams[w1] + wellGrams[w2];
                    if (total > wellSize * wellSize) continue;
                    for (int i1 = 0; i1 < wellSize; i1++) {
                        for (int j1 = 0; j1 < wellSize; j1++) {
                            int x1 = wellX[w1] + i1;
                            int y1 = wellY[w1] + j1;
                            for (int dxy = 0; dxy < 4; dxy++) {
                                int[] dx = {1, 0, -1, 0};
                                int[] dy = {0, 1, 0, -1};
                                int x2 = x1 + dx[dxy];
                                int y2 = y1 + dy[dxy];
                                if (x2 < 0 || x2 >= N || y2 < 0 || y2 >= N) continue;
                                if (x2 >= wellX[w2] && x2 < wellX[w2] + wellSize &&
                                    y2 >= wellY[w2] && y2 < wellY[w2] + wellSize) {
                                    // 混合
                                    double[] mix = new double[3];
                                    for (int d = 0; d < 3; d++)
                                        mix[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / total;
                                    double dist = colorDist(mix, targets[t]);
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

            // 新規色（空きウェル＋最も近いチューブ色）
            int emptyWell = -1;
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1e-8) {
                    emptyWell = w;
                    break;
                }
            }
            if (emptyWell != -1) {
                int bestTubeIdx = 0;
                double bestTubeDist = Double.MAX_VALUE;
                for (int k = 0; k < K; k++) {
                    double d = colorDist(tubes[k], targets[t]);
                    if (d < bestTubeDist) {
                        bestTubeDist = d;
                        bestTubeIdx = k;
                    }
                }
                if (bestTubeDist < minDist) {
                    minDist = bestTubeDist;
                    opType = 4;
                    bestWell = emptyWell;
                    bestTube = bestTubeIdx;
                    for (int d = 0; d < 3; d++) bestColor[d] = tubes[bestTubeIdx][d];
                }
            }

            // 実行
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
            } else if (opType == 4) {
                System.out.println("1 " + wellX[bestWell] + " " + wellY[bestWell] + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestColor[d];
                wellGrams[bestWell] = 1.0;
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            }
        }
        sc.close();
    }

    /**
     * 2色間のユークリッド距離を返す（色差の計算）
     * @param c1 色1（RGB配列）
     * @param c2 色2（RGB配列）
     * @return ユークリッド距離
     */
    static double colorDist(double[] c1, double[] c2) {
        double dc = c1[0] - c2[0];
        double dm = c1[1] - c2[1];
        double dy = c1[2] - c2[2];
        return Math.sqrt(dc * dc + dm * dm + dy * dy);
    }
}
