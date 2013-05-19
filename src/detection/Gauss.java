package detection;

import java.awt.image.BufferedImage;

public class Gauss {

    public static BufferedImage blur(BufferedImage image) {
        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int i = 1; i < image.getWidth() - 1; i++) {
            for (int j = 1; j < image.getHeight() - 1; j++) {
                int[] RGB = new int[3];
                updateIntensity(image, i, j, RGB, 4);

                updateIntensity(image, i + 1, j, RGB, 2);
                updateIntensity(image, i - 1, j, RGB, 2);
                updateIntensity(image, i, j + 1, RGB, 2);
                updateIntensity(image, i, j - 1, RGB, 2);

                updateIntensity(image, i + 1, j + 1, RGB, 1);
                updateIntensity(image, i + 1, j - 1, RGB, 1);
                updateIntensity(image, i - 1, j + 1, RGB, 1);
                updateIntensity(image, i - 1, j - 1, RGB, 1);

                //System.out.println((Math.round(RGB[0] / 16) ) +" "+(Math.round(RGB[1] / 16) )+" "+ Math.round(RGB[2] / 16));               
                img.setRGB(i, j, (Math.round(RGB[0] / 16) << 16)  + (Math.round(RGB[1] / 16) << 8) + Math.round(RGB[2] / 16));               
            }
        }
        return img;
    }

    private static void updateIntensity(BufferedImage image, int x, int y, int[] RGB, int multiplier) {
        int rgb = image.getRGB(x, y);
        RGB[0] += multiplier * ((rgb >> 16) & 0xff);
        RGB[1] += multiplier * ((rgb >> 8) & 0xff);
        RGB[2] += multiplier * (rgb & 0xff);
    }
}
