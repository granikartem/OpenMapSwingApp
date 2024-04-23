package graphics;

import com.bbn.openmap.omGraphics.OMGeometry;
import com.bbn.openmap.omGraphics.OMGraphicConstants;
import com.bbn.openmap.omGraphics.OMPoint;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.util.Debug;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

public class CustomPoint extends OMPoint {
    protected String name;
    protected double rotationAngle = DEFAULT_ROTATIONANGLE;
    /**
     * The rotation angle used at render time, depending on rotate-ability.
     * Radians. If null, no rotation should be applied at render time.
     */
    protected Double renderRotationAngle = null;

    /** Default constructor, waiting to be filled. */
    public CustomPoint() {
        super();
    }

    /**
     * Create an OMPoint at a lat/lon position, with the default radius.
     */
    public CustomPoint(double lat, double lon) {
        this(lat, lon, DEFAULT_RADIUS);
    }

    /**
     * Create an OMPoint at a lat/lon position, with the specified radius.
     */
    public CustomPoint(double lat, double lon, int radius) {
        setRenderType(RENDERTYPE_LATLON);
        set(lat, lon);
        setName("point");
        this.radius = radius;
    }

    /**
     * Create an OMPoint at a lat/lon position with a screen X/Y pixel offset,
     * with the default radius.
     */
    public CustomPoint(double lat, double lon, int offsetx, int offsety) {
        this(lat, lon, offsetx, offsety, DEFAULT_RADIUS);
    }

    /**
     * Create an OMPoint at a lat/lon position with a screen X/Y pixel offset,
     * with the specified radius.
     */
    public CustomPoint(double lat, double lon, int offsetx, int offsety, int radius) {
        setRenderType(RENDERTYPE_OFFSET);
        set(lat, lon, offsetx, offsety);
        setName("point");
        this.radius = radius;
    }

    /**
     * Put the point at a screen location, marked with a rectangle with edge
     * size DEFAULT_RADIUS * 2 + 1.
     */
    public CustomPoint(int x, int y) {
        this(x, y, DEFAULT_RADIUS);
    }

    /**
     * Put the point at a screen location, marked with a rectangle with edge
     * size radius * 2 + 1.
     */
    public CustomPoint(int x, int y, int radius) {
        setRenderType(RENDERTYPE_XY);
        set(x, y);
        setName("point");
        this.radius = radius;
    }

    public synchronized void setName(String name){
        this.name = name;
    }

    /**
     * Set the angle by which the text is to rotated.
     *
     * @param angle the number of radians the text is to be rotated. Measured
     *        clockwise from horizontal. Positive numbers move the positive x
     *        axis toward the positive y axis.
     */
    public void setRotationAngle(double angle) {
        this.rotationAngle = angle;
        setNeedToRegenerate(true);
    }

    public String getName() {
        return name;
    }
    /**
     * Get the current rotation of the text.
     *
     * @return the text rotation.
     */
    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Prepare the point for rendering.
     *
     * @param proj Projection
     * @return true if generate was successful
     */
    @Override
    public boolean generate(Projection proj) {

        setNeedToRegenerate(true);

        if (proj == null) {
            Debug.message("omgraphic", "CustomPoint: null projection in generate!");
            return false;
        }

        // reset the internals
        int x1 = 0;
        int x2 = 0;
        int y1 = 0;
        int y2 = 0;
        renderRotationAngle = null;


        switch (renderType) {
            case RENDERTYPE_XY:
                x1 = x - radius;
                y1 = y - radius;
                x2 = x + radius;
                y2 = y + radius;

                break;
            case RENDERTYPE_OFFSET:
            case RENDERTYPE_LATLON:
                if (!proj.isPlotable(lat1, lon1)) {
                    setNeedToRegenerate(true);
                    return false;
                }
                Point p1 = (Point) proj.forward(lat1, lon1, new Point());

                x1 = p1.x + x - radius;
                y1 = p1.y + y - radius;
                x2 = p1.x + x + radius;
                y2 = p1.y + y + radius;
                break;
            case RENDERTYPE_UNKNOWN:
                System.err.println("CustomPoint.generate(): invalid RenderType");
                return false;
        }
        evaluateRotationAngle(proj);
        int x = Math.min(x2, x1);
        int y = Math.min(y2, y1);
        AffineTransform at;
        if (renderRotationAngle != null) {
            at = AffineTransform.getRotateInstance(renderRotationAngle, x, y);
        }else{
            at = AffineTransform.getRotateInstance(0.0);
        }
        if (oval) {
            GeneralPath shape = new GeneralPath(new Ellipse2D.Float((float) x, (float) y, (float) Math.abs(x2
                    - x1), (float) Math.abs(y2 - y1)));
            shape.transform(at);
            setShape(shape);
        } else {
            GeneralPath shape = createBoxShape(x, y, Math.abs(x2 - x1),
                    Math.abs(y2 - y1));
            shape.transform(at);
            setShape(shape);
        }

        initLabelingDuringGenerate();
        setLabelLocation(new Point(x2, y1), proj);

        setNeedToRegenerate(false);
        return true;
    }
    /**
     * Set the renderRotationAngle based on the projection angle and OMText
     * settings.
     *
     * @param proj the current projection.
     */
    public void evaluateRotationAngle(Projection proj) {
        renderRotationAngle = null;
        double projRotation = proj.getRotationAngle();
        Object noRotationAtt = getAttribute(OMGraphicConstants.NO_ROTATE);
        boolean compensateForProjRot = noRotationAtt != null
                && !noRotationAtt.equals(Boolean.FALSE);

        if (compensateForProjRot) {
            renderRotationAngle = rotationAngle - projRotation;
        } else if (rotationAngle != DEFAULT_ROTATIONANGLE) {
            renderRotationAngle = rotationAngle;
        }
    }

    public void restore(OMGeometry source) {
        super.restore(source);
        if (source instanceof CustomPoint) {
            CustomPoint point = (CustomPoint) source;
            this.radius = point.radius;
            this.x = point.x;
            this.y = point.y;
            this.lat1 = point.lat1;
            this.lon1 = point.lon1;
            this.oval = point.oval;
            this.name = point.name;
            this.rotationAngle = point.rotationAngle;
        }
    }
}
