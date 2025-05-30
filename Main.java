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
            tubes[i][0] = sc.nextDouble();
            tubes[i][1] = sc.nextDouble();
            tubes[i][2] = sc.nextDouble();
        }
        double[][] targets = new double[H][3];
        for (int i = 0; i < H; i++) {
            targets[i][0] = sc.nextDouble();
            targets[i][1] = sc.nextDouble();
            targets[i][2] = sc.nextDouble();
        }

        // 仕切り全部下げて連結ウェル1つにする
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

        // 3本混ぜて最も近い色を作る
        for (int i = 0; i < H; i++) {
            int bestA = 0, bestB = 0, bestC = 0;
            double bestX = 1.0, bestY = 0.0;
            double bestDist = Double.MAX_VALUE;
            for (int a = 0; a < K; a++) {
                for (int b = 0; b < K; b++) {
                    if (b == a) continue;
                    for (int c = 0; c < K; c++) {
                        if (c == a || c == b) continue;
                        for (double x = 0.0; x <= 1.0; x += 0.05) {
                            for (double y = 0.0; y <= 1.0 - x; y += 0.05) {
                                double z = 1.0 - x - y;
                                if (z < -1e-8) continue;
                                double[] mix = new double[3];
                                for (int d = 0; d < 3; d++) {
                                    mix[d] = tubes[a][d] * x + tubes[b][d] * y + tubes[c][d] * z;
                                }
                                double dist = colorDist(mix, targets[i]);
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
            double z = 1.0 - bestX - bestY;
            if (bestX > 1e-6) {
                System.out.println("1 0 0 " + bestA);
            }
            if (bestY > 1e-6) {
                System.out.println("1 0 0 " + bestB);
            }
            if (z > 1e-6) {
                System.out.println("1 0 0 " + bestC);
            }
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
