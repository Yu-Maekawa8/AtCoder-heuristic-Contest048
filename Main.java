import java.util.Scanner;

public class Main {
    static final int N = 20;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int K = sc.nextInt();
        int H = sc.nextInt();
        int T = sc.nextInt();
        int D = sc.nextInt();

        double[][] tubes = new double[K][3];
        for (int i = 0; i < K; i++) {
            tubes[i][0] = sc.nextDouble(); // C
            tubes[i][1] = sc.nextDouble(); // M
            tubes[i][2] = sc.nextDouble(); // Y
        }
        double[][] targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // 仕切り全部下げて連結ウェル1つにする → 仕切りは全部0で出力
        // v[i][j] 縦の仕切り N行N-1列
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N - 1; j++) {
                System.out.print("0");
                if (j < N - 2) System.out.print(" ");
            }
            System.out.println();
        }
        // h[i][j] 横の仕切り N-1行N列
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < N; j++) {
                System.out.print("0");
                if (j < N - 1) System.out.print(" ");
            }
            System.out.println();
        }

        // 1つ目のマス (0,0) に注ぐ戦略（全部同じマス使う）

        for (int i = 0; i < H; i++) {
            // 目標色に一番近いチューブ番号を探す（ユークリッド距離）
            int best = 0;
            double bestDist = Double.MAX_VALUE;
            for (int k = 0; k < K; k++) {
                double d = colorDist(tubes[k], targets[i]);
                if (d < bestDist) {
                    bestDist = d;
                    best = k;
                }
            }
            // 操作1: (0,0) に bestチューブ から 1g注ぐ
            System.out.println("1 0 0 " + best);
            // 操作2: (0,0) から 1g取り出す
            System.out.println("2 0 0");
        }
        sc.close();
    }

    static double colorDist(double[] c1, double[] c2) {
        double dc = c1[0] - c2[0];
        double dm = c1[1] - c2[1];
        double dy = c1[2] - c2[2];
        return Math.sqrt(dc * dc + dm * dm + dy * dy);
    }
}
