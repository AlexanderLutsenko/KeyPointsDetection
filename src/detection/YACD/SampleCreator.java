package detection.YACD;

import java.io.File;
import java.util.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import javax.imageio.*;

public class SampleCreator {

    public static ArrayList<Sample> createSample(BufferedImage image, ArrayList<KeyPoint> keyPoints) {
        ArrayList<Sample> sample = new ArrayList<>();

        for (int i = 0; i < number_of_samples; i++) {
            sample.add(new Sample(image, keyPoints));
        }

        /*
        for (int i = 0; i < sample.size(); i++) {
            try {
                File saveFile = new File("lena" + i + ".jpg");
                ImageIO.write(sample.get(i).getImage(), "jpg", saveFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        */
        return sample;
    }

    public static ArrayList<int[]> createTrainingData(BufferedImage image, ArrayList<Sample> samples, ArrayList<KeyPoint> keyPoints) {
        features = new Features();
        ArrayList<int[]> data = new ArrayList<>();
        for (int i = 0; i < keyPoints.size(); i++) {
            for (Sample sample : samples) {
                KeyPoint kP = sample.convertKeyPoint(keyPoints.get(i));
                BufferedImage img = sample.getImage();
                data.add(features.getAttributes(img, kP, i + 1));
            }
        }
        for (int i = 0; i < number_of_bad_points; i++) {
            Point falsePoint = new Point();

            do {
                int x = (int) Math.round(Math.random() * (image.getWidth() - maxR * 2) + maxR);
                int y = (int) Math.round(Math.random() * (image.getHeight() - maxR * 2) + maxR);
                falsePoint = new Point(x, y);
                /////////////////////FIX!!!!!!!!!!!!!!!!!!!!!!!!!!!!
            } while (!keyPoints.contains(falsePoint));

            for (Sample sample : samples) {
                BufferedImage sImg = sample.getImage();
                Point point = sample.convertPoint(falsePoint);
                int maxAvailableR = YACD.getMaxRadius(sImg, point);
                int randomScale = (int) Math.round(Math.random() * (maxAvailableR - maxR)) + maxR;
                double orientation = YACD.getPointOrientation(sImg, point, randomScale);
                KeyPoint kPoint = new KeyPoint(point, randomScale, orientation);
                data.add(features.getAttributes(sample.getImage(), kPoint, 0));
            }

        }
        return data;
    }

    public static int[] getAttributes(BufferedImage image, KeyPoint point) {
        int[] dec = features.getAttributes(image, point, 0);
        int[] decision = new int[dec.length - 1];
        //or (int i = 0; i < dec.length -1; i++) {
        //    decision[i] = dec[i];
        //}
        return dec;
    }

    private static class Features {

        Features() {
            for (int i = 0; i < number_of_attributes; i++) {
                double ax = Math.random() - 0.5;
                double ay = Math.random() - 0.5;
                double bx = Math.random() - 0.5;
                double by = Math.random() - 0.5;

                features.add(new DPoint(ax, ay, bx, by));
            }
        }


        /*
         int[] getAttributes(BufferedImage image, KeyPoint p, int keyPointID) {
         int[] histogram = Histogram.getHistogram(image, p, maxR);
         int[] attributes = Arrays.copyOf(histogram, histogram.length+1);
         attributes[attributes.length -1] = keyPointID;
         return attributes;          
         }
         */

        int[] getAttributes(BufferedImage img, KeyPoint p, int keyPointID) {
            int size = features.size() + 1;
            int[] result = new int[size];

            for (int i = 0; i < features.size(); i += 3) {
                DPoint feature = features.get(i);
                double ax = feature.ax * p.scale + p.x;
                double ay = feature.ay * p.scale + p.y;
                double bx = feature.bx * p.scale + p.x;
                double by = feature.by * p.scale + p.y;

                double cos = Math.cos(p.orientation);
                double sin = Math.sin(p.orientation);

                int rax = (int) Math.round(ax * cos - ay * sin + p.x - p.x * cos + p.y * sin);
                int ray = (int) Math.round(ax * sin + ay * cos + p.y - p.x * sin - p.y * cos);

                int rbx = (int) Math.round(bx * cos - by * sin + p.x - p.x * cos + p.y * sin);
                int rby = (int) Math.round(bx * sin + by * cos + p.y - p.x * sin - p.y * cos);

                //System.out.println(ax+" "+ay+" --- "+rax+" "+ray);
                //System.out.println(bx+" "+by+" --- "+rbx+" "+rby);

                int cA = img.getRGB(rax, ray);
                int redA = (cA & 0x00ff0000) >> 16;
                int greenA = (cA & 0x0000ff00) >> 8;
                int blueA = cA & 0x000000ff;

                int cB = img.getRGB(rbx, rby);
                int redB = (cB & 0x00ff0000) >> 16;
                int greenB = (cB & 0x0000ff00) >> 8;
                int blueB = cB & 0x000000ff;

                result[i] = redA - redB;
                result[i + 1] = greenA - greenB;
                result[i + 2] = blueA - blueB;

            }
            result[size - 1] = keyPointID;
            return result;
        }
        private static ArrayList<DPoint> features = new ArrayList<>();
    }
    private static int number_of_bad_points = 50;
    private static int number_of_attributes = 720; /////////////////
    private static int maxR = 6;
    private static Features features;
    private static int number_of_samples = 10;
}

class DPoint {

    double ax;
    double ay;
    double bx;
    double by;

    DPoint(double ax, double ay, double bx, double by) {
        this.ax = ax;
        this.ay = ay;
        this.bx = bx;
        this.by = by;
    }
}