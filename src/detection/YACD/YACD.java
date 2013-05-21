package detection.YACD;

import java.util.*;
import java.awt.*;
import java.awt.image.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class YACD {

    public static void Init() {
        tolerance = 85;
        bestPointsNum = 100;
        minR = 3;
        maxR = 6;
        maxAvailableR = 200;
        isColored = true;
        DX_DY_Table.init();
    }

    public static ArrayList<KeyPoint> findKeyPoints(final BufferedImage image) {

        final ArrayList<KeyPoint> points = new ArrayList<>();

        int processors_num = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(processors_num);

        int piece = (image.getWidth() - maxR * 2) / processors_num;
        int st = maxR;

        for (int k = 0; k < processors_num; k++) {
            final int start = st;
            final int end;
            if (k < processors_num - 1) {
                end = start + piece;
            } else {
                end = image.getWidth() - maxR;
            }
            Runnable r = new Runnable() {
                public void run() {
                    for (int j = maxR; j < image.getHeight() - maxR; j++) {
                        for (int i = start; i < end; i++) {
                            Point point = new Point(i, j);
                            if (isKeyPoint(image, point)) {
                                int scale = getPointScale(image, point, getMaxRadius(image, point));
                                double orientation = getPointOrientation(image, point, scale);

                                synchronized (points) {
                                    points.add(new KeyPoint(point, scale, orientation));
                                }

                            }
                        }
                    }
                }
            };
            st = end;
            pool.execute(r);
        }
        pool.shutdown();

        try {
            pool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return points;
    }

    public static ArrayList<KeyPoint> selectBestPoints(ArrayList<KeyPoint> keyPoints, ArrayList<Sample> samples) {

        class ValueComparator implements Comparator<Integer> {

            Map<Integer, Integer> base;

            public ValueComparator(Map<Integer, Integer> base) {
                this.base = base;
            }

            @Override
            public int compare(Integer a, Integer b) {
                int valDed = base.get(b) - base.get(a);
                if (valDed != 0) {
                    return valDed;
                } else {
                    return b - a;
                }
            }
        }
        HashMap<Integer, Integer> counts = new HashMap<>();
        TreeMap<Integer, Integer> sortedCounts = new TreeMap<>(new ValueComparator(counts));

        for (Sample sample : samples) {
            BufferedImage image = sample.getImage();
            for (int i = 0; i < keyPoints.size(); i++) {
                Point convertedPoint = sample.convertPoint(keyPoints.get(i));
                if (YACD.isKeyPoint(image, convertedPoint)) {
                    int prev = 0;
                    if (counts.get(i) != null) {
                        prev = counts.get(i);
                    }
                    counts.put(i, prev + 1);
                }
            }
        }
        sortedCounts.putAll(counts);


        /*
        ///////////////////////////////////////////
        double rep = 0;
        for (int i = 0; i < bestPointsNum; i++) {
            Map.Entry<Integer, Integer> entr = sortedCounts.pollFirstEntry();
            System.out.print(entr + " ");
            rep += entr.getValue();
        }
        rep /= bestPointsNum * samples.size();
        System.out.println("Average keypoints repeatability is " + rep);
        ///////////////////////////////////////////
        */
        
        ArrayList<KeyPoint> bestPoints = new ArrayList<>();
        for (int i = 0; i < bestPointsNum; i++) {
            int index = sortedCounts.pollFirstEntry().getKey();
            bestPoints.add(keyPoints.get(index));
        }
        return bestPoints;
    }

    public static boolean isKeyPoint(BufferedImage image, Point point) {
        for (int radius = minR; radius <= maxR; radius++) {
            int steps = DX_DY_Table.getStepsNum(radius);
            for (int k = 0; k < steps; k++) {
                int dx = DX_DY_Table.getDX(radius, k);
                int dy = DX_DY_Table.getDY(radius, k);
                Point a = new Point(point.x + dx, point.y + dy);
                Point b = new Point(point.x - dx, point.y - dy);

                if (coloredDifference(image, a, point) < tolerance && coloredDifference(image, point, b) < tolerance) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int getPointScale(BufferedImage image, Point point, int maxAvailRadius) {
        int scale = maxR;
        for (int radius = maxR + 1; radius <= maxAvailRadius; radius += 2) {
            int steps = DX_DY_Table.getStepsNum(radius);
            for (int k = 0; k < steps; k++) {
                int dx = DX_DY_Table.getDX(radius, k);
                int dy = DX_DY_Table.getDY(radius, k);
                Point a = new Point(point.x + dx, point.y + dy);
                Point b = new Point(point.x - dx, point.y - dy);

                if (coloredDifference(image, a, point) < tolerance && coloredDifference(image, point, b) < tolerance) {
                    return scale;
                }
            }
            scale++;
        }
        return scale;
    }

    public static double getPointOrientation(BufferedImage image, Point point, int rad) {
        //int[] centerRGB = calculateBluredIntensity(image, point);
        int radius = Math.round(rad / 2) - 1;
        int steps = DX_DY_Table.getStepsNum(radius);

        double[][] vectors = new double[steps][2];
        double[] sumVector = new double[2];

        for (int k = 0; k < steps; k++) {
            int dx = DX_DY_Table.getDX(radius, k);
            int dy = DX_DY_Table.getDY(radius, k);

            /////////////////////////////////////////////
            int[] pointRGB = new int[3];
            int c = image.getRGB(point.x + dx, point.y + dy);

            pointRGB[0] += (c & 0x00ff0000) >> 16;
            pointRGB[1] += (c & 0x0000ff00) >> 8;
            pointRGB[2] += (c & 0x000000ff);
            /////////////////////////////////////////////
            //int[] pointRGB = calculateBluredIntensity(image, new Point(point.x + dx, point.y + dy));

            //int difference = Math.abs(centerRGB[0] - pointRGB[0]) + Math.abs(centerRGB[1] - pointRGB[1])
            //        + Math.abs(centerRGB[2] - pointRGB[2]);

            int difference = pointRGB[0] + pointRGB[1] + pointRGB[2];

            vectors[k][0] = dx * difference;
            vectors[k][1] = dy * difference;
        }

        for (int k = 0; k < steps; k++) {
            sumVector[0] += vectors[k][0];
            sumVector[1] += vectors[k][1];
        }

        double maxAngle = Math.atan2(sumVector[1], sumVector[0]);

        /////////////////////////////////
        //System.out.println("???" + maxAngle);

        return maxAngle;
    }

    private static int[] calculateBluredIntensity(BufferedImage image, Point point) {
        int[] RGB = new int[3];
        int x = point.x;
        int y = point.y;

        updateIntensity(image, x, y, RGB, 4);

        updateIntensity(image, x + 1, y, RGB, 2);
        updateIntensity(image, x - 1, y, RGB, 2);
        updateIntensity(image, x, y + 1, RGB, 2);
        updateIntensity(image, x, y - 1, RGB, 2);

        updateIntensity(image, x + 1, y + 1, RGB, 1);
        updateIntensity(image, x + 1, y - 1, RGB, 1);
        updateIntensity(image, x - 1, y + 1, RGB, 1);
        updateIntensity(image, x - 1, y - 1, RGB, 1);

        return RGB;
    }

    private static void updateIntensity(BufferedImage image, int x, int y, int[] RGB, int multiplier) {
        int c = image.getRGB(x, y);
        RGB[0] += multiplier * (c & 0x00ff0000) >> 16;
        RGB[1] += multiplier * (c & 0x0000ff00) >> 8;
        RGB[2] += multiplier * (c & 0x000000ff);
    }

    public static int getMaxRadius(BufferedImage image, Point point) {
        int x = point.x;
        int y = point.y;
        int nx = image.getWidth() - point.x - 1;
        int ny = image.getHeight() - point.y - 1;
        return Math.min(Math.min(x, y), Math.min(nx, ny));
    }

    protected static int monochromeDifference(BufferedImage image, Point a, Point b) {
        int cA = image.getRGB(a.x, a.y) & 0x000000ff;
        int cB = image.getRGB(b.x, b.y) & 0x000000ff;

        return Math.abs(cA - cB);
    }

    public static int coloredDifference(BufferedImage image, Point a, Point b) {
        int cA = image.getRGB(a.x, a.y);
        int redA = (cA & 0x00ff0000) >> 16;
        int greenA = (cA & 0x0000ff00) >> 8;
        int blueA = cA & 0x000000ff;

        int cB = image.getRGB(b.x, b.y);
        int redB = (cB & 0x00ff0000) >> 16;
        int greenB = (cB & 0x0000ff00) >> 8;
        int blueB = cB & 0x000000ff;

        return Math.abs(redA - redB) + Math.abs(greenA - greenB) + Math.abs(blueA - blueB);
    }

    private static class DX_DY_Table {

        static void init() {
            table = new int[maxAvailableR + 1][][];
            stepsArray = new int[maxAvailableR + 1];
            for (int radius = 1; radius <= maxAvailableR; radius++) {
                int steps = (int) (2 * Math.PI * radius) - ((int) (2 * Math.PI * radius) % 4);
                stepsArray[radius] = steps;
                table[radius] = new int[steps][2];
                for (int k = 0; k < steps; k++) {
                    double angle = (2 * Math.PI / steps) * k;
                    int dx = (int) Math.round(Math.cos(angle) * radius);
                    int dy = (int) Math.round(Math.sin(angle) * radius);
                    table[radius][k][0] = dx;
                    table[radius][k][1] = dy;
                }
            }
        }

        static int getStepsNum(int radius) {
            return stepsArray[radius];
        }

        static int getDX(int radius, int k) {
            return table[radius][k][0];
        }

        static int getDY(int radius, int k) {
            return table[radius][k][1];
        }
        static int[] stepsArray;
        static int[][][] table;
    }
    private static int tolerance;
    private static int bestPointsNum;
    private static int minR;
    private static int maxR;
    private static int maxAvailableR;
    private static boolean isColored;
}
