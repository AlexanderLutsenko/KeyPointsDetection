package detection.YACD;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Sample {

    public Sample(BufferedImage image, ArrayList<KeyPoint> keyPoints) {
        double angle = Math.PI * Math.random();
        double dX = Math.random() * 1.2 - 0.6;
        double dY = Math.random() * 1.2 - 0.6;
        AffineTransform at = new AffineTransform();

        int w = image.getWidth();
        int h = image.getHeight();

        at.concatenate(AffineTransform.getTranslateInstance(w / 3 * 2, h / 3 * 2));
        at.concatenate(AffineTransform.getRotateInstance(angle, w / 3 * 2, h / 3 * 2));
        at.concatenate(AffineTransform.getShearInstance(dX, dY));

        this.image = new BufferedImage(3 * w, 3 * h, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = this.image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setTransform(at);
        g2.drawImage(image, 0, 0, null);

        matrix = new double[6];
        at.getMatrix(matrix);
    }

    public BufferedImage getImage() {
        return image;
    }

    public Point convertPoint(Point point) {
        double a = matrix[0];
        double b = matrix[1];
        double c = matrix[2];
        double d = matrix[3];
        double e = matrix[4];
        double f = matrix[5];

        int x = point.x;
        int y = point.y;

        Point newP = new Point((int) Math.round(a * x + c * y + e), (int) Math.round(b * x + d * y + f));

        //////////eflkvbhfhldvb-sdjkbv-!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!FIX!!!!!!!!!!!!
        return new Point(newP);
    }

    public KeyPoint convertKeyPoint(KeyPoint keyPoint) {
        Point point = convertPoint(keyPoint);        
        int maxRadius = YACD.getMaxRadius(image, point);
        int scale = YACD.getPointScale(image, point, maxRadius);
        double orientation = YACD.getPointOrientation(image, point, scale);
        
        return new KeyPoint(point, scale, orientation);
    }
    private BufferedImage image;
    double[] matrix;
}