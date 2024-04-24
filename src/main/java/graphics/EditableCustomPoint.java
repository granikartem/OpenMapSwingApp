package graphics;

import com.bbn.openmap.I18n;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.omGraphics.editable.*;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;
import com.bbn.openmap.util.stateMachine.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.Objects;

/**
 * A class that encompasses basic CustomPoint and provides methods for creating it or modifying the existing one.
 */
public class EditableCustomPoint extends EditableOMPoint implements ActionListener{

    /**
     * This object's instance of CustomPoint that it creates and/or modifies.
     */
    protected CustomPoint point;

    /**
     * Create the EditableCustomPoint, setting the state machine to create the point
     * off of the gestures.
     */
    public EditableCustomPoint() {
        createGraphic(null);
    }

    /**
     * Create an EditableCustomPoint with the pointType and renderType parameters in
     * the GraphicAttributes object.
     */
    public EditableCustomPoint(GraphicAttributes ga) {
        createGraphic(ga);
    }

    /**
     * Create the EditableCustomPoint with a CustomPoint already defined, ready for
     * editing.
     *
     * @param point CustomPoint that should be edited.
     */
    public EditableCustomPoint(CustomPoint point) {
        setGraphic(point);
    }

    /**
     * Set the graphic within the state machine. If the graphic is null, then
     * one shall be created, and located off screen until the gestures driving
     * the state machine place it on the map.
     */
    @Override
    public void setGraphic(OMGraphic graphic) {
        init();
        if (graphic instanceof CustomPoint) {
            point = (CustomPoint) graphic;
            stateMachine.setSelected();
            setGrabPoints(point);
        } else {
            createGraphic(null);
        }
    }

    /**
     * Create and set the graphic within the state machine. The
     * GraphicAttributes describe the type of point to create.
     */
    @Override
    public void createGraphic(GraphicAttributes ga) {
        init();
        stateMachine.setUndefined();
        int renderType = OMGraphic.RENDERTYPE_UNKNOWN;

        if (ga != null) {
            renderType = ga.getRenderType();
        }

        if (Debug.debugging("eomg")) {
            Debug.output("EditableCustomPoint.createGraphic(): rendertype = " + renderType);
        }

        switch (renderType) {
            case (OMGraphic.RENDERTYPE_LATLON):
                point = new CustomPoint(90f, -180f);
                break;
            case (OMGraphic.RENDERTYPE_OFFSET):
                point = new CustomPoint(90f, -180f, 0, 0);
                break;
            default:
                point = new CustomPoint(-1, -1);
        }

        if (ga != null) {
            ga.setTo(point);
        }

        assertGrabPoints();
    }

    /**
     * Get the OMGraphic being created/modified by the EditableCustomPoint.
     */
    @Override
    public OMGraphic getGraphic() {
        return point;
    }

    /**
     * Set the grab points for the graphic provided, setting them on the extents
     * of the graphic. Called when you want to set the grab points off the
     * location of the graphic.
     */
    @Override
    public void setGrabPoints(OMGraphic graphic) {
        Debug.message("eomg", "EditableCustomPoint.setGrabPoints(graphic)");
        if (!(graphic instanceof CustomPoint)) {
            return;
        }

        assertGrabPoints();

        CustomPoint point = (CustomPoint) graphic;
        boolean ntr = point.getNeedToRegenerate();
        int renderType = point.getRenderType();

        LatLonPoint llp;
        int latoffset = 0;
        int lonoffset = 0;

        boolean doStraight = true;

        if (!ntr) {

            if (renderType == OMGraphic.RENDERTYPE_LATLON
                    || renderType == OMGraphic.RENDERTYPE_OFFSET) {

                if (projection != null) {
                    double lon = point.getLon();
                    double lat = point.getLat();

                    llp = new LatLonPoint.Double(lat, lon);
                    Point2D p = projection.forward(llp);
                    if (renderType == OMGraphic.RENDERTYPE_LATLON) {
                        doStraight = false;
                        gpc.set((int) p.getX(), (int) p.getY());
                    } else {
                        latoffset = (int) p.getY();
                        lonoffset = (int) p.getX();
                        gpo.set(lonoffset, latoffset);
                    }
                }
            }

            if (doStraight) {
                gpc.set(lonoffset + point.getX(), latoffset + point.getY());
            }

            if (renderType == OMGraphic.RENDERTYPE_OFFSET) {
                gpo.updateOffsets();
            }

        } else {
            Debug.message("eomg", "EditableCustomPoint.setGrabPoints: graphic needs to be regenerated");
        }
    }

    /**
     * Take the current location of the GrabPoints, and modify the location
     * parameters of the CustomPoint with them. Called when you want the graphic to
     * change according to the grab points.
     */
    @Override
    public void setGrabPoints() {

        int renderType = point.getRenderType();
        LatLonPoint llp1;

        Debug.message("eomg", "EditableCustCustomPoint.setGrabPoints()");

        // Do center point for lat/lon or offset points
        if (renderType == OMGraphic.RENDERTYPE_LATLON) {

            if (projection != null) {
                // movingPoint == gpc
                llp1 = projection.inverse(gpc.getX(), gpc.getY(), new LatLonPoint.Double());
                point.set(llp1.getY(), llp1.getX());
                // point.setNeedToRegenerate set
            }
        }

        boolean settingOffset = getStateMachine().getState() instanceof GraphicSetOffsetState
                && movingPoint == gpo;

        // If the center point is moving, the offset distance changes
        if (renderType == OMGraphic.RENDERTYPE_OFFSET) {

            llp1 = projection.inverse(gpo.getX(), gpo.getY(), new LatLonPoint.Double());

            point.setLat(llp1.getY());
            point.setLon(llp1.getX());

            if (settingOffset || movingPoint == gpc) {
                // Don't call point.setLocation because we only want
                // to
                // setNeedToRegenerate if !settingOffset.
                point.setX(gpc.getX() - gpo.getX());
                point.setY(gpc.getY() - gpo.getY());
            }

            if (!settingOffset) {
                Debug.message("eomg", "EditableCustomPoint: updating offset point");
                point.set(gpc.getX() - gpo.getX(), gpc.getY() - gpo.getY());
            }

            // Set Location has reset the rendertype, but provides
            // the convenience of setting the max and min values
            // for us.
            point.setRenderType(OMGraphic.RENDERTYPE_OFFSET);
        }

        // Do the point height and width for XY and OFFSET render
        // types.
        if (renderType == OMGraphic.RENDERTYPE_XY) {
            Debug.message("eomg", "EditableCustomPoint: updating x/y point");

            if (movingPoint == gpc) {
                point.set(gpc.getX(), gpc.getY());
            }
        }

        if (projection != null) {
            regenerate(projection);
        }
    }

    /**
     * Use the current projection to place the graphics on the screen. Has to be
     * called to at least assure the graphics that they are ready for rendering.
     * Called when the graphic position changes.
     *
     * @param proj com.bbn.openmap.proj.Projection
     * @return true
     */
    @Override
    public boolean generate(Projection proj) {
        Debug.message("eomgdetail", "EditableCustomPoint.generate()");
        if (this.point != null)
            this.point.generate(proj);

        for (GrabPoint gp : gPoints) {
            if (gp != null) {
                gp.generate(proj);
            }
        }
        return true;
    }

    /**
     * Given a new projection, the grab points may need to be repositioned off
     * the current position of the graphic. Called when the projection changes.
     */
    @Override
    public void regenerate(Projection proj) {
        Debug.message("eomg", "EditableCustomPoint.regenerate()");
        if (this.point != null)
            this.point.generate(proj);

        setGrabPoints(this.point);
        generate(proj);
    }

    /**
     * Draw the EditableCustomPoint parts into the java.awt.Graphics object. The
     * grab points are only rendered if the point machine state is
     * PointSelectedState.POINT_SELECTED.
     *
     * @param graphics java.awt.Graphics.
     */
    @Override
    public void render(java.awt.Graphics graphics) {
        Debug.message("eomgdetail", "EditableCustomPoint.render()");

        if (this.point == null) {
            Debug.message("eomg", "EditableCustomPoint.render: null point.");
            return;
        }

        State state = getStateMachine().getState();

        if (!(state instanceof GraphicUndefinedState)) {
            this.point.setVisible(true);
            this.point.render(graphics);
            this.point.setVisible(false);

            int renderType = this.point.getRenderType();

            if (state instanceof GraphicSelectedState || state instanceof GraphicEditState) {

                for (int i = 0; i < gPoints.length; i++) {

                    GrabPoint gp = gPoints[i];
                    if (gp != null) {
                        if ((i == OFFSET_POINT_INDEX && renderType == OMGraphic.RENDERTYPE_OFFSET && movingPoint == gpo)
                                || (state instanceof GraphicSelectedState && (i != OFFSET_POINT_INDEX || renderType == OMGraphic.RENDERTYPE_OFFSET))

                        ) {

                            gp.setVisible(true);
                            gp.render(graphics);
                            gp.setVisible(false);
                        }
                    }
                }
            }
        }
    }

    /**
     * Modifies the gui to not include line type adjustments, and adds widgets
     * to control point settings.
     *
     * @param graphicAttributes the GraphicAttributes to use to get the GUI
     *        widget from to control those parameters for this EOMG.
     * @return java.awt.Component to use to control parameters for this EOMG.
     */
    @Override
    public Component getGUI(GraphicAttributes graphicAttributes) {
        Debug.message("eomg", "EditableCustomPoint.getGUI");
        if (graphicAttributes != null) {
            JComponent toolbar = createAttributePanel(graphicAttributes);
            getCustomPointGUI(graphicAttributes.getOrientation(), toolbar);
            return toolbar;
        } else {
            return getCustomPointGUI();
        }
    }

    /** Commands for changing the name, rotation angle and coordinates of the point. */
    public final static String NameFieldCommand = "NameField";
    public final static String PointRotationCommand = "PointRotation";
    public final static String PointLatitudeCommand = "PointLatitude";
    public final static String PointLongitudeCommand = "PointLongitude";

    /**
     * Get the GUI associated with changing the CustomPoint.
     */
    protected JComponent getCustomPointGUI() {
        return getCustomPointGUI(SwingConstants.HORIZONTAL, null);
    }

    JComponent attributeBox;

    /**
     * Get the GUI associated with changing the CustomPoint.
     *
     * @param orientation SwingConstants.HORIZONTAL/VERTICAL
     * @param guiComp the JComponent to add stuff to. If the orientation is
     *        HORIZONTAL, the components will be added directly to this
     *        component, or to a new JComponent that is returned if null. If the
     *        orientation is Vertical, a button will be added to the guiComp, or
     *        returned. This button will call up a dialog box with the settings,
     *        since they don't really lay out vertically.
     * @return JComponent with controls.
     */
    protected JComponent getCustomPointGUI(int orientation, JComponent guiComp) {
        attributeBox = null;

        if (guiComp == null || orientation == SwingConstants.VERTICAL) {
            attributeBox = javax.swing.Box.createHorizontalBox();

            attributeBox.setAlignmentX(Component.CENTER_ALIGNMENT);
            attributeBox.setAlignmentY(Component.CENTER_ALIGNMENT);

        } else if (orientation == SwingConstants.HORIZONTAL) {
            attributeBox = guiComp;
        }

        if (guiComp == null) {
            guiComp = new JPanel();
        }

        guiComp.add(PaletteHelper.getToolBarFill(orientation));

        if (orientation == SwingConstants.VERTICAL) {
            JButton launchButton = new JButton("CustomPoint");
            launchButton.addActionListener(ae -> {
                if (attributeBox != null) {
                    JDialog dialog = new JDialog();
                    dialog.setContentPane(attributeBox);
                    dialog.setModal(true);
                    dialog.pack();
                    dialog.setLocationRelativeTo((JButton) ae.getSource());
                    dialog.setVisible(true);
                }
            });
            guiComp.add(launchButton);
        }

        String textString = "Text";
        if (point != null) {
            textString = point.getName();
        }

        attributeBox.add(PaletteHelper.getToolBarFill(SwingConstants.HORIZONTAL));

        JTextField textField = new JTextField(textString, 25);
        textField.setActionCommand(NameFieldCommand);
        textField.addActionListener(this);
        textField.setMinimumSize(new java.awt.Dimension(100, 20));
        textField.setPreferredSize(new java.awt.Dimension(100, 20));
        attributeBox.add(textField);

        attributeBox.add(PaletteHelper.getToolBarFill(SwingConstants.HORIZONTAL));

        javax.swing.Box rotationPalette = javax.swing.Box.createHorizontalBox();
        JTextField rotationTextField = new JTextField(String.valueOf((int) Math.toDegrees(point.getRotationAngle())), 5);
        rotationTextField.setActionCommand(PointRotationCommand);
        rotationTextField.setToolTipText(i18n.get(EditableCustomPoint.class, "rotationTextField", I18n.TOOLTIP, "Point rotation in degrees"));
        rotationTextField.addActionListener(this);
        rotationTextField.setMinimumSize(new java.awt.Dimension(30, 20));
        rotationTextField.setPreferredSize(new java.awt.Dimension(30, 20));
        rotationPalette.add(rotationTextField);
        rotationPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(rotationPalette);

        javax.swing.Box latPalette = javax.swing.Box.createHorizontalBox();
        JTextField latTextField = new JTextField(String.valueOf(point.getLat()), 25);
        latTextField.setActionCommand(PointLatitudeCommand);
        latTextField.setToolTipText(i18n.get(EditableCustomPoint.class, "latTextField", I18n.TOOLTIP, "Point latitude, decimal degrees."));
        latTextField.addActionListener(this);
        latTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        latTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        latPalette.add(latTextField);
        latPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(latPalette);

        javax.swing.Box lonPalette = javax.swing.Box.createHorizontalBox();
        JTextField lonTextField = new JTextField(String.valueOf( point.getLon()), 25);
        lonTextField.setActionCommand(PointLongitudeCommand);
        lonTextField.setToolTipText(i18n.get(EditableCustomPoint.class, "lonTextField", I18n.TOOLTIP, "Point longitude, decimal degrees."));
        lonTextField.addActionListener(this);
        lonTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        lonTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        lonPalette.add(lonTextField);
        lonPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(lonPalette);

        return guiComp;
    }

    /**
     * Method for applying changes from the GUI fields to the point.
     * @param e event this class listens to, i.e. changes in GUI fields.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String command = e.getActionCommand();

        if (Objects.equals(command, NameFieldCommand)) {
            point.setName(((JTextField) source).getText());
            point.regenerate(projection);
            repaint();
        } else if (Objects.equals(command, PointRotationCommand)) {
            Integer rotation = new Integer(((JTextField) source).getText());
            point.setRotationAngle(Math.toRadians(rotation));
            point.regenerate(projection);
            repaint();
        } else if (Objects.equals(command, PointLatitudeCommand)) {
            double latitude = new Double(((JTextField) source).getText());
            if(Math.abs(latitude) < 90) {
                point.setLat(latitude);
                point.regenerate(projection);
                for (GrabPoint gp: gPoints) {
                    gp.set(point.getLon(), point.getLat(), 0, 0);
                    gp.setVisible(true);
                }
                repaint();
            }
        } else if (Objects.equals(command, PointLongitudeCommand)) {
            double longitude = new Double(((JTextField) source).getText());
            if(Math.abs(longitude) < 180) {
                point.setLon(longitude);
                point.regenerate(projection);
                for (GrabPoint gp: gPoints) {
                    gp.set(point.getLon(), point.getLat(), 0, 0);
                    gp.setVisible(true);
                }
                repaint();
            }
        }
    }
}
