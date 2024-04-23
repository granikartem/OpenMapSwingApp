package graphics;

import com.bbn.openmap.gui.GridBagToolBar;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.omGraphics.editable.*;
import com.bbn.openmap.proj.ProjMath;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;
import com.bbn.openmap.util.stateMachine.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.net.URL;

public class EditableCustomPoly extends EditableOMPoly implements ActionListener{
    protected CustomPoly poly;
    /**
     * Create the EditableOMPoly, setting the state machine to create the poly
     * off of the gestures.
     */
    public EditableCustomPoly() {
        createGraphic(null);
    }

    /**
     * Create an EditableOMPoly with the polyType and renderType parameters in
     * the GraphicAttributes object.
     */
    public EditableCustomPoly(GraphicAttributes ga) {
        createGraphic(ga);
    }

    /**
     * Create the EditableOMPoly with an OMPoly already defined, ready for
     * editing.
     *
     * @param omp OMPoly that should be edited.
     */
    public EditableCustomPoly(CustomPoly omp) {
        setGraphic(omp);
    }
    /**
     * Set the graphic within the state machine. If the graphic is null, then
     * one shall be created, and located off screen until the gestures driving
     * the state machine place it on the map.
     */
    public void setGraphic(OMGraphic graphic) {
        init();
        if (graphic instanceof CustomPoly) {
            poly = (CustomPoly) graphic;
            poly.setDoShapes(true);
            stateMachine.setSelected();
            setGrabPoints(poly);
        } else {
            createGraphic(null);
        }
    }
    /**
     * Create and set the graphic within the state machine. The
     * GraphicAttributes describe the type of poly to create.
     */
    public void createGraphic(GraphicAttributes ga) {
        init();
        stateMachine.setUndefined();
        int renderType = OMGraphic.RENDERTYPE_LATLON;
        int lineType = OMGraphic.LINETYPE_GREATCIRCLE;

        if (ga != null) {
            renderType = ga.getRenderType();
            lineType = ga.getLineType();
        }

        if (Debug.debugging("eomg")) {
            Debug.output("EditableOMPoly.createGraphic(): rendertype = " + renderType);
        }

        if (lineType == OMGraphic.LINETYPE_UNKNOWN) {
            lineType = OMGraphic.LINETYPE_GREATCIRCLE;
            if (ga != null)
                ga.setLineType(OMGraphic.LINETYPE_GREATCIRCLE);
        }

        this.poly = (CustomPoly) createGraphic(renderType, lineType);

        if (ga != null) {
            ga.setRenderType(poly.getRenderType());
            ga.setTo(poly, true);
        }
    }

    /**
     * Extendable method to create specific subclasses of OMPolys.
     */
    public OMGraphic createGraphic(int renderType, int lineType) {
        OMGraphic g = null;
        switch (renderType) {
            case (OMGraphic.RENDERTYPE_LATLON):
                g = new CustomPoly(new double[0], OMGraphic.RADIANS, lineType);
                break;
            case (OMGraphic.RENDERTYPE_OFFSET):
                g = new CustomPoly(90f, -180f, new int[0], CustomPoly.COORDMODE_ORIGIN);
                break;
            default:
                g = new CustomPoly(new int[0]);
        }
        ((CustomPoly) g).setDoShapes(true);
        return g;
    }
    public OMGraphic getGraphic() {
        return poly;
    }
    /**
     * Take the current location of the GrabPoints, and modify the location
     * parameters of the OMPoly with them. Called when you want the graphic to
     * change according to the grab points.
     */
    public void setGrabPoints() {

        int renderType = poly.getRenderType();
        Projection proj = getProjection();
        if (renderType == OMGraphic.RENDERTYPE_LATLON) {
            if (proj != null) {

                double[] newCoords = new double[polyGrabPoints.size() * 2];

                // OK, this code resets the location of every point slightly to
                // the inverse location of the grab points. So if you grab one
                // node and move it, all of the precise values of each node
                // actually changes. As we go through the array of grab points,
                // we can check the corresponding projected location of the
                // current node and it matches the grab point, just use the
                // current poly value.
                double[] currentCoords = ProjMath.arrayRadToDeg(poly.getLatLonArrayCopy());

                Point2D polyPoint = new Point2D.Double();
                LatLonPoint movedPoint = new LatLonPoint.Double();

                for (int i = 0; i < polyGrabPoints.size(); i++) {
                    GrabPoint gb = polyGrabPoints.get(i);

                    int latIndex = i * 2;
                    int lonIndex = i * 2 + 1;

                    // Set the current coordinate to the GP coordinate - the
                    // logic is easier this way
                    proj.inverse(gb.getX(), gb.getY(), movedPoint);
                    newCoords[latIndex] = movedPoint.getY();
                    newCoords[lonIndex] = movedPoint.getX();

                    if (lonIndex < currentCoords.length && gpm != gb) {

                        double lat = currentCoords[latIndex];
                        double lon = currentCoords[lonIndex];
                        polyPoint = proj.forward(lat, lon, polyPoint);

                        // if the grab point, forward projected, is in the same
                        // pixel as the old point, then use the previous value.
                        boolean pointUnmoved = polyPoint.getX() == gb.getX()
                                && polyPoint.getY() == gb.getY();
                        if (pointUnmoved) {
                            newCoords[latIndex] = currentCoords[latIndex];
                            newCoords[lonIndex] = currentCoords[lonIndex];
                        }
                    }
                }

                double[] oldCoords = ProjMath.arrayRadToDeg(poly.getLatLonArrayCopy());
                int changeCount = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < oldCoords.length - 1; i += 2) {
                    if (oldCoords[i] != newCoords[i] || oldCoords[i + 1] != newCoords[i + 1]) {
                        sb.append(i / 2).append(" ");
                        changeCount++;
                    }
                }

                /*
                 * Debug.output("EOMP.SGP: " + changeCount +
                 * " points changed out of " + (oldCoords.length / 2) + ", " +
                 * sb.toString());
                 */
                poly.setLocation(ProjMath.arrayDegToRad(newCoords), OMGraphic.RADIANS);

            } else {
                Debug.message("eomg", "EditableOMPoly.setGrabPoints: projection is null, can't figure out LATLON points for poly.");
            }
        }

        if (renderType == OMGraphic.RENDERTYPE_XY || renderType == OMGraphic.RENDERTYPE_OFFSET) {

            int[] ints = new int[polyGrabPoints.size() * 2];
            if (renderType == OMGraphic.RENDERTYPE_OFFSET && gpo != null) {
                // If offset rendertype, the x-y have to be offset
                // distances, not screen pixel locations. For the
                // polygon, you also need to take into account that
                // the ints can represent 2 different things: distance
                // from the origin (Offset) or distance from the
                // previous point. Need to check with the poly to
                // find out which to do.
                GrabPoint previous = gpo;

                for (int i = 0; i < polyGrabPoints.size(); i++) {
                    GrabPoint gb = polyGrabPoints.get(i);

                    if (poly.getCoordMode() == CustomPoly.COORDMODE_PREVIOUS) {

                        ints[2 * i] = gb.getX() - previous.getX();
                        ints[2 * i + 1] = gb.getY() - previous.getY();

                        previous = gb;

                    } else {
                        ints[2 * i] = gb.getX() - gpo.getX();
                        ints[2 * i + 1] = gb.getY() - gpo.getY();
                    }
                }

                if (proj != null) {

                    LatLonPoint llp = proj.inverse(gpo.getX(), gpo.getY(), new LatLonPoint.Double());
                    poly.setLocation(llp.getRadLat(), llp.getRadLon(), OMGraphic.RADIANS, ints);

                } else {
                    Debug.message("eomg", "EditableOMPoly.setGrabPoints: projection is null, can't figure out LATLON points for poly offset.");
                }
            } else {

                for (int i = 0; i < polyGrabPoints.size(); i++) {
                    GrabPoint gb = polyGrabPoints.get(i);

                    ints[2 * i] = gb.getX();
                    ints[2 * i + 1] = gb.getY();
                }

                poly.setLocation(ints);
            }
        }

    }
    /**
     * Add a point at a certain point in the polygon coordinate list. If the
     * position is less than zero, the point will be the starting point. If the
     * position is greater than the list of current points, the point will be
     * added to the end of the poly. This method is convenient because it lets
     * you define the GrabPoint object to use for the node, in case you need a
     * special type of GrabPoint.
     *
     * @return the index for the point in the polygon, starting with 0.
     */
    public int addPoint(GrabPoint gp, int position) {

        if (gp == null) {
            return -1;
        }

        int x = gp.getX();
        int y = gp.getY();

        int renderType = poly.getRenderType();
        Projection proj = getProjection();

        if (renderType == OMGraphic.RENDERTYPE_LATLON) {
            Debug.message("eomg", "EditableOMPoly: adding point to lat/lon poly");

            if (proj != null) {

                double[] ll = poly.getLatLonArray();
                int actualPosition = (position == Integer.MAX_VALUE ? ll.length : position * 2);

                LatLonPoint llpnt = proj.inverse(x, y, new LatLonPoint.Double());

                if (Debug.debugging("eomp")) {
                    Debug.output("EditableCustomPoly: adding point to lat/lon poly at " + x + ", " + y
                            + ": " + llpnt + ", at the end of ");

                    for (int j = 0; j < ll.length; j += 2) {
                        Debug.output(ll[j] + ", " + ll[j + 1]);
                    }
                }

                double[] newll = new double[ll.length + 2];

                double newlat = llpnt.getRadLat();
                double newlon = llpnt.getRadLon();

                if (actualPosition >= ll.length) {
                    // Put the new points at the end
                    if (ll.length != 0) {
                        System.arraycopy(ll, 0, newll, 0, ll.length);
                    }

                    newll[ll.length] = newlat;
                    newll[ll.length + 1] = newlon;

                    position = ll.length / 2;

                } else if (actualPosition <= 0) {
                    // Put the new point at the beginning
                    System.arraycopy(ll, 0, newll, 2, ll.length);
                    newll[0] = newlat;
                    newll[1] = newlon;
                    position = 0;
                } else {
                    // actualPosition because there are 2 floats for every
                    // position.
                    newll[actualPosition] = newlat;
                    newll[actualPosition + 1] = newlon;
                    System.arraycopy(ll, 0, newll, 0, actualPosition);
                    System.arraycopy(ll, actualPosition, newll, actualPosition + 2, ll.length
                            - actualPosition);
                }

                poly.setLocation(newll, OMGraphic.RADIANS);
            }
        } else if (renderType == OMGraphic.RENDERTYPE_XY) {
            // Grab the projected endpoints
            Debug.message("eomg", "EditableOMPoly: adding point to x/y poly");
            int currentLength = poly.getXs().length;
            int[] newxs = new int[currentLength + 1];
            int[] newys = new int[currentLength + 1];

            if (position >= currentLength) {
                // Put the new points at the end
                System.arraycopy(poly.getXs(), 0, newxs, 0, currentLength);
                System.arraycopy(poly.getYs(), 0, newys, 0, currentLength);
                newxs[currentLength] = x;
                newys[currentLength] = y;

                position = currentLength;

            } else if (position <= 0) {
                // Put the new points at the beginning
                System.arraycopy(poly.getXs(), 0, newxs, 1, currentLength);
                System.arraycopy(poly.getYs(), 0, newys, 1, currentLength);
                newxs[0] = x;
                newys[0] = y;

                position = 0;

            } else {
                newxs[position] = x;
                newys[position] = y;

                System.arraycopy(poly.getXs(), 0, newxs, 0, position);
                System.arraycopy(poly.getXs(), position, newxs, position + 1, currentLength - position);

                System.arraycopy(poly.getYs(), 0, newys, 0, position);
                System.arraycopy(poly.getYs(), position, newys, position + 1, currentLength - position);
            }

            poly.setLocation(newxs, newys);

        } else {
            // Rendertype is offset...
            // Grab the projected endpoints
            Debug.message("eomg", "EditableOMPoly: adding point to offset poly");
            int currentLength = poly.getXs().length;
            int[] newxs = new int[currentLength + 1];
            int[] newys = new int[currentLength + 1];

            if (position >= currentLength) {
                // Put the new points at the end
                position = currentLength;

                System.arraycopy(poly.getXs(), 0, newxs, 0, currentLength);
                System.arraycopy(poly.getYs(), 0, newys, 0, currentLength);

            } else if (position <= 0) {
                // Put the new points at the beginning
                position = 0;

                System.arraycopy(poly.getXs(), 0, newxs, 1, currentLength);
                System.arraycopy(poly.getYs(), 0, newys, 1, currentLength);

            } else {

                System.arraycopy(poly.getXs(), 0, newxs, 0, position);
                System.arraycopy(poly.getXs(), position, newxs, position + 1, currentLength - position);

                System.arraycopy(poly.getYs(), 0, newys, 0, position);
                System.arraycopy(poly.getYs(), position, newys, position + 1, currentLength - position);
            }

            int offsetX;
            int offsetY;

            if (gpo.getX() == -1 && gpo.getY() == -1) {
                offsetX = proj.getWidth() / 2;
                offsetY = proj.getHeight() / 2;
            } else {
                offsetX = gpo.getX();
                offsetY = gpo.getY();
            }

            if (poly.getCoordMode() == CustomPoly.COORDMODE_ORIGIN || position == 0) { // cover
                // the first point
                newxs[position] = x - offsetX;
                newys[position] = y - offsetY;
            } else { // CMode Previous offset deltas
                newxs[position] = x - offsetX - newxs[position - 1];
                newys[position] = y - offsetY - newys[position - 1];
            }

            if (position == 0) {
                // Could call projection.getCenter() but that might
                // break if/when we make other projection
                // libraries/paradigms active.
                LatLonPoint llpnt = proj.inverse(offsetX, offsetY, new LatLonPoint.Double());

                poly.setLat(llpnt.getRadLat());
                poly.setLon(llpnt.getRadLon());
            }

            poly.setLocation(poly.getLat(), poly.getLon(), OMGraphic.RADIANS, newxs, newys);
        }

        // Need to reset the arrowhead when an end point is added,
        // removing it from the shape when the point gets added.
        // Otherwise, you end up with a arrowhead at each junction of
        // the polygon.
        OMArrowHead omah = poly.getArrowHead();
        poly.setArrowHead(null);

        // Reset the arrowhead so it will get drawn on the new
        // segment.
        poly.setArrowHead(omah);
        polyGrabPoints.add(position, gp);

        if (gpo != null) {
            gpo.addGrabPoint(gp);
        }

        // This is the standard call that needs to be made here, the
        // arrowhead changes are around this.
        poly.regenerate(proj);
        gp.generate(proj);

        return position;
    }

    /**
     * Delete a point at a certain point in the polygon coordinate list. If the
     * position is less than zero, the deleted point will be the starting point.
     * If the position is greater than the list of current points, the point
     * will be deleted from the end of the poly.
     */
    public void deletePoint(int position) {

        int renderType = poly.getRenderType();
        Projection proj = getProjection();

        boolean needToHookUp = false;
        if (position <= 0 && isEnclosed()) {
            // if the position is 0 and the polygon is enclosed, we
            // need to disengage the two points, then reattach.
            enclose(false);
            needToHookUp = true;
        }

        if (renderType == OMGraphic.RENDERTYPE_LATLON) {
            Debug.message("eomg", "EditableOMPoly: removing point from lat/lon poly");

            if (proj != null) {

                double[] ll = poly.getLatLonArray();
                double[] newll = new double[ll.length - 2];

                int actualPosition = (position == Integer.MAX_VALUE ? ll.length : position * 2);

                if (actualPosition >= ll.length) {
                    // Pull the new points off the end
                    System.arraycopy(ll, 0, newll, 0, ll.length - 2);
                    position = (ll.length - 2) / 2;
                } else if (actualPosition <= 0) {
                    // Pull the new points off the beginning
                    System.arraycopy(ll, 2, newll, 0, ll.length - 2);
                    position = 0;
                } else {
                    // actualPosition because there are 2 floats for
                    // every
                    // position.
                    System.arraycopy(ll, 0, newll, 0, actualPosition);
                    System.arraycopy(ll, actualPosition + 2, newll, actualPosition, ll.length
                            - actualPosition - 2);
                }
                poly.setLocation(newll, poly.getUnits());
            }
        } else {
            // Grab the projected endpoints
            Debug.message("eomg", "EditableOMPoly: removing point from x/y or offset poly");
            int currentLength = poly.getXs().length;
            int[] newxs = new int[currentLength - 1];
            int[] newys = new int[currentLength - 1];

            if (position >= currentLength) {
                // Pull the points from the end...
                System.arraycopy(poly.getXs(), 0, newxs, 0, currentLength - 1);
                System.arraycopy(poly.getYs(), 0, newys, 0, currentLength - 1);
                position = currentLength - 1;
            } else if (position <= 0) {
                // Pull the points from the beginning...
                System.arraycopy(poly.getXs(), 1, newxs, 0, currentLength - 1);
                System.arraycopy(poly.getYs(), 1, newys, 0, currentLength - 1);
                position = 0;
            } else {

                System.arraycopy(poly.getXs(), 0, newxs, 0, position);
                System.arraycopy(poly.getXs(), position + 1, newxs, position, currentLength - position
                        - 1);

                System.arraycopy(poly.getYs(), 0, newys, 0, position);
                System.arraycopy(poly.getYs(), position + 1, newys, position, currentLength - position
                        - 1);

            }

            if (poly.getRenderType() == OMGraphic.RENDERTYPE_OFFSET) {
                poly.setLocation(poly.getLat(), poly.getLon(), poly.getUnits(), newxs, newys);
            } else {
                poly.setLocation(newxs, newys);
            }
        }

        if (proj != null) {
            poly.regenerate(proj);
        }

        // Remove the GrabPoint for the deleted spot.
        GrabPoint gp = polyGrabPoints.remove(position);
        if (gpo != null && gp != null) {
            gpo.removeGrabPoint(gp);
        }

        if (needToHookUp) {
            enclose(true);
        }
    }
    /**
     * Called to set the OffsetGrabPoint to the current mouse location, and
     * update the OffsetGrabPoint with all the other GrabPoint locations, so
     * everything can shift smoothly. Should also set the OffsetGrabPoint to the
     * movingPoint. Should be called only once at the beginning of the general
     * movement, in order to set the movingPoint. After that, redraw(e) should
     * just be called, and the movingPoint will make the adjustments to the
     * graphic that are needed.
     */
    public void move(MouseEvent e) {
        // Need to check to see if the OffsetGrabPoint is currently
        // being used. If not, just use it, otherwise, will need to
        // create a special one for the move.

        Point2D pnt = getProjectionPoint(e);
        int x = (int) pnt.getX();
        int y = (int) pnt.getY();

        if (poly.getRenderType() == OMGraphic.RENDERTYPE_OFFSET) {
            gpm = new OffsetGrabPoint(x, y);
            gpm.clear();
        } else {
            gpm = gpo;
            gpm.clear();
            gpm.set(x, y);
        }

        // Move all the other points along with the offset point...
        addPolyGrabPointsToOGP(gpm);

        movingPoint = gpm;
    }
    /**
     * Use the current projection to place the graphics on the screen. Has to be
     * called to at least assure the graphics that they are ready for rendering.
     * Called when the graphic position changes.
     *
     * @param proj com.bbn.openmap.proj.Projection
     * @return true
     */
    public boolean generate(Projection proj) {
        Debug.message("eomg", "EditableOMPoly.generate()");
        if (poly != null) {
            poly.generate(proj);
        }
        generateGrabPoints(proj);
        return true;
    }
    /**
     * Generate the grab points, checking the OMGraphic to see if it contains
     * information about what the grab points should look like.
     *
     * @param proj
     */
    protected void generateGrabPoints(Projection proj) {

        DrawingAttributes grabPointDA = null;
        Object obj = poly.getAttribute(EditableOMGraphic.GRAB_POINT_DRAWING_ATTRIBUTES_ATTRIBUTE);
        if (obj instanceof DrawingAttributes) {
            grabPointDA = (DrawingAttributes) obj;
        }

        int index = 0;
        // Generate all the grab points, but also check to make sure the drawing
        // attributes are right
        for (GrabPoint gb : polyGrabPoints) {
            if (gb != null) {

                if (selectNodeIndex == index) {
                    Object daobj = poly.getAttribute(EditableOMGraphic.SELECTED_GRAB_POINT_DRAWING_ATTRIBUTES_ATTRIBUTE);
                    if (daobj instanceof DrawingAttributes) {
                        ((DrawingAttributes) daobj).setTo(gb);
                    }
                } else if (grabPointDA != null) {
                    grabPointDA.setTo(gb);
                } else {
                    gb.setDefaultDrawingAttributes(GrabPoint.DEFAULT_RADIUS);
                }

                gb.generate(proj);
            }

            index++;
        }

        if (gpo != null) {

            if (grabPointDA != null) {
                grabPointDA.setTo(gpo);
            } else {
                gpo.setDefaultDrawingAttributes(GrabPoint.DEFAULT_RADIUS);
            }

            gpo.generate(proj);
            gpo.updateOffsets();
        }
    }
    /**
     * Given a new projection, the grab points may need to be repositioned off
     * the current position of the graphic. Called when the projection changes.
     */
    public void regenerate(Projection proj) {
        Debug.message("eomg", "EditableOMPoly.regenerate()");
        if (poly != null) {
            poly.generate(proj);
            setGrabPoints(poly);
        }

        generateGrabPoints(proj);
    }
    /**
     * Draw the EditableOMPoly parts into the java.awt.Graphics object. The grab
     * points are only rendered if the poly machine state is
     * PolySelectedState.POLY_SELECTED.
     *
     * @param graphics java.awt.Graphics.
     */
    public void render(java.awt.Graphics graphics) {
        Debug.message("eomg", "EditableOMPoly.render()");

        State state = getStateMachine().getState();

        if (poly != null && !(state instanceof PolyUndefinedState)) {
            poly.setVisible(true);
            poly.render(graphics);
            poly.setVisible(false);
        } else {
            Debug.message("eomg", "EditableOMPoly.render: null or undefined poly.");
            return;
        }

        // Render the points actually on the polygon
        if (state instanceof GraphicSelectedState || state instanceof PolyAddNodeState
                || state instanceof PolyDeleteNodeState) {
            for (GrabPoint gb : polyGrabPoints) {
                if (gb != null) {
                    gb.setVisible(true);
                    gb.render(graphics);
                    gb.setVisible(false);
                }
            }
        }

        // In certain conditions, render the offset grab point.

        if (state instanceof GraphicSelectedState || state instanceof GraphicEditState /*
         * ||
         * state
         * instanceof
         * PolySetOffsetState
         */) {
            if (gpo != null && poly.getRenderType() == OMGraphic.RENDERTYPE_OFFSET) {
                gpo.setVisible(true);
                gpo.render(graphics);
                gpo.setVisible(false);
            }
        }
    }
    /**
     * Adds widgets to modify polygon.
     *
     * @param graphicAttributes the GraphicAttributes to use to get the GUI
     *        widget from to control those parameters for this EOMG.
     * @return Component to use to control parameters for this EOMG.
     */
    public Component getGUI(GraphicAttributes graphicAttributes) {
        Debug.message("eomg", "EditableCustomPoly.getGUI");
        if (graphicAttributes != null) {
            JMenu ahm = getArrowHeadMenu();
            graphicAttributes.setLineMenuAdditions(new JMenu[] { ahm });
            // JComponent gaGUI = (JComponent) graphicAttributes.getGUI();
            JComponent toolbar = createAttributePanel(graphicAttributes);
            getPolyGUI(graphicAttributes.getOrientation(), toolbar);
            return toolbar;
        } else {
            return getPolyGUI();
        }
    }

    /** Command for text string adjustments. */
    public final static String NameFieldCommand = "NameField";
    JToggleButton polygonButton = null;
    JButton extButton = null;
    JButton addButton = null;
    JButton deleteButton = null;

    public void enablePolygonButton(boolean enable) {
        if (polygonButton != null) {
            polygonButton.setEnabled(enable);
        }
    }

    public void enablePolygonEditButtons(boolean enable) {
        if (extButton != null) {
            extButton.setEnabled(enable);
        }
        if (addButton != null) {
            addButton.setEnabled(enable);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(enable);
        }
    }

    public JComponent getPolyGUI() {
        return getPolyGUI(true, true, true, true, SwingConstants.HORIZONTAL);
    }

    public JComponent getPolyGUI(int orientation, JComponent toolbar) {
        return getPolyGUI(true, true, true, true, orientation, toolbar);
    }

    public JComponent getPolyGUI(boolean includeEnclose, boolean includeExt, boolean includeAdd,
                                 boolean includeDelete, int orientation) {
        return getPolyGUI(includeEnclose, includeExt, includeAdd, includeDelete, orientation, null);
    }
    @Override
    public JComponent getPolyGUI(boolean includeEnclose, boolean includeExt, boolean includeAdd,
                                 boolean includeDelete, int orientation, JComponent buttonBox) {

        if (buttonBox == null) {
            buttonBox = new GridBagToolBar();
            ((GridBagToolBar) buttonBox).setOrientation(orientation);
        }

        buttonBox.add(PaletteHelper.getToolBarFill(orientation));

        URL url;
        ImageIcon imageIcon;
        if (polygonButton == null) {
            url = getImageURL("enclosepoly.gif");
            imageIcon = new ImageIcon(url);
            polygonButton = new JToggleButton(imageIcon);
            polygonButton.setToolTipText(i18n.get(EditableCustomPoly.class, "polygonButton.tooltip", "Automatically link first and last nodes"));

            polygonButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (getStateMachine().getState() instanceof GraphicSelectedState) {
                        enclose(((JToggleButton) e.getSource()).isSelected());
                    } else {
                        setEnclosed(((JToggleButton) e.getSource()).isSelected());
                    }
                    updateCurrentState(null);
                }
            });
        }

        polygonButton.setSelected(isEnclosed());

        if (includeEnclose) {
            buttonBox.add(polygonButton);
        }

        if (extButton == null) {
            url = getImageURL("addpoint.gif");
            imageIcon = new ImageIcon(url);
            extButton = new JButton(imageIcon);
            extButton.setToolTipText(i18n.get(EditableCustomPoly.class, "extButton.tooltip", "Add a point to the polygon"));
            extButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    // If an enclosed poly is having nodes added to it, we need
                    // to
                    // remove the connection, but make a note to reconnect after
                    // editing.
                    if (isEnclosed()) {
                        enclose(false);
                        setEnclosed(true);
                    }
                    ((PolyStateMachine) stateMachine).setAddPoint();
                    enablePolygonEditButtons(false);
                }
            });
        }

        extButton.setEnabled(false);
        if (includeExt) {
            buttonBox.add(extButton);
        }

        if (addButton == null) {
            url = getImageURL("addnode.gif");
            imageIcon = new ImageIcon(url);
            addButton = new JButton(imageIcon);
            addButton.setToolTipText(i18n.get(EditableCustomPoly.class, "addButton.tooltip", "Add a node to the polygon"));
            addButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ((PolyStateMachine) stateMachine).setAddNode();
                    enablePolygonEditButtons(false);
                }
            });
        }

        addButton.setEnabled(false);
        if (includeAdd) {
            buttonBox.add(addButton);
        }

        if (deleteButton == null) {
            url = getImageURL("deletepoint.gif");
            imageIcon = new ImageIcon(url);
            deleteButton = new JButton(imageIcon);
            deleteButton.setToolTipText(i18n.get(EditableCustomPoly.class, "deleteButton.tooltip", "Delete a node from the polygon"));
            deleteButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ((PolyStateMachine) stateMachine).setDeleteNode();
                    enablePolygonEditButtons(false);
                }
            });
        }

        deleteButton.setEnabled(false);
        if (includeDelete) {
            buttonBox.add(deleteButton);
        }

        String textString = "Text";
        if (poly != null) {
            textString = poly.getName();
        }

        buttonBox.add(PaletteHelper.getToolBarFill(SwingConstants.HORIZONTAL));

        JTextField textField = new JTextField(textString, 25);
        textField.setActionCommand(NameFieldCommand);
        textField.addActionListener(this);
        textField.setMinimumSize(new java.awt.Dimension(100, 20));
        textField.setPreferredSize(new java.awt.Dimension(100, 20));
        buttonBox.add(textField);
        return buttonBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String command = e.getActionCommand();

        if (command == NameFieldCommand) {
            poly.setName(((JTextField) source).getText());
            poly.regenerate(projection);
            repaint();
        }
    }
}
