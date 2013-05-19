package detection.YACD;

import java.awt.Point;

public class KeyPoint extends Point {

    int scale;
    double orientation;
    
    public KeyPoint() {
        super(0, 0);
        this.scale = 0;
    }

    public KeyPoint(Point p, int scale, double orientation) {
        super(p.x, p.y);
        this.scale = scale;
        this.orientation = orientation;
    }

    public int getScale() {
        return scale;
    }
    
    public double getOrientation() {
        return orientation;
    }
}
