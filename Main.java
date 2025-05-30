/**
 * AHC048 - Mixing on the Palette 
 * 
 * 戦略: 全仕切り下げ＋TSP順序最適化
 * 
 * 1. 仕切りを全て下げて1つの大ウェルにする
 *    → パレット全体を1つの大きな混色エリアとして使う。
 *    → 仕切り出力は全て"0"（下げる）で出力。
 * 
 * 2. ターゲット色の順序をTSP（巡回セールスマン問題）近似（Nearest Neighbor法＋2-opt）で最適化
 *    → まず最初の色を0番に決め、以降は直前の色に最も近い色を貪欲法で選ぶ（Nearest Neighbor法）
 *    → さらに2-opt法で順序を改善し、全体の色移動距離（色差）を短縮
 * 
 * 3. 直前の色を最大限再利用し、操作回数・誤差を抑える
 *    → 直前に作った色が十分近ければ、そのまま渡す（注ぎ操作を省略）
 *    → 1本注ぎで十分近い場合は1本だけ注ぐ
 *    → どちらもダメなら2本混ぜで最適な比率を探索
 * 
 * 4. 1本注ぎ・2本混ぜでターゲット色を作る
 *    → 2本混ぜは0.01刻みで全探索し、最も近い色を作る
 *    → 操作ごとに"1 x y t"（注ぎ）や"2 x y"（渡す）を出力
 */

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt(); // パレットの一辺
        int K = sc.nextInt(); // 絵の具の種類数
        int H = sc.nextInt(); // ターゲット色の数
        int T = sc.nextInt(); // 制限時間
        int D = sc.nextInt(); // 許容誤差

        double[][] tubes = new double[K][3]; // 絵の具の色（各RGB成分）
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        double[][] targets = new double[H][3]; // ターゲット色
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // --- 全仕切り下げ ---
        // パレットの全ての仕切りを下げて1つの大ウェルにする
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

        // --- TSP順序最適化（Nearest Neighbor法＋2-opt改善） ---
        // まずNearest Neighbor法で初期順序を作成
        boolean[] used = new boolean[H];
        int[] order = new int[H];
        order[0] = 0; // 最初は0番目のターゲット色
        used[0] = true;
        for (int i = 1; i < H; i++) {
            int prev = order[i - 1];
            double minDist = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < H; j++) {
                if (used[j]) continue;
                double dist = colorDist(targets[prev], targets[j]);
                if (dist < minDist) {
                    minDist = dist;
                    next = j;
                }
            }
            order[i] = next;
            used[next] = true;
        }

        // 2-opt法で順序をさらに改善（局所最適化）
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < H - 2; i++) {
                for (int j = i + 1; j < H - 1; j++) {
                    double before = colorDist(targets[order[i - 1]], targets[order[i]])
                                  + colorDist(targets[order[j]], targets[order[j + 1]]);
                    double after  = colorDist(targets[order[i - 1]], targets[order[j]])
                                  + colorDist(targets[order[i]], targets[order[j + 1]]);
                    if (after < before) {
                        // order[i]～order[j]を反転
                        for (int l = 0; l < (j - i + 1) / 2; l++) {
                            int tmp = order[i + l];
                            order[i + l] = order[j - l];
                            order[j - l] = tmp;
                        }
                        improved = true;
                    }
                }
            }
        }

        // --- 1つの大ウェル(0,0)で順に色を作る ---
        double[] lastColor = null; // 直前に作った色
        for (int idx = 0; idx < H; idx++) {
            int i = order[idx];
            int wxIdx = 0, wyIdx = 0; // 常に(0,0)の大ウェルを使う

            // 1本注ぎ or 既存色再利用
            double bestDist = Double.MAX_VALUE;
            int bestTube = -1;
            boolean reused = false;
            // 直前の色が十分近ければ再利用
            if (lastColor != null) {
                double dist = colorDist(lastColor, targets[i]);
                if (dist < bestDist) {
                    bestDist = dist;
                    reused = true;
                }
            }
            // 1本注ぎで十分近い色があれば選ぶ
            for (int a = 0; a < K; a++) {
                double dist = colorDist(tubes[a], targets[i]);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestTube = a;
                    reused = false;
                }
            }
            // 1本注ぎまたは再利用で十分近い場合
            if (bestDist < 0.01) {
                if (!reused) {
                    // 1本注ぎ
                    System.out.println("1 " + wxIdx + " " + wyIdx + " " + bestTube);
                    lastColor = tubes[bestTube].clone();
                }
                // 色を渡す
                System.out.println("2 " + wxIdx + " " + wyIdx);
                continue;
            }

            // 2本混ぜ（0.01刻みで全探索）
            int bestA = 0, bestB = 0;
            double bestX = 1.0;
            bestDist = Double.MAX_VALUE;
            for (int a = 0; a < K; a++) {
                for (int b = 0; b < K; b++) {
                    if (b == a) continue;
                    for (double x = 0.0; x <= 1.0; x += 0.01) {
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
            // 2本混ぜで注ぐ
            if (bestX > 1e-6) {
                System.out.println("1 " + wxIdx + " " + wyIdx + " " + bestA);
            }
            if (y > 1e-6) {
                System.out.println("1 " + wxIdx + " " + wyIdx + " " + bestB);
            }
            // 色を渡す
            System.out.println("2 " + wxIdx + " " + wyIdx);
            // 直前の色を更新
            double[] newColor = new double[3];
            for (int d = 0; d < 3; d++) {
                newColor[d] = tubes[bestA][d] * bestX + tubes[bestB][d] * y;
            }
            lastColor = newColor;
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
