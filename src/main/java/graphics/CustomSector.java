package graphics;

import com.bbn.openmap.omGraphics.OMGeometry;
import com.bbn.openmap.omGraphics.OMRect;
import com.bbn.openmap.proj.GeoProj;
import com.bbn.openmap.proj.Length;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.Debug;

import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;

public class CustomSector extends OMRect {

    private static final double start = -90.0;
    private static final double extent = 90.0;
    private static final int arcType = Arc2D.PIE;

    protected String name;

    /** Default constructor, waiting to be filled. */
    public CustomSector() {
        super();
    }

    /**
     * Create a lat/lon rectangle.
     *
     * @param lt1 latitude of north edge, decimal degrees.
     * @param ln1 longitude of west edge, decimal degrees.
     * @param lt2 latitude of south edge, decimal degrees.
     * @param ln2 longitude of east edge, decimal degrees.
     * @param lType line type - see OMGraphic.lineType.
     */
    public CustomSector(double lt1, double ln1, double lt2, double ln2, int lType) {
        super(lt1, ln1, lt2, ln2, lType);
        setName("sector");
    }

    /**
     * Create a lat/lon rectangle.
     *
     * @param lt1 latitude of north edge, decimal degrees.
     * @param ln1 longitude of west edge, decimal degrees.
     * @param lt2 latitude of south edge, decimal degrees.
     * @param ln2 longitude of east edge, decimal degrees.
     * @param lType line type - see OMGraphic.lineType.
     * @param nsegs number of segment points (only for LINETYPE_GREATCIRCLE or
     *        LINETYPE_RHUMB line types, and if &lt; 1, this value is generated
     *        internally)
     */
    public CustomSector(double lt1, double ln1, double lt2, double ln2, int lType, int nsegs) {
        super(lt1, ln1, lt2, ln2, lType, nsegs);
        setName("sector");
    }

    /**
     * Construct an XY rectangle. It doesn't matter which corners of the
     * rectangle are used, as long as they are opposite from each other.
     *
     * @param px1 x pixel position of the first corner relative to the window
     *        origin
     * @param py1 y pixel position of the first corner relative to the window
     *        origin
     * @param px2 x pixel position of the second corner relative to the window
     *        origin
     * @param py2 y pixel position of the second corner relative to the window
     *        origin
     */
    public CustomSector(int px1, int py1, int px2, int py2) {
        super(px1, py1, px2, py2);
        setName("sector");
    }

    /**
     * Construct an XY rectangle relative to a lat/lon point
     * (RENDERTYPE_OFFSET). It doesn't matter which corners of the rectangle are
     * used, as long as they are opposite from each other.
     *
     * @param lt1 latitude of the reference point, decimal degrees.
     * @param ln1 longitude of the reference point, decimal degrees.
     * @param px1 x pixel position of the first corner relative to the reference
     *        point
     * @param py1 y pixel position of the first corner relative to the reference
     *        point
     * @param px2 x pixel position of the second corner relative to the
     *        reference point
     * @param py2 y pixel position of the second corner relative to the
     *        reference point
     */
    public CustomSector(double lt1, double ln1, int px1, int py1, int px2, int py2) {
        super(lt1, ln1, px1, py1, px2, py2);
        setName("sector");
    }


    public synchronized void setName(String name){
        this.name = name;
    }

    public void setLat1(double lat1){
        this.lat1 = lat1;
    }

    public void setLat2(double lat2){
        this.lat2 = lat2;
    }

    public void setLon1(double lon1){
        this.lon1 = lon1;
    }

    public void setLon2(double lon2){
        this.lon2 = lon2;
    }

    public void setX1(int x1){
        this.x1 = x1;
    }

    public void setX2(int x2){
        this.x2 = x2;
    }

    public void setY1(int y1){
        this.y1 = y1;
    }

    public void setY2(int y2){
        this.y2 = y2;
    }

    public String getName() {
        return this.name;
    }

    public double getLat1(){
        return this.lat1;
    }

    public double getLat2(){
        return this.lat2;
    }

    public double getLon1(){
        return this.lon1;
    }

    public double getLon2(){
        return this.lon2;
    }

    public int getX1(){
        return this.x1;
    }

    public int getX2(){
        return this.x2;
    }

    public int getY1(){
        return this.y1;
    }

    public int getY2(){
        return this.y2;
    }

    /**
     * Prepare the rectangle for rendering.
     *
     * @param proj Projection
     * @return true if generate was successful
     */
    @Override
    public boolean generate(Projection proj) {

        setNeedToRegenerate(true);

        if (proj == null) {
            Debug.message("omgraphic", "OMRect: null projection in generate!");
            return false;
        }
        // reset the internals
        GeneralPath projectedShape = null;
        double x;
        double y;
        double width;
        double height;
        Shape arcShape = null;
        PathIterator pi = null;
        switch (renderType) {
            case RENDERTYPE_XY:
                x = Math.min(x2, x1);
                y = Math.min(y2, y1);
                width = Math.abs(x2 - x1);
                height = Math.abs(y2 - y1);

                arcShape = createArcShape(x, y, width, height);

                pi = arcShape.getPathIterator(null);
                projectedShape = new GeneralPath();
                projectedShape.append(pi, false);
                break;
            case RENDERTYPE_OFFSET:
                if (!proj.isPlotable(lat1, lon1)) {
                    setNeedToRegenerate(true);// HMMM not the best flag
                    return false;
                }
                Point p1 = (Point) proj.forward(lat1, lon1, new Point());

                x = Math.min(p1.x + x1, p1.x + x2);
                y = Math.min(p1.y + y1, p1.y + y2);
                width = Math.abs(x2 - x1);
                height =Math.abs(y2 - y1);

                arcShape = createArcShape(x, y, width, height);

                pi = arcShape.getPathIterator(null);
                projectedShape = new GeneralPath();
                projectedShape.append(pi, false);
                break;
            case RENDERTYPE_LATLON:
                double[] rawllpts = createLatLonPoints();
                ArrayList<float[]> vector = null;

                // polygon/polyline project the polygon/polyline.
                // Vertices should already be in radians.ArrayList vector;
                if (proj instanceof GeoProj) {
                    vector = ((GeoProj) proj).forwardPoly(rawllpts, getLineType(), -1, true);

                    int size = vector.size();
                    // We could call create shape, but this is more efficient.
                    for (int i = 0; i < size; i += 2) {
                        GeneralPath gp = createShape(vector.get(i), vector.get(i + 1), true);

                        projectedShape = appendShapeEdge(projectedShape, gp, false);
                    }
                } else {
                    Point2D center = new LatLonPoint.Double(Math.min(lat1, lat2), Math.min(lon1, lon2));
                    Point2D opposite = new LatLonPoint.Double(Math.max(lat1, lat2), Math.max(lon1, lon2));
                    Shape shape = createArcShape(center.getX(), center.getY(), opposite.getX() - center.getX(),
                            opposite.getY() - center.getY());
                    projectedShape = new GeneralPath(shape);
                }
                break;
            case RENDERTYPE_UNKNOWN:
                System.err.println("CustomSector.generate(): invalid RenderType");
                return false;
        }
        setShape(projectedShape);
        setLabelLocation(getShape(), proj);

        setNeedToRegenerate(false);
        return true;
    }

    @Override
    public void restore(OMGeometry source) {
        super.restore(source);
        if (source instanceof CustomSector) {
            CustomSector sector = (CustomSector) source;
            this.name = sector.name;
            this.x1 = sector.x1;
            this.y1 = sector.y1;
            this.lat1 = sector.lat1;
            this.lon1 = sector.lon1;
            this.x2 = sector.x2;
            this.y2 = sector.y2;
            this.lat2 = sector.lat2;
            this.lon2 = sector.lon2;
            this.nsegs = sector.nsegs;
        }
    }
    /**
     * An internal method designed to fetch the Shape to be used for an XY or
     * OFFSET OMArc. This method is smart enough to take the calculated position
     * information and make a call to Arc2D.Double with start, extent and
     * arcType information.
     */
    protected Shape createArcShape(double x, double y, double fwidth, double fheight) {
        return new Arc2D.Double(x, y, fwidth, fheight, start, extent, arcType);
    }

    public double[] createLatLonPoints() {
        int i;
        int nMax = 18;
        double angle = -Math.PI/2;
        double angleInc = Math.PI / 2 / nMax;
        double[] distance = new double[nMax + 1];
        double x;
        double y;
        double a;
        double b;
        double[] azimuth = new double[nMax + 1];
        double[] llPoints = new double[2 * (nMax + 1)];
        Length units = Length.DECIMAL_DEGREE;
        b = units.toRadians(Math.abs(lat1 - lat2));
        a = units.toRadians(Math.abs(lon1-lon2));

        for (i = 0; i < nMax - 1; i++) {

            x = Math.sqrt((a * a * b * b) / ((b * b) + ((a * a) * Math.pow(Math.tan(angle), 2))));
            double yt = (x * x) / (a * a);
            if (yt > 1.0) {
                yt = 1.0;
            }
            y = Math.sqrt((1.0 - yt) * (b * b));

            distance[i] = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
            azimuth[i] = angle + com.bbn.openmap.MoreMath.HALF_PI;

            if (Debug.debugging("ellipse")) {
                Debug.output(" "
                        + i
                        + " "
                        + Math.toDegrees(azimuth[i])
                        + " ( "
                        + distance[i]
                        + " ) "
                        + (Debug.debugging("ellipsedetail") ? ("[from x:" + x + ", y:" + y + ", a:"
                        + a + ", b:" + b + "]") : ""));
            }
            angle += angleInc;
        }
        distance[nMax - 1] = 0;
        azimuth[nMax - 1] = 0;
        distance[nMax ] = distance[0];
        azimuth[nMax ] = azimuth[0];
        int nCounter = 0;
        Point2D center = new LatLonPoint.Double(Math.min(lat1, lat2), Math.min(lon1, lon2));
        for (i = 0; i < nMax + 1; i++) {

            LatLonPoint llPt = LatLonPoint.getDouble(center).getPoint((float) distance[i], azimuth[i]);
            llPoints[nCounter++] = llPt.getRadLat();
            llPoints[nCounter++] = llPt.getRadLon();
        }
        return llPoints;
    }
}
