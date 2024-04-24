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

/**
 * Graphic type that lets you draw CustomPoints, i.e. points that can have their name and rotation angle
 * be set by the user.
 */
public class CustomPoint extends OMPoint implements Nameable{

    /**
     * String field that contains the name of the object.
     */
    protected String name;

    /**
     * Field containing the rotation angle of the object in radians.
     */
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
     * Create a CustomPoint at a lat/lon position, with the default radius.
     * <p>Set the name of the object to 'point' by default. </p>
     */
    public CustomPoint(double lat, double lon) {
        this(lat, lon, DEFAULT_RADIUS);
    }

    /**
     * Create a CustomPoint at a lat/lon position, with the specified radius.
     * <p>Set the name of the object to 'point' by default. </p>
     */
    public CustomPoint(double lat, double lon, int radius) {
        setRenderType(RENDERTYPE_LATLON);
        set(lat, lon);
        setName("point");
        this.radius = radius;
    }

    /**
     * Create a CustomPoint at a lat/lon position with a screen X/Y pixel offset,
     * with the default radius.
     * <p>Set the name of the object to 'point' by default. </p>
     */
    public CustomPoint(double lat, double lon, int offsetx, int offsety) {
        this(lat, lon, offsetx, offsety, DEFAULT_RADIUS);
    }

    /**
     * Create a CustomPoint at a lat/lon position with a screen X/Y pixel offset,
     * with the specified radius.
     * <p>Set the name of the object to 'point' by default. </p>
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
     * <p>Set the name of the object to 'point' by default. </p>
     */
    public CustomPoint(int x, int y) {
        this(x, y, DEFAULT_RADIUS);
    }

    /**
     * Put the point at a screen location, marked with a rectangle with edge
     * size radius * 2 + 1.
     * <p>Set the name of the object to 'point' by default. </p>
     */
    public CustomPoint(int x, int y, int radius) {
        setRenderType(RENDERTYPE_XY);
        set(x, y);
        setName("point");
        this.radius = radius;
    }

    /**
     * Set the name of the object.
     * @param name the name to be set for the object.
     */
    @Override
    public synchronized void setName(String name){
        this.name = name;
    }

    /**
     * Set the angle by which the point is to be rotated.
     *
     * @param angle the number of radians the point is to be rotated. Measured
     *        clockwise from horizontal. Positive numbers move the positive x
     *        axis toward the positive y axis.
     */
    public void setRotationAngle(double angle) {
        this.rotationAngle = angle;
        setNeedToRegenerate(true);
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
     * Get the current rotation of the point.
     *
     * @return the angle of point rotation.
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
        GeneralPath shape;
        if (oval) {
            shape = new GeneralPath(new Ellipse2D.Float((float) x, (float) y, (float) Math.abs(x2
                    - x1), (float) Math.abs(y2 - y1)));
        } else {
            shape = createBoxShape(x, y, Math.abs(x2 - x1),
                    Math.abs(y2 - y1));
        }
        shape.transform(at);
        setShape(shape);

        initLabelingDuringGenerate();
        setLabelLocation(new Point(x2, y1), proj);

        setNeedToRegenerate(false);
        return true;
    }

    /**
     * Set the renderRotationAngle based on the projection angle and this CustomPoint
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

    /**
     * Takes the OMGeometry object and if it also belongs to this class copies it to this object.
     * @param source Object to be restored.
     */
    @Override
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
