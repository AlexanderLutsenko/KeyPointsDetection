package detection;

import detection.YACD.*;
import detection.forest.RandomForest;

import javax.imageio.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.Graphics2D;

public class Detection {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setTitle("My Panel");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BufferedImage image = null;
        BufferedImage testImage = null;
        BufferedImage gImage = null;

        try {
            image = ImageIO.read(new File("lena.jpg"));
            image = Gauss.blur(image);
            gImage = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D gi = gImage.createGraphics();
            gi.drawImage(image, 0, 0, null);
            gi.dispose();
            //image = gImage;

            //testImage = ImageIO.read(new File("lena_test1.jpg"));
            //testImage = Gauss.blur(testImage);
        } catch (IOException e) {
            e.printStackTrace();
        }

        YACD.Init();
        

        //////////////////////////////////////////////////////////
        //long time_s = System.currentTimeMillis();
        
        ArrayList<KeyPoint> keyPoints = YACD.findKeyPoints(image);
        
        //long time_e = System.currentTimeMillis();
        //long time = time_e - time_s;       
        //System.out.println("Detection time: " + time);
        System.out.println("Key points count: " + keyPoints.size());
        //////////////////////////////////////////////////////////
        ArrayList<Sample> samples = SampleCreator.createSample(image, keyPoints);
        keyPoints = YACD.selectBestPoints(keyPoints, samples);
        System.out.println("Best key points count: " + keyPoints.size());

        ArrayList<int[]> trainingData = SampleCreator.createTrainingData(image, samples, keyPoints);
        /*
         for (int[] list : trainingData) {
         for (int i = 0; i < list.length; i++) {
         System.out.print(list[i] + " ");
         }
         System.out.println();
         }
         */
        System.out.println("Total number of training attributes: " + trainingData.get(0).length);
        System.out.println("Training set size: " + trainingData.size());

        /*
         /////////////////////////////////////////////
         RandomForest forest = new RandomForest(100, trainingData);
         forest.Start();

         ArrayList<KeyPoint> points = YACD.findKeyPoints(testImage);
         ArrayList<KeyPoint> classPoints = new ArrayList<>();
         for (KeyPoint p : points) {
         int[] attribs = SampleCreator.getAttributes(testImage, p);
         int decision = forest.Evaluate(attribs);
         if (decision != 0) {
         classPoints.add(p);
         System.out.println(decision);
         }
         }
         System.out.println("Ключевых точек сопоставлено: " + classPoints.size());


         ImagePanel cpp = new ImagePanel(classPoints);
         cpp.setImage(testImage);

         /////////////////////////////////////////
         */
        ImagePanel pp = new ImagePanel(keyPoints);
        pp.setImage(image);


        f.add(pp);
        /////////////////////////////////////////
        //f.add(cpp);

        f.pack();
        f.setSize(800, 600);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}

class ImagePanel extends JPanel {

    private BufferedImage image;

    public ImagePanel(ArrayList<KeyPoint> keyPoints) {
        this.keyPoints = keyPoints;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        super.paintComponent(g2);
        if (image != null) {
            g2.setColor(Color.GREEN);
            g2.drawImage(image, 0, 0, null);

            for (KeyPoint p : keyPoints) {
                int scale = p.getScale();
                int dx = (int) Math.round(Math.cos(p.getOrientation()) * scale);
                int dy = (int) Math.round(Math.sin(p.getOrientation()) * scale);
                g2.drawOval(p.x - scale, p.y - scale, scale * 2, scale * 2);
                g2.drawLine(p.x, p.y, p.x + dx, p.y + dy);
            }
        }
    }
    private ArrayList<KeyPoint> keyPoints;
}