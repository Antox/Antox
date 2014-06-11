package im.tox.antox.utils;

/**
 * Created by ollie on 29/05/14.
 */
public class Triple<X, Y, Z> {
    public final X x;
    public final Y y;
    public final Z z;
    public Triple(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}