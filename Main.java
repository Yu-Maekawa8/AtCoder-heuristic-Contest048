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
        int wellSize = 3; // 1ウェルの一辺の長さ
        int wellsPerRow = N / wellSize; // 1行あたりのウェル数（5）
        int wellCount = wellsPerRow * wellsPerRow; // 全ウェル数（25）

        // --- 仕切り状態を管理する配列 ---
        // verticalWalls[x][y] はピクセル(x,y)と(x+1,y)の間の垂直仕切り
        boolean[][] verticalWalls = new boolean[N - 1][N];
        // horizontalWalls[x][y] はピクセル(x,y)と(x,y+1)の間の水平仕切り
        boolean[][] horizontalWalls = new boolean[N][N - 1];

        // 初期仕切り状態を設定
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N - 1; x++) {
                if (((x + 1) % wellSize == 0)) {
                    verticalWalls[x][y] = true;
                }
            }
        }
        for (int y = 0; y < N - 1; y++) {
            for (int x = 0; x < N; x++) {
                if (((y + 1) % wellSize == 0)) {
                    horizontalWalls[x][y] = true;
                }
            }
        }

        // --- 仕切り出力（初期状態）---
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N - 1; x++) {
                System.out.print(verticalWalls[x][y] ? "1" : "0");
                if (x < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        for (int y = 0; y < N - 1; y++) {
            for (int x = 0; x < N; x++) {
                System.out.print(horizontalWalls[x][y] ? "1" : "0");
                if (x < N - 1) System.out.print(" ");
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

        int prevWell = -1;
        int[] wellUsed = new int[wellCount];

        for (int t = 0; t < H; t++) {
            double minDist = Double.MAX_VALUE;
            int opType = -1;
            int bestWell = -1, bestTube = -1;
            int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1;
            double[] bestColor = new double[3];

            // 既存ウェルそのまま納品
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue; // 1グラム未満のウェルは使用不可
                double penalty = (w == prevWell) ? 1.0 : 0.0; // 前回と同じウェルを使用する場合のペナルティ
                penalty += 0.02 * wellUsed[w]; // 使用回数が多いウェルにペナルティ（均等使用を促進）
                double dist = colorDist(wellColors[w], targets[t]) + penalty;
                if (dist < minDist) {
                    minDist = dist;
                    opType = 0;
                    bestWell = w;
                    for (int d = 0; d < 3; d++) bestColor[d] = wellColors[w][d];
                }
            }

            // 追加注ぎ（全ウェル・全チューブ）
            // 1g未満のウェルも対象にする
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1e-8 || wellGrams[w] + 1.0 > wellSize * wellSize) continue; // 0gは後で処理、容量チェック
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

            // 混合・混合＋追加注ぎ
            for (int w1 = 0; w1 < wellCount; w1++) {
                if (wellGrams[w1] < 1.0) continue; // 1グラム未満のウェルは使用不可
                for (int w2 = 0; w2 < wellCount; w2++) {
                    if (w1 == w2 || wellGrams[w2] < 1.0) continue; // 同じウェルは使用不可
                    double total = wellGrams[w1] + wellGrams[w2];
                    if (total > wellSize * wellSize) continue; // 容量チェック

                    // 隣接するウェルのみを考慮（効率的な混合のため）
                    boolean isAdjacent = false;
                    for (int i1 = 0; i1 < wellSize && !isAdjacent; i1++) {
                        for (int j1 = 0; j1 < wellSize && !isAdjacent; j1++) {
                            int x1_pixel = wellX[w1] + i1;
                            int y1_pixel = wellY[w1] + j1;
                            for (int i2 = 0; i2 < wellSize && !isAdjacent; i2++) {
                                for (int j2 = 0; j2 < wellSize && !isAdjacent; j2++) {
                                    int x2_pixel = wellX[w2] + i2;
                                    int y2_pixel = wellY[w2] + j2;
                                    if (Math.abs(x1_pixel - x2_pixel) + Math.abs(y1_pixel - y2_pixel) == 1) { // 隣接判定
                                        isAdjacent = true;
                                        // 混合
                                        double[] mix = new double[3];
                                        for (int d = 0; d < 3; d++)
                                            mix[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / total;
                                        double dist = colorDist(mix, targets[t]);
                                        if (dist < minDist) {
                                            minDist = dist;
                                            opType = 2;
                                            mixW1 = w1; mixW2 = w2;
                                            mixX1 = x1_pixel; mixY1 = y1_pixel; mixX2 = x2_pixel; mixY2 = y2_pixel;
                                            for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                                        }

                                        // 混合＋追加注ぎ
                                        for (int k_tube = 0; k_tube < K; k_tube++) {
                                            if (total + 1.0 > wellSize * wellSize) continue; // 容量チェック
                                            double[] mixAdd = new double[3];
                                            for (int d_rgb = 0; d_rgb < 3; d_rgb++)
                                                mixAdd[d_rgb] = (mix[d_rgb] * total + tubes[k_tube][d_rgb]) / (total + 1.0);
                                            double dist2 = colorDist(mixAdd, targets[t]);
                                            if (dist2 < minDist) {
                                                minDist = dist2;
                                                opType = 3;
                                                bestTube = k_tube;
                                                mixW1 = w1; mixW2 = w2;
                                                mixX1 = x1_pixel; mixY1 = y1_pixel; mixX2 = x2_pixel; mixY2 = y2_pixel;
                                                for (int d_rgb = 0; d_rgb < 3; d_rgb++) bestColor[d_rgb] = mixAdd[d_rgb];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 空きウェルの活用（他の操作が失敗した場合のフォールバック）
            if (opType == -1) {
                for (int w = 0; w < wellCount; w++) {
                    if (wellGrams[w] < 1e-8) { // 空きウェルを探す
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
                            opType = 1;
                            bestWell = w;
                            bestTube = bestTubeIdx;
                            for (int d = 0; d < 3; d++) bestColor[d] = tubes[bestTubeIdx][d];
                        }
                    }
                }
            }

            // 操作の実行
            if (opType == 0) {
                // そのまま納品
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 1) {
                // 追加注ぎ
                System.out.println("1 " + wellX[bestWell] + " " + wellY[bestWell] + " " + bestTube);
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestColor[d];
                wellGrams[bestWell] += 1.0;
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 2) {
                // 混合
                // 外す仕切りの座標を特定し、状態を更新
                int wallX = -1, wallY = -1; // 外した仕切りの座標
                boolean isVerticalWall = false;
                if (mixY1 == mixY2) { // 同じ行 -> 垂直仕切り
                    wallX = Math.min(mixX1, mixX2);
                    wallY = mixY1;
                    verticalWalls[wallX][wallY] = false;
                    isVerticalWall = true;
                } else { // 同じ列 -> 水平仕切り
                    wallX = mixX1;
                    wallY = Math.min(mixY1, mixY2);
                    horizontalWalls[wallX][wallY] = false;
                    isVerticalWall = false;
                }
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);

                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = bestColor[d];
                wellGrams[mixW1] += wellGrams[mixW2];
                double originalW2Grams = wellGrams[mixW2]; // 元のw2のグラム数を保持（仕切りを戻す際に使用）
                wellGrams[mixW2] = 0.0;

                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0;
                prevWell = mixW1;
                wellUsed[mixW1]++;

                System.err.println("[DEBUG opType 2] Before wall return check - Turn: " + t + ", wellGrams[mixW1]: " + wellGrams[mixW1] + ", wellCapacity: " + (wellSize * wellSize));
                // --- 仕切りを戻す処理 ---
                // 残ったグラム数がウェル容量の半分未満で、かつ操作回数に余裕がある場合
                if (wellGrams[mixW1] < (double)(wellSize * wellSize) / 2.0 && wellGrams[mixW1] >= 0.0 && t < H - 20 ) { // 条件変更
                    System.err.println("[DEBUG] Attempting to return wall for opType 2. Turn: " + t);
                    System.err.println("  - mixW1: " + mixW1 + ", mixW2: " + mixW2);
                    System.err.println("  - Pixels: (" + mixX1 + "," + mixY1 + ") <-> (" + mixX2 + "," + mixY2 + ")");
                    System.err.println("  - isVerticalWall: " + isVerticalWall + ", wallX: " + wallX + ", wallY: " + wallY);
                    System.err.println("  - wellGrams[mixW1] (before split): " + wellGrams[mixW1]);
                    System.err.println("  - originalW2Grams: " + originalW2Grams);

                    System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);
                    if (isVerticalWall) {
                        verticalWalls[wallX][wallY] = true;
                    } else {
                        horizontalWalls[wallX][wallY] = true;
                    }
                    // 絵の具を分割して戻す (例: 半分ずつ)
                    // ただし、元のw2が空だった場合はw1に全て残す
                    if (originalW2Grams > 1e-8) { // 元のw2に絵の具があった場合
                        double remainingGrams = wellGrams[mixW1];
                        if (remainingGrams >= 0.0) { // 0以上の場合のみ分割
                           wellGrams[mixW1] = remainingGrams / 2.0;
                           wellGrams[mixW2] = remainingGrams / 2.0;
                           // 色もw2に戻す
                           for (int d = 0; d < 3; d++) {
                               wellColors[mixW2][d] = wellColors[mixW1][d];
                           }
                           System.err.println("  - Split successful. wellGrams[mixW1]: " + wellGrams[mixW1] + ", wellGrams[mixW2]: " + wellGrams[mixW2]);
                        } else {
                           System.err.println("  - Split skipped: remainingGrams < 0");
                        }
                    } else {
                        System.err.println("  - Split skipped: originalW2Grams was ~0.");
                    }
                }

            } else if (opType == 3) {
                // 混合＋追加注ぎ
                // 外す仕切りの座標を特定し、状態を更新
                int wallX = -1, wallY = -1; // 外した仕切りの座標
                boolean isVerticalWall = false;
                if (mixY1 == mixY2) { // 同じ行 -> 垂直仕切り
                    wallX = Math.min(mixX1, mixX2);
                    wallY = mixY1;
                    verticalWalls[wallX][wallY] = false;
                    isVerticalWall = true;
                } else { // 同じ列 -> 水平仕切り
                    wallX = mixX1;
                    wallY = Math.min(mixY1, mixY2);
                    horizontalWalls[wallX][wallY] = false;
                    isVerticalWall = false;
                }
                System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);

                // 混合による色とグラムの更新
                double currentW1Grams = wellGrams[mixW1]; // 混合前のw1のグラム数
                double currentW2Grams = wellGrams[mixW2]; // 混合前のw2のグラム数
                double mixedTotalGrams = currentW1Grams + currentW2Grams;
                double[] tempMixedColor = new double[3]; // bestColorを上書きしないように一時変数を使用
                for (int d = 0; d < 3; d++) {
                     tempMixedColor[d] = (wellColors[mixW1][d] * currentW1Grams + wellColors[mixW2][d] * currentW2Grams) / mixedTotalGrams;
                }

                wellGrams[mixW1] = mixedTotalGrams;
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = tempMixedColor[d]; // 混合後の色をw1に設定
                double originalW2GramsForRestore = currentW2Grams; // 元のw2のグラム数を保持
                wellGrams[mixW2] = 0.0;

                // 追加注ぎの処理
                System.out.println("1 " + wellX[mixW1] + " " + wellY[mixW1] + " " + bestTube);
                // bestColorは「混合後に追加注ぎした後の理想の色」
                // wellColors[mixW1]とwellGrams[mixW1]を更新
                double gramsBeforeAddingTube = wellGrams[mixW1];
                for (int d = 0; d < 3; d++) {
                    wellColors[mixW1][d] = (wellColors[mixW1][d] * gramsBeforeAddingTube + tubes[bestTube][d] * 1.0) / (gramsBeforeAddingTube + 1.0);
                }
                wellGrams[mixW1] += 1.0;


                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0;
                prevWell = mixW1;
                wellUsed[mixW1]++;

                System.err.println("[DEBUG opType 3] Before wall return check - Turn: " + t + ", wellGrams[mixW1]: " + wellGrams[mixW1] + ", wellCapacity: " + (wellSize * wellSize));
                // --- 仕切りを戻す処理 ---
                if (wellGrams[mixW1] < (double)(wellSize * wellSize) / 2.0 && wellGrams[mixW1] >= 0.0 && t < H - 20) { // 条件変更
                    System.err.println("[DEBUG] Attempting to return wall for opType 3. Turn: " + t);
                    System.err.println("  - mixW1: " + mixW1 + ", mixW2: " + mixW2);
                    System.err.println("  - Pixels: (" + mixX1 + "," + mixY1 + ") <-> (" + mixX2 + "," + mixY2 + ")");
                    System.err.println("  - isVerticalWall: " + isVerticalWall + ", wallX: " + wallX + ", wallY: " + wallY);
                    System.err.println("  - wellGrams[mixW1] (before split): " + wellGrams[mixW1]);
                    System.err.println("  - originalW2GramsForRestore: " + originalW2GramsForRestore);

                    System.out.println("4 " + mixX1 + " " + mixY1 + " " + mixX2 + " " + mixY2);
                    if (isVerticalWall) {
                        verticalWalls[wallX][wallY] = true;
                    } else {
                        horizontalWalls[wallX][wallY] = true;
                    }
                    if (originalW2GramsForRestore > 1e-8) {
                        double remainingGrams = wellGrams[mixW1];
                        if (remainingGrams >= 0.0) {
                            wellGrams[mixW1] = remainingGrams / 2.0;
                            wellGrams[mixW2] = remainingGrams / 2.0;
                            for (int d = 0; d < 3; d++) {
                                wellColors[mixW2][d] = wellColors[mixW1][d];
                            }
                            System.err.println("  - Split successful. wellGrams[mixW1]: " + wellGrams[mixW1] + ", wellGrams[mixW2]: " + wellGrams[mixW2]);
                        } else {
                            System.err.println("  - Split skipped: remainingGrams < 0");
                        }
                    } else {
                        System.err.println("  - Split skipped: originalW2GramsForRestore was ~0.");
                    }
                }
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