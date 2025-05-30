/**
 * AHC048 - Mixing on the Palette 
 * 
 * 解法:３本混ぜ最もターゲットの色に近い色を作る
 * 1. 仕切りを全部下げて連結ウェルを1つにする
 * 2. 3本のチューブから色を混ぜて、ターゲットの色に最も近い色を作る
 */

import java.util.Scanner;

public class Main {
    static final int N = 20; // パレットのサイズ（20x20）

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int K = sc.nextInt(); // チューブの種類数
        int H = sc.nextInt(); // 作るべき色の数
        int T = sc.nextInt(); // 操作回数上限（未使用）
        int D = sc.nextInt(); // 1g使うごとのコスト（未使用）

        double[][] tubes = new double[K][3]; // チューブの色（C, M, Y）
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        double[][] targets = new double[H][3]; // ターゲット色（C, M, Y）
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // 仕切りをすべて下げて、パレット全体を1つの大きなウェルにする
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                System.out.print("0");
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print("0");
                if (j < N - 1) System.out.print(" ");
            }
            System.out.println();
        }

        // 各ターゲット色ごとに最適な3本混合を探索
        for (int i = 0; i < H; i++) {
            int bestA = 0, bestB = 0, bestC = 0; // 最適なチューブのインデックス
            double bestX = 1.0, bestY = 0.0;     // 最適な混合比率（x, y, z=1-x-y）
            double bestDist = Double.MAX_VALUE;   // 最小距離（色の誤差）

            // 3本のチューブの組み合わせ全探索
            for (int a = 0; a < K; a++) {
                for (int b = 0; b < K; b++) {
                    if (b == a) continue;
                    for (int c = 0; c < K; c++) {
                        if (c == a || c == b) continue;
                        // x, y, zの混合比を0.05刻みで全探索
                        for (double x = 0.0; x <= 1.0; x += 0.05) {
                            for (double y = 0.0; y <= 1.0 - x; y += 0.05) {
                                double z = 1.0 - x - y;
                                if (z < -1e-8) continue; // zが負ならスキップ
                                double[] mix = new double[3];
                                // 混ぜた色を計算
                                for (int d = 0; d < 3; d++) {
                                    mix[d] = tubes[a][d] * x + tubes[b][d] * y + tubes[c][d] * z;
                                }
                                // ターゲット色との距離を計算
                                double dist = colorDist(mix, targets[i]);
                                // より近ければ記録
                                if (dist < bestDist) {
                                    bestDist = dist;
                                    bestA = a;
                                    bestB = b;
                                    bestC = c;
                                    bestX = x;
                                    bestY = y;
                                }
                            }
                        }
                    }
                }
            }
            double z = 1.0 - bestX - bestY; // 3本目の比率
            // それぞれの比率が十分大きければ注ぐ
            if (bestX > 1e-6) {
                System.out.println("1 0 0 " + bestA); // bestAのチューブから1g注ぐ
            }
            if (bestY > 1e-6) {
                System.out.println("1 0 0 " + bestB); // bestBのチューブから1g注ぐ
            }
            if (z > 1e-6) {
                System.out.println("1 0 0 " + bestC); // bestCのチューブから1g注ぐ
            }
            System.out.println("2 0 0"); // 1g取り出して画伯に渡す
        }
        sc.close();
    }

    // 2つの色ベクトル間のユークリッド距離を返す
    static double colorDist(double[] c1, double[] c2) {
        double dc = c1[0] - c2[0];
        double dm = c1[1] - c2[1];
        double dy = c1[2] - c2[2];
        return Math.sqrt(dc * dc + dm * dm + dy * dy);
    }
}
