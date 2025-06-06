import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class Main {

    // クラスのフィールドとして壁の状態管理配列を定義
    static boolean[][] verticalWalls;   // verticalWalls[x][y] は (x,y)と(x+1,y)の間の垂直の壁の有無 (true: 壁あり, false: 壁なし)
    static boolean[][] horizontalWalls; // horizontalWalls[x][y] は (x,y)と(x,y+1)の間の水平の壁の有無 (true: 壁あり, false: 壁なし)
    static int N_GLOBAL; // Nをstaticで保持するため

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int N = sc.nextInt(); // パレットの一辺(20 固定)
        int K = sc.nextInt(); // 絵の具の種類数
        int H = sc.nextInt(); // ターゲット色の数(1000 固定)
        int T_UNUSED = sc.nextInt(); // 最大ターン数（未使用）
        int D_UNUSED = sc.nextInt(); // 1グラム出すコストD（未使用）
        N_GLOBAL = N; // Nをstatic変数に保持

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

        // --- パレットを2x2のウェルに分割（wellSize=2, N=20で49個） ---
        int wellSize = 2; // 1ウェルの一辺の長さ
        int wellsPerRow = (N - 6) / wellSize; // 1行あたりのウェル数（7）
        int wellCount = wellsPerRow * wellsPerRow; // 全ウェル数（49）

        // 壁の状態管理配列の初期化
        verticalWalls = new boolean[N][N - 1];   // (x,y)と(x+1,y)の間の壁
        horizontalWalls = new boolean[N - 1][N]; // (x,y)と(x,y+1)の間の壁

        // --- 仕切り出力（2x2分割） & 壁状態の初期化 ---
        // 垂直方向の仕切り
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                if ((j + 1) % wellSize == 0) { // ウェルの境界線に当たる場合
                    System.out.print("1");
                    verticalWalls[i][j] = true; // 壁が存在する
                } else {
                    System.out.print("0");
                    verticalWalls[i][j] = false; // 壁が存在しない
                }
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        // 水平方向の仕切り
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                if ((i + 1) % wellSize == 0) { // ウェルの境界線に当たる場合
                    System.out.print("1");
                    horizontalWalls[i][j] = true; // 壁が存在する
                } else {
                    System.out.print("0");
                    horizontalWalls[i][j] = false; // 壁が存在しない
                }
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

        int prevWell = -1;
        int[] wellUsed = new int[wellCount];

        for (int t = 0; t < H; t++) {
            // 50ターンごとに全ての開いているウェル境界の壁を再構築（入れる）
            if (t > 0 && t % 50 == 0) {
                // 垂直の壁をチェック
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N - 1; j++) {
                        // ウェルを分ける垂直の壁であり、現在開いている場合
                        if (((j + 1) % wellSize == 0) && !verticalWalls[i][j]) {
                            toggleWall(i, j, i, j + 1); // 壁を構築 (状態がfalseなのでtrueになる)
                        }
                    }
                }
                // 水平の壁をチェック
                for (int i = 0; i < N - 1; i++) {
                    for (int j = 0; j < N; j++) {
                        // ウェルを分ける水平の壁であり、現在開いている場合
                        if (((i + 1) % wellSize == 0) && !horizontalWalls[i][j]) {
                            toggleWall(i, j, i + 1, j); // 壁を構築 (状態がfalseなのでtrueになる)
                        }
                    }
                }
            }

            double minDist = Double.MAX_VALUE;
            int opType = -1; // 0:納品, 1:追加注ぎ, 2:混合, 3:混合+追加注ぎ
            int bestWell = -1, bestTube = -1;
            int mixW1 = -1, mixW2 = -1, mixX1 = -1, mixY1 = -1, mixX2 = -1, mixY2 = -1; // 混合操作用のウェルインデックスとピクセル座標
            double[] bestColor = new double[3]; // 最適な操作後の色

            // 1. 既存ウェルそのまま納品 (opType=0)
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue; // 1グラム未満のウェルは使用不可
                
                double penalty = (w == prevWell) ? 0.05 : 0.0; // 前回と同じウェルを使用する場合のペナルティを低減
                penalty += 0.01 * wellUsed[w]; // 使用回数が多いウェルにペナルティ（均等使用を促進）

                double dist = colorDist(wellColors[w], targets[t]) + penalty;
                if (dist < minDist) {
                    minDist = dist;
                    opType = 0;
                    bestWell = w;
                    for (int d = 0; d < 3; d++) bestColor[d] = wellColors[w][d];
                }
            }

            // 2. 追加注ぎ (opType=1) (既存ウェルにチューブを追加)
            for (int w = 0; w < wellCount; w++) {
                if (wellGrams[w] < 1.0) continue; // 1グラム未満のウェルは操作対象外
                if (wellGrams[w] + 1.0 > wellSize * wellSize + 1e-8) continue; // 容量オーバーを防ぐ (誤差考慮)

                for (int k = 0; k < K; k++) {
                    double total = wellGrams[w] + 1.0;
                    double[] mix = new double[3];
                    for (int d = 0; d < 3; d++)
                        mix[d] = (wellColors[w][d] * wellGrams[w] + tubes[k][d]) / total; // 重み付き平均で色を計算
                    
                    double dist = colorDist(mix, targets[t]); // Dコストはここでは評価に含めない (コマンド時に発生)
                    
                    if (dist < minDist) {
                        minDist = dist;
                        opType = 1;
                        bestWell = w;
                        bestTube = k;
                        for (int d = 0; d < 3; d++) bestColor[d] = mix[d];
                    }
                }
            }

            // 3. 混合 (opType=2) および 4. 混合＋追加注ぎ (opType=3)
            for (int w1 = 0; w1 < wellCount; w1++) {
                if (wellGrams[w1] < 1.0) continue; // 1グラム未満のウェルは操作不可
                for (int w2 = 0; w2 < wellCount; w2++) {
                    if (w1 == w2 || wellGrams[w2] < 1.0) continue; // 同じウェルは使用不可、または2番目のウェルが1グラム未満

                    double totalGrams = wellGrams[w1] + wellGrams[w2];
                    if (totalGrams > wellSize * wellSize + 1e-8) continue; // 容量オーバーチェック (誤差考慮)

                    // 隣接するウェルのみを考慮 (このコードの意図通り)
                    // 隣接するウェル間の壁の座標と、壁が存在するかどうかを確認
                    int targetWallX1 = -1, targetWallY1 = -1, targetWallX2 = -1, targetWallY2 = -1;
                    boolean isVerticalWall = false;
                    boolean isHorizontalWall = false;

                    // ウェル1とウェル2の相対位置から壁の座標を特定
                    int wx1 = wellX[w1] / wellSize; // ウェル1のグリッドX
                    int wy1 = wellY[w1] / wellSize; // ウェル1のグリッドY
                    int wx2 = wellX[w2] / wellSize; // ウェル2のグリッドX
                    int wy2 = wellY[w2] / wellSize; // ウェル2のグリッドY

                    if (Math.abs(wx1 - wx2) == 1 && wy1 == wy2) { // 垂直方向で隣接
                        isVerticalWall = true;
                        targetWallX1 = Math.min(wellX[w1], wellX[w2]) + wellSize -1; // 小さい方のXのウェルの右端ピクセル
                        targetWallY1 = wellY[w1]; // Y座標は同じ
                        targetWallX2 = targetWallX1 + 1;
                        targetWallY2 = wellY[w1];
                    } else if (Math.abs(wy1 - wy2) == 1 && wx1 == wx2) { // 水平方向で隣接
                        isHorizontalWall = true;
                        targetWallX1 = wellX[w1]; // X座標は同じ
                        targetWallY1 = Math.min(wellY[w1], wellY[w2]) + wellSize -1; // 小さい方のYのウェルの下端ピクセル
                        targetWallX2 = wellX[w1];
                        targetWallY2 = targetWallY1 + 1;
                    } else { // 隣接していない場合はスキップ
                        continue;
                    }
                    
                    // 混合後の色を計算
                    double[] mixColor = new double[3];
                    for (int d = 0; d < 3; d++)
                        mixColor[d] = (wellColors[w1][d] * wellGrams[w1] + wellColors[w2][d] * wellGrams[w2]) / totalGrams;
                    
                    // 混合操作 (opType=2)
                    double distMix = colorDist(mixColor, targets[t]);
                    if (distMix < minDist) {
                        minDist = distMix;
                        opType = 2;
                        mixW1 = w1; mixW2 = w2;
                        mixX1 = targetWallX1; mixY1 = targetWallY1; mixX2 = targetWallX2; mixY2 = targetWallY2;
                        for (int d = 0; d < 3; d++) bestColor[d] = mixColor[d];
                    }

                    // 混合＋追加注ぎ (opType=3)
                    for (int k = 0; k < K; k++) {
                        if (totalGrams + 1.0 > wellSize * wellSize + 1e-8) continue; // 容量チェック

                        double[] mixAdd = new double[3];
                        for (int d = 0; d < 3; d++)
                            mixAdd[d] = (mixColor[d] * totalGrams + tubes[k][d]) / (totalGrams + 1.0);
                        
                        double distMixAdd = colorDist(mixAdd, targets[t]);
                        if (distMixAdd < minDist) {
                            minDist = distMixAdd;
                            opType = 3;
                            bestTube = k;
                            mixW1 = w1; mixW2 = w2;
                            mixX1 = targetWallX1; mixY1 = targetWallY1; mixX2 = targetWallX2; mixY2 = targetWallY2;
                            for (int d = 0; d < 3; d++) bestColor[d] = mixAdd[d];
                        }
                    }
                }
            }

            // 4. 空きウェルへの注ぎ込み (opType=1) (常に評価されるように昇格)
            // このループは上記2.の追加注ぎのループとは異なる目的で、
            // 「空のウェルにチューブを注ぐことで、そのウェルを新しい作業スペースとして使う」ことを評価する。
            for (int w = 0; w < wellCount; w++) {
                // wellGramsが0に近い（空のウェル）場合のみ対象
                if (wellGrams[w] < 1e-8) { // 1g未満を空と見なす
                    int bestTubeIdx = -1;
                    double bestTubeDist = Double.MAX_VALUE;
                    for (int k = 0; k < K; k++) {
                        double d = colorDist(tubes[k], targets[t]);
                        if (d < bestTubeDist) {
                            bestTubeDist = d;
                            bestTubeIdx = k;
                        }
                    }
                    
                    // 空きウェルにベストなチューブを注いだ場合の色差が、現在のminDistより良ければ採用
                    if (bestTubeIdx != -1 && bestTubeDist < minDist) { // bestTubeIdxが-1でないことを確認
                        minDist = bestTubeDist;
                        opType = 1; // ここでは「新規にチューブを注ぎ、納品」を想定
                        bestWell = w;
                        bestTube = bestTubeIdx;
                        for (int d = 0; d < 3; d++) bestColor[d] = tubes[bestTubeIdx][d];
                    }
                }
            }


            // --- 最適な操作の実行と状態更新 ---
            if (opType == 0) { // そのまま納品
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0;
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 1) { // 追加注ぎ or 空きウェルへの注ぎ込み
                // Type 1コマンド（チューブを注ぐ）
                System.out.println("1 " + wellX[bestWell] + " " + wellY[bestWell] + " " + bestTube);
                
                // 内部状態の更新
                // bestColor は、注ぎ込んだ後の色になっているはず
                for (int d = 0; d < 3; d++) wellColors[bestWell][d] = bestColor[d];
                wellGrams[bestWell] += 1.0;

                // 注ぎ込んだ後、すぐに納品する
                System.out.println("2 " + wellX[bestWell] + " " + wellY[bestWell]);
                wellGrams[bestWell] -= 1.0; // 納品で1g減る
                
                prevWell = bestWell;
                wellUsed[bestWell]++;
            } else if (opType == 2) { // 混合
                // 隣接ウェル間の壁をトグル（破壊/構築）
                toggleWall(mixX1, mixY1, mixX2, mixY2);
                
                // 内部シミュレーションでウェルを結合し色を混合
                // mixW2 の絵の具が mixW1 に合流する
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = bestColor[d]; // bestColorは混合後の色
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0; // mixW2 は空になる

                // 納品
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0;
                
                prevWell = mixW1;
                wellUsed[mixW1]++;
            } else if (opType == 3) { // 混合＋追加注ぎ
                // 隣接ウェル間の壁をトグル（破壊/構築）
                toggleWall(mixX1, mixY1, mixX2, mixY2);
                
                // 内部シミュレーションでウェルを結合し色を混合
                // mixW2 の絵の具が mixW1 に合流する
                for (int d = 0; d < 3; d++) wellColors[mixW1][d] = bestColor[d]; // bestColorは混合+注ぎ後の色
                wellGrams[mixW1] += wellGrams[mixW2];
                wellGrams[mixW2] = 0.0; // mixW2 は空になる

                // チューブを注ぎ込む
                System.out.println("1 " + wellX[mixW1] + " " + wellY[mixW1] + " " + bestTube);
                wellGrams[mixW1] += 1.0; // 1g増える

                // 納品
                System.out.println("2 " + wellX[mixW1] + " " + wellY[mixW1]);
                wellGrams[mixW1] -= 1.0; // 納品で1g減る
                
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

    // --- 壁の状態をトグルし、コマンドを出力するヘルパーメソッド ---
    // (p1x, p1y) と (p2x, p2y) は隣接しているピクセル
    static void toggleWall(int p1x, int p1y, int p2x, int p2y) {
        System.out.println("4 " + p1x + " " + p1y + " " + p2x + " " + p2y);

        // 垂直の壁か水平の壁かを判断し、内部状態を更新
        if (p1x == p2x) { // 水平の壁 (y座標が異なる)
            // horizontalWalls[x][y] は (x,y)と(x,y+1)の間の壁
            horizontalWalls[p1x][Math.min(p1y, p2y)] = !horizontalWalls[p1x][Math.min(p1y, p2y)];
        } else { // 垂直の壁 (x座標が異なる)
            // verticalWalls[x][y] は (x,y)と(x+1,y)の間の壁
            verticalWalls[Math.min(p1x, p2x)][p1y] = !verticalWalls[Math.min(p1x, p2x)][p1y];
        }
    }
}