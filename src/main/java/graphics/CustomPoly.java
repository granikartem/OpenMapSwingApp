package graphics;

import com.bbn.openmap.omGraphics.OMGeometry;
import com.bbn.openmap.omGraphics.OMPoly;
import com.bbn.openmap.util.DeepCopyUtil;


/**
 * Graphic type that lets you draw CustomPolys, i.e. polys that can have their name
 * be set by the user.
 */
public class CustomPoly extends OMPoly implements Nameable{

    /**
     * String field that contains the name of the object.
     */
    protected String name;

    /**
     * Construct a default CustomPoly.
     * <p>Set the name of the object to 'poly' by default. </p>
     */
    public CustomPoly() {
        super();
        setName("poly");
    }

    /**
     * Create an CustomPoly from a list of float lat/lon pairs.
     * <p>Set the name of the object to 'poly' by default. </p>
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
     * Create an CustomPoly from a list of float lat/lon pairs.
     * <p>Set the name of the object to 'poly' by default. </p>
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
     * Create an CustomPoly from a list of xy pairs. If you want the poly to be
     * connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     * <p>Set the name of the object to 'poly' by default. </p>
     *
     * @param xypoints array of x/y points, arranged x, y, x, y, etc.
     */
    public CustomPoly(int[] xypoints) {
        super(xypoints);
        setName("poly");
    }

    /**
     * Create an x/y CustomPoly. If you want the poly to be connected, you need to
     * ensure that the first and last coordinate pairs are the same.
     * <p>Set the name of the object to 'poly' by default. </p>
     * @param xPoints float[] of x coordinates
     * @param yPoints float[] of y coordinates
     */
    public CustomPoly(int[] xPoints, int[] yPoints) {
        super(xPoints, yPoints);
        setName("poly");
    }

    /**
     * Create an x/y CustomPoly at an offset from lat/lon. If you want the poly to
     * be connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     * <p>Set the name of the object to 'poly' by default. </p>
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
     * Create an x/y CustomPoly at an offset from lat/lon. If you want the poly to
     * be connected, you need to ensure that the first and last coordinate pairs
     * are the same.
     * <p>Set the name of the object to 'poly' by default. </p>
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

    /**
     * Sets the name of the object.
     * @param name name to be set for the object.
     */
    @Override
    public synchronized void setName(String name){
        this.name = name;
    }

    /**
     * Get the current name of the object.
     *
     * @return String containing the name of the object
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Takes the OMGeometry object and if it also belongs to this class copies it to this object.
     * @param source Object to be restored.
     */
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
