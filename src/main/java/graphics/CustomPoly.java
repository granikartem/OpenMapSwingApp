package graphics;

import com.bbn.openmap.omGraphics.OMGeometry;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.util.DeepCopyUtil;

public class CustomPoly extends OMPoly {
    protected String name;
    /**
     * Construct a default OMPoly.
     */
    public CustomPoly() {
        super();
        setName("poly");
    }

    /**
     * Create an OMPoly from a list of float lat/lon pairs.
     * <p>
     * NOTES:
     * <ul>
     * <li>llPoints array is converted into radians IN PLACE for more efficient
     * handling internally if it's not already in radians! For even better
     * performance, you should send us an array already in radians format!
     * <li>If you want the poly to be connected (as a polygon), you need to
     * ensure that the first and last coordinate pairs are the same.
     * </ul>
     *
     * @param llPoints array of lat/lon points, arranged lat, lon, lat, lon,
     *        etc.
     * @param units radians or decimal degrees. Use OMGraphic.RADIANS or
     *        OMGraphic.DECIMAL_DEGREES
     * @param lType line type, from a list defined in OMGraphic.
     */
    public CustomPoly(double[] llPoints, int units, int lType) {
        this(llPoints, units, lType, -1);
    }

    /**
     * Create an OMPoly from a list of float lat/lon pairs.
     * <p>
     * NOTES:
     * <ul>
     * <li>llPoints array is converted into radians IN PLACE for more efficient
     * handling internally if it's not already in radians! For even better
     * performance, you should send us an array already in radians format!
     * <li>If you want the poly to be connected (as a polygon), you need to
     * ensure that the first and last coordinate pairs are the same.
     * </ul>
     *
     * @param llPoints array of lat/lon points, arranged lat, lon, lat, lon,
     *        etc.
     * @param units radians or decimal degrees. Use OMGraphic.RADIANS or
     *        OMGraphic.DECIMAL_DEGREES
     * @param lType line type, from a list defined in OMGraphic.
     * @param nsegs number of segment points (only for LINETYPE_GREATCIRCLE or
     *        LINETYPE_RHUMB line types, and if &lt; 1, this value is generated
     *        internally)
     */
    public CustomPoly(double[] llPoints, int units, int lType, int nsegs) {
        super(llPoints, units, lType, nsegs);
        setName("poly");
    }

    /**
     * Create an OMPoly from a list of xy pairs. If you want the poly to be
     * connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     *
     * @param xypoints array of x/y points, arranged x, y, x, y, etc.
     */
    public CustomPoly(int[] xypoints) {
        super(xypoints);
        setName("poly");
    }

    /**
     * Create an x/y OMPoly. If you want the poly to be connected, you need to
     * ensure that the first and last coordinate pairs are the same.
     *
     * @param xPoints float[] of x coordinates
     * @param yPoints float[] of y coordinates
     */
    public CustomPoly(int[] xPoints, int[] yPoints) {
        super(xPoints, yPoints);
        setName("poly");
    }

    /**
     * Create an x/y OMPoly at an offset from lat/lon. If you want the poly to
     * be connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     *
     * @param latPoint latitude in decimal degrees
     * @param lonPoint longitude in decimal degrees
     * @param xypoints float[] of x,y pairs
     * @param cMode offset coordinate mode
     */
    public CustomPoly(double latPoint, double lonPoint, int[] xypoints, int cMode) {
        super(latPoint, lonPoint, xypoints, cMode);
        setName("poly");
    }

    /**
     * Create an x/y OMPoly at an offset from lat/lon. If you want the poly to
     * be connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     *
     * @param latPoint latitude in decimal degrees
     * @param lonPoint longitude in decimal degrees
     * @param xPoints float[] of x coordinates
     * @param yPoints float[] of y coordinates
     * @param cMode offset coordinate mode
     */
    public CustomPoly(double latPoint, double lonPoint, int[] xPoints, int[] yPoints, int cMode) {
        super(latPoint, lonPoint, xPoints, yPoints, cMode);
        setName("poly");
    }

    public synchronized void setName(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void restore(OMGeometry source) {
        super.restore(source);
        if (source instanceof CustomPoly) {
            CustomPoly polySource = (CustomPoly) source;
            this.units = polySource.units;
            // These two things are in radians!
            this.lat = polySource.lat;
            this.lon = polySource.lon;
            this.coordMode = polySource.coordMode;
            this.xs = DeepCopyUtil.deepCopy(polySource.xs);
            this.ys = DeepCopyUtil.deepCopy(polySource.ys);
            this.isPolygon = polySource.isPolygon;
            this.rawllpts = DeepCopyUtil.deepCopy(polySource.getLatLonArray());
            this.doShapes = polySource.doShapes;
            this.name = polySource.name;
        }
    }
}
