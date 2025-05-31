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
        for (int t = 0; t < H; t++) {
            // 1. 全ウェルから最も近い色を探索（1g以上残っているもののみ）
            int bestWell = -1;
            double bestDist = Double.MAX_VALUE;
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue; // 1g未満は使えない
                double dist = colorDist(wellColors[w], targets[t]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestWell = w;
                }
            }
            // 使えるウェルがない場合は空きウェルにチューブ0番を追加
            if (bestWell == -1) {
                for (int w = 0; w < wellCount; w++) {
                    if (wellGrams[w] < 1e-8) {
                        System.out.println("1 " + wellX[w] + " " + wellY[w] + " 0");
                        for (int d = 0; d < 3; d++) wellColors[w][d] = tubes[0][d];
                        wellGrams[w] = 1.0;
                        bestWell = w;
                        break;
                    }
                }
                // それでも空きがなければスキップ
                if (bestWell == -1) continue;
                bestDist = colorDist(wellColors[bestWell], targets[t]);
            }
            int wx = wellX[bestWell];
            int wy = wellY[bestWell];

            // 2. 追加注ぎ・混合の組み合わせを全探索
            // comboType: 0=そのまま, 1=追加注ぎ, 2=混合, 3=混合+追加注ぎ
            double minComboDist = bestDist;
            int comboType = 0;
            int comboTube = -1;
            int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
            double[] comboColor = new double[3];

            // 追加注ぎのみ（bestWellに各チューブを追加した場合を全探索）
            for (int k = 0; k < K; k++) {
                double[] mix = new double[3];
                for (int d = 0; d < 3; d++) mix[d] = (wellColors[bestWell][d] + tubes[k][d]) / 2.0;
                double d2 = colorDist(mix, targets[t]);
                if (d2 < minComboDist) {
                    minComboDist = d2;
                    comboType = 1;
                    comboTube = k;
                    for (int d = 0; d < 3; d++) comboColor[d] = mix[d];
                }
            }

            // 混合のみ or 混合+追加注ぎ（全ウェルペア・全チューブを全探索）
            for (int w1 = 0; w1 < wellCount; w1++) {
                if (wellGrams[w1] < 1.0) continue;
                for (int w2 = 0; w2 < wellCount; w2++) {
                    if (w1 == w2 || wellGrams[w2] < 1.0) continue;
                    for (int i1 = 0; i1 < wellSize; i1++) {
                        for (int j1 = 0; j1 < wellSize; j1++) {
                            int x1 = wellX[w1] + i1;
                            int y1 = wellY[w1] + j1;
                            for (int dxy = 0; dxy < 4; dxy++) {
                                int[] dx = {1, 0, -1, 0};
                                int[] dy = {0, 1, 0, -1};
                                int x2 = x1 + dx[dxy];
                                int y2 = y1 + dy[dxy];
                                // 隣接していなければスキップ
                                if (x2 < 0 || x2 >= N || y2 < 0 || y2 >= N) continue;
                                if (x2 >= wellX[w2] && x2 < wellX[w2] + wellSize &&
                                    y2 >= wellY[w2] && y2 < wellY[w2] + wellSize) {
                                    // 混合のみ
                                    double[] mix = new double[3];
                                    for (int d = 0; d < 3; d++)
                                        mix[d] = (wellColors[w1][d] + wellColors[w2][d]) / 2.0;
                                    double d2 = colorDist(mix, targets[t]);
                                    if (d2 < minComboDist) {
                                        minComboDist = d2;
                                        comboType = 2;
                                        mixW1 = w1; mixW2 = w2;
                                        mixX1 = x1; mixY1 = y1; mixX2 = x2; mixY2 = y2;
                                        for (int d = 0; d < 3; d++) comboColor[d] = mix[d];
                                    }
                                    // 混合+追加注ぎ（混合後さらに各チューブを追加）
                                    for (int k = 0; k < K; k++) {
                                        double[] mixAdd = new double[3];
                                        for (int d = 0; d < 3; d++)
                                            mixAdd[d] = (mix[d] + tubes[k][d]) / 2.0;
                                        double d3 = colorDist(mixAdd, targets[t]);
                                        if (d3 < minComboDist) {
                                            minComboDist = d3;
                                            comboType = 3;
                                            comboTube = k;
                                            mixW1 = w1; mixW2 = w2;
                                            mixX1 = x1; mixY1 = y1; mixX2 = x2; mixY2 = y2;
                                            for (int d = 0; d < 3; d++) comboColor[d] = mixAdd[d];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. 最良の操作を実行
            if (comboType == 0) {
                // そのまま納品
                System.out.println("2 " + wx + " " + wy); // 動作2: 納品
                wellGrams[bestWell] -= 1.0;
            } else if (comboType == 1) {
                // 追加注ぎ
                System.out.println("1 " + wx + " " + wy + " " + comboTube); // 動作1: 追加注ぎ
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = comboColor[d];
                System.out.println("2 " + wx + " " + wy); // 動作2: 納品
                wellGrams[bestWell] += 1.0;
                wellGrams[bestWell] -= 1.0;
            } else if (comboType == 2) {
                // 混合
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2); // 動作4: 混合
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = comboColor[d];
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0;
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]); // 動作2: 納品
                wellGrams[mixW1] -= 1.0;
            } else if (comboType == 3) {
                // 混合＋追加注ぎ
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2); // 動作4: 混合
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = comboColor[d];
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0;
                System.out.println("1 " + wellX[mixW1] + " " + wellY[mixW1] + " " + comboTube); // 動作1: 追加注ぎ
                wellGrams[mixW1] += 1.0;
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]); // 動作2: 納品
                wellGrams[mixW1] -= 1.0;
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
