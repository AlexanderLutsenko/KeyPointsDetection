package detection.YACD;

import java.awt.image.*;
import java.awt.Point;

public class Histogram {

    public static int[] getHistogram(BufferedImage image, Point point, int rad) {
        int[] histogram = new int[85];
        for (int i = point.x - rad; i <= point.x + rad; i++) {
            for (int j = point.y - rad; j <= point.y + rad; j++) {
                int RGB = image.getRGB(i, j);
                addToHistogram(RGB, histogram);
            }
        }
        return histogram;
    }

    public static void addToHistogram(int RGB, int[] histogram) {
        int red = (RGB & 0x00ff0000) >> 16;
        int green = (RGB & 0x0000ff00) >> 8;
        int blue = RGB & 0x000000ff;
        histogram[red / 9]++;
        histogram[85 / 3 + green / 9]++;
        histogram[85 / 3 * 2 + blue / 9]++;               
    }
}
