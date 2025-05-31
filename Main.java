/**
 * AHC048 - Mixing on the Palette
 * 
 * 5×5のウェル（wellSize=4, N=20で25個）に分割し、
 * まず全てのウェルに色を1gずつ入れてから調整を始める。
 * 
 * 各ターゲット色ごとに：
 *   1. 全ウェルから最も近い色を探索し、1g以上あればそのまま渡す。
 *   2. 追加注ぎ（動作1）で近づく場合は追加して渡す。
 *   3. 動作4（隣接マス間の仕切りを外して混ぜる）で近づく場合は混ぜて渡す。
 *   4. それでもダメなら一番近いウェルを上書きして渡す。
 * 
 * 各ウェルのグラム数を管理し、1g未満の場合は渡せない・混ぜられないようにしている。
 * 動作4はマス単位で隣接判定し、正しく混合できるようにしている。
 */

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt(); // パレットの一辺(20 固定)
        int K = sc.nextInt(); // 絵の具の種類数
        int H = sc.nextInt(); // ターゲット色の数(1000 固定)
        int T = sc.nextInt(); // 最大ターン数
        int D = sc.nextInt(); // 1グラム出すコストD

        // 絵の具の色（各RGB成分）
        double[][] tubes = new double[K][3];
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        // ターゲット色
        double[][] targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // --- パレットを5×5のウェルに分割（wellSize=4, N=20で25個） ---
        int wellSize = 4;
        int wellsPerRow = N / wellSize; // 5
        int wellCount = wellsPerRow * wellsPerRow; // 25

        // 仕切り出力（5×5分割）
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

        // --- まず全てのウェルに色を入れる ---
        double[][] wellColors = new double[wellCount][3];
        int[] wellX = new int[wellCount];
        int[] wellY = new int[wellCount];
        double[] wellGrams = new double[wellCount]; // 各ウェルのグラム数を管理
        int idx = 0;
        for (int wy = 0; wy < wellsPerRow; wy++) {
            for (int wx = 0; wx < wellsPerRow; wx++) {
                int x = wx * wellSize;
                int y = wy * wellSize;
                int tubeIdx = idx % K;
                // 各ウェルの左上に1gずつ色を入れる（動作1）
                System.out.println("1 " + x + " " + y + " " + tubeIdx);
                for (int d = 0; d < 3; d++) wellColors[idx][d] = tubes[tubeIdx][d];
                wellX[idx] = x;
                wellY[idx] = y;
                wellGrams[idx] = 1.0; // 最初に1g入れる
                idx++;
            }
        }

        // --- 全ウェルに色を入れ終わってから調整開始 ---
        for (int t = 0; t < H; t++) {
            // 25個すべてのウェルを全探索して最も近い色を記録
            int bestWell = -1;
            double bestDist = Double.MAX_VALUE;
            for (int w = 0; w < wellCount; w++) {
                double dist = colorDist(wellColors[w], targets[t]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestWell = w;
                }
            }
            int wx = wellX[bestWell];
            int wy = wellY[bestWell];

            // かなり近い場合は即渡す（1g未満は渡せない）
            if (bestDist < 0.003 && wellGrams[bestWell] >= 1.0) {
                // 動作2：納品
                System.out.println("2 " + wx + " " + wy);
                wellGrams[bestWell] -= 1.0;
                continue;
            }

            // 追加注ぎ（全チューブ）でさらに近づける場合を全探索
            int bestTube = -1;
            double minTubeDist = Double.MAX_VALUE;
            double[] bestTubeMix = new double[3];
            for (int k = 0; k < K; k++) {
                // 動作1：追加注ぎ
                double[] mix = new double[3];
                for (int d = 0; d < 3; d++) mix[d] = (wellColors[bestWell][d] + tubes[k][d]) / 2.0;
                double d2 = colorDist(mix, targets[t]);
                if (d2 < minTubeDist) {
                    minTubeDist = d2;
                    bestTube = k;
                    for (int d = 0; d < 3; d++) bestTubeMix[d] = mix[d];
                }
            }
            if (minTubeDist < bestDist) {
                // 追加注ぎで近づくなら追加して渡す
                System.out.println("1 " + wx + " " + wy + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestTubeMix[d];
                System.out.println("2 " + wx + " " + wy);
                continue;
            }

            // 動作4: 全ての隣接マスで異なるウェル同士を全探索
            int bestMixWell1 = -1, bestMixWell2 = -1;
            double bestMixDist = Double.MAX_VALUE;
            double[] bestMixColor = new double[3];
            int mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
            boolean bestMixIsAdd = false;
            int bestMixAddTube = -1;
            double[] bestMixAddColor = new double[3];

            for (int w1 = 0; w1 < wellCount; w1++) {
                for (int w2 = 0; w2 < wellCount; w2++) {
                    if (w1 == w2) continue;
                    // 各ウェルの4x4マスを全探索
                    for (int i1 = 0; i1 < wellSize; i1++) {
                        for (int j1 = 0; j1 < wellSize; j1++) {
                            int x1 = wellX[w1] + i1;
                            int y1 = wellY[w1] + j1;
                            for (int dxy = 0; dxy < 4; dxy++) {
                                int[] dx = {1, 0, -1, 0};
                                int[] dy = {0, 1, 0, -1};
                                int x2 = x1 + dx[dxy];
                                int y2 = y1 + dy[dxy];
                                // 範囲外は除外
                                if (x2 < 0 || x2 >= N || y2 < 0 || y2 >= N) continue;
                                // x2,y2がw2の範囲内か
                                if (x2 >= wellX[w2] && x2 < wellX[w2] + wellSize &&
                                    y2 >= wellY[w2] && y2 < wellY[w2] + wellSize) {
                                    // 動作4：仕切りを外して混ぜるだけ
                                    double[] mix = new double[3];
                                    for (int d = 0; d < 3; d++)
                                        mix[d] = (wellColors[w1][d] + wellColors[w2][d]) / 2.0;
                                    double d2 = colorDist(mix, targets[t]);
                                    if (d2 < bestMixDist) {
                                        bestMixDist = d2;
                                        bestMixWell1 = w1;
                                        bestMixWell2 = w2;
                                        for (int d = 0; d < 3; d++) bestMixColor[d] = mix[d];
                                        mixX1 = x1;
                                        mixY1 = y1;
                                        mixX2 = x2;
                                        mixY2 = y2;
                                        bestMixIsAdd = false;
                                    }
                                    // さらに、混ぜた後に追加注ぎして近づける場合も探索
                                    for (int k = 0; k < K; k++) {
                                        double[] mixAdd = new double[3];
                                        for (int d = 0; d < 3; d++)
                                            mixAdd[d] = (mix[d] + tubes[k][d]) / 2.0;
                                        double d3 = colorDist(mixAdd, targets[t]);
                                        if (d3 < bestMixDist) {
                                            bestMixDist = d3;
                                            bestMixWell1 = w1;
                                            bestMixWell2 = w2;
                                            for (int d = 0; d < 3; d++) bestMixAddColor[d] = mixAdd[d];
                                            mixX1 = x1;
                                            mixY1 = y1;
                                            mixX2 = x2;
                                            mixY2 = y2;
                                            bestMixIsAdd = true;
                                            bestMixAddTube = k;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 混ぜるだけ or 混ぜて追加注ぎ、どちらが良いかで分岐
            if (bestMixDist < bestDist && wellGrams[bestMixWell1] >= 1.0 && wellGrams[bestMixWell2] >= 1.0) {
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);
                if (bestMixIsAdd) {
                    // 混ぜた後さらに追加注ぎ
                    System.out.println("1 " + mixX1 + " " + mixY1 + " " + bestMixAddTube);
                    for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestMixAddColor[d];
                } else {
                    for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestMixColor[d];
                }
                wellGrams[bestMixWell1] -= 0.5;
                wellGrams[bestMixWell2] -= 0.5;
                wellGrams[bestWell] += 1.0; // 混ぜて1g増やすイメージ
                System.out.println("2 " + mixX1 + " " + mixY1);
                wellGrams[bestWell] -= 1.0;
                continue;
            }

            // どちらもダメなら一番近いウェルを上書きして渡す
            bestTube = 0;
            minTubeDist = Double.MAX_VALUE;
            for (int k = 0; k < K; k++) {
                double dist = colorDist(tubes[k], targets[t]);
                if (dist < minTubeDist) {
                    minTubeDist = dist;
                    bestTube = k;
                }
            }
            System.out.println("1 " + wx + " " + wy + " " + bestTube);
            for (int d = 0; d < 3; d++) wellColors[bestWell][d] = tubes[bestTube][d];
            System.out.println("2 " + wx + " " + wy);
        }
        sc.close();
    }

    // 2色間のユークリッド距離を返す（色差の計算）
    static double colorDist(double[] c1, double[] c2) {
        double dc = c1[0] - c2[0];
        double dm = c1[1] - c2[1];
        double dy = c1[2] - c2[2];
        return Math.sqrt(dc * dc + dm * dm + dy * dy);
    }
}
