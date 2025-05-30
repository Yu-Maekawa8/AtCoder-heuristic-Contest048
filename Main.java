/**
 * AHC048 - Mixing on the Palette 
 * 
 * 解法:２本混ぜ最もターゲットの色に近い色を作る
 * 1. 4マス正方形（2x2）のウェルを作る
 * 2. 2本のチューブから色を混ぜて、ターゲットの色に最も近い色を作る
 */

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt(); // パレットのサイズ
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

        // 4マス正方形（2x2）のウェルを作る仕切り配置
        // 縦仕切り：2列ごとに仕切りを上げる
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                System.out.print(((j + 1) % 2 == 0) ? "1" : "0");
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        // 横仕切り：2行ごとに仕切りを上げる
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print(((i + 1) % 2 == 0) ? "1" : "0");
                if (j < N - 1) System.out.print(" ");
            }
            System.out.println();
        }

        // 各ターゲット色ごとに最適な2本混合を探索し、必ずH回「2 0 0」を出力
        for (int i = 0; i < H; i++) {
            int bestA = 0, bestB = 0;
            double bestX = 1.0; // bestAの比率
            double bestDist = Double.MAX_VALUE;

            // 2本のチューブの組み合わせ全探索
            for (int a = 0; a < K; a++) {
                for (int b = 0; b < K; b++) {
                    if (b == a) continue;
                    // x, (1-x)の混合比を0.05刻みで全探索
                    for (double x = 0.0; x <= 1.0; x += 0.05) {
                        double[] mix = new double[3];
                        for (int d = 0; d < 3; d++) {
                            mix[d] = tubes[a][d] * x + tubes[b][d] * (1.0 - x);
                        }
                        double dist = colorDist(mix, targets[i]);
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestA = a;
                            bestB = b;
                            bestX = x;
                        }
                    }
                }
            }
            double y = 1.0 - bestX;
            if (bestX > 1e-6) {
                System.out.println("1 0 0 " + bestA);
            }
            if (y > 1e-6) {
                System.out.println("1 0 0 " + bestB);
            }
            System.out.println("2 0 0");
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
