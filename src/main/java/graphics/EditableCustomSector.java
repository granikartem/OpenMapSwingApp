package graphics;

import com.bbn.openmap.I18n;
import com.bbn.openmap.omGraphics.*;
import com.bbn.openmap.omGraphics.editable.GraphicEditState;
import com.bbn.openmap.omGraphics.editable.GraphicSelectedState;
import com.bbn.openmap.omGraphics.editable.GraphicSetOffsetState;
import com.bbn.openmap.proj.Projection;
import com.bbn.openmap.proj.coords.LatLonPoint;
import com.bbn.openmap.util.Debug;
import com.bbn.openmap.util.PaletteHelper;
import com.bbn.openmap.util.stateMachine.State;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;

/**
 * A class that encompasses basic CustomSector and provides methods for creating it or modifying the existing one.
 */
public class EditableCustomSector extends EditableOMRect implements ActionListener{

    /**
     * This object's instance of CustomSector that it creates and/or modifies.
     */
    protected CustomSector sector;
    public final static int OFFSET_POINT_INDEX = 5;

    /**
     * Create the EditableCustomSector() setting the state machine to create the sector
     * off of the gestures.
     */
    public EditableCustomSector() {
        createGraphic(null);
    }

    /**
     * Create an EditableCustomSector with the sectorType and renderType parameters in
     * the GraphicAttributes object.
     */
    public EditableCustomSector(GraphicAttributes ga) {
        createGraphic(ga);
    }

    /**
     * Create the EditableCustomSector with an CustomSector already defined, ready for
     * editing.
     *
     * @param omc CustomSector that should be edited.
     */
    public EditableCustomSector(CustomSector omc) {
        setGraphic(omc);
    }

    /**
     * Set the graphic within the state machine. If the graphic is null, then
     * one shall be created, and located off screen until the gestures driving
     * the state machine place it on the map.
     */
    @Override
    public void setGraphic(OMGraphic graphic) {
        init();
        if (graphic instanceof CustomSector) {
            sector = (CustomSector) graphic;
            stateMachine.setSelected();
            setGrabPoints(sector);
        } else {
            createGraphic(null);
        }
    }

    /**
     * Create and set the graphic within the state machine. The
     * GraphicAttributes describe the type of sector to create.
     */
    @Override
    public void createGraphic(GraphicAttributes ga) {
        init();
        stateMachine.setUndefined();
        int renderType = OMGraphic.RENDERTYPE_UNKNOWN;
        int lineType = OMGraphic.LINETYPE_GREATCIRCLE;

        if (ga != null) {
            renderType = ga.getRenderType();
            lineType = ga.getLineType();
        }

        if (Debug.debugging("eomg")) {
            Debug.output("Editable—ustomSector.createGraphic(): rendertype = " + renderType);
            Debug.output("Editable—ustomSector.createGraphic(): linetype = " + lineType);
        }

        switch (renderType) {
            case (OMGraphic.RENDERTYPE_LATLON):
                if (lineType == OMGraphic.LINETYPE_UNKNOWN) {
                    lineType = OMGraphic.LINETYPE_GREATCIRCLE;
                    ga.setLineType(OMGraphic.LINETYPE_GREATCIRCLE);
                }

                sector = new CustomSector(90f, -180f, 90f, -180f, lineType);
                break;
            case (OMGraphic.RENDERTYPE_OFFSET):
                sector = new CustomSector(90d, -180d, -1, -1, 1, 1);
                break;
            default:
                sector = new CustomSector(-1, -1, -1, -1);
        }

        if (ga != null) {
            ga.setTo(sector, true);
        }

        assertGrabPoints();
    }

    /**
     * Get the OMGraphic being created/modified by the EditableCustomSector.
     */
    @Override
    public OMGraphic getGraphic() {
        return sector;
    }

    double diffx;
    double diffy;

    @Override
    public void initRectSize() {
        diffx = Math.abs(sector.getEastLon() - sector.getWestLon()) / 2f;
        diffy = Math.abs(sector.getNorthLat() - sector.getSouthLat()) / 2f;
    }

    /**
     * Take the current location of the GrabPoints, and modify the location
     * parameters of the Custom Sector with them. Called when you want the graphic to
     * change according to the grab points.
     */
    @Override
    public void setGrabPoints() {

        int renderType = sector.getRenderType();
        LatLonPoint llp1;

        Debug.message("eomg", "EditableCustomSector.setGrabPoints()");

        if (renderType == OMGraphic.RENDERTYPE_LATLON) {
            if (projection != null) {

                if (movingPoint == gpne) {
                    llp1 = projection.inverse(gpne.getX(), gpne.getY(), new LatLonPoint.Double());
                    sector.setLat1(llp1.getY());
                    sector.setLon2(llp1.getX());
                } else if (movingPoint == gpnw) {
                    llp1 = projection.inverse(gpnw.getX(), gpnw.getY(), new LatLonPoint.Double());
                    sector.setLat1(llp1.getY());
                    sector.setLon1(llp1.getX());
                } else if (movingPoint == gpsw) {
                    llp1 = projection.inverse(gpsw.getX(), gpsw.getY(), new LatLonPoint.Double());
                    sector.setLat2(llp1.getY());
                    sector.setLon1(llp1.getX());
                } else if (movingPoint == gpse) {
                    llp1 = projection.inverse(gpse.getX(), gpse.getY(), new LatLonPoint.Double());
                    LatLonPoint llp2 = projection.inverse(gpnw.getX(), gpnw.getY(), new LatLonPoint.Double());
                    sector.setLat1(llp2.getY());
                    sector.setLon1(llp2.getX());
                    sector.setLat2(llp1.getY());
                    sector.setLon2(llp1.getX());
                } else {
                    llp1 = projection.inverse(gpc.getX(),
                            gpc.getY(),
                            new LatLonPoint.Double());
                    sector.setLat1(llp1.getY() + diffy);
                    sector.setLon1(llp1.getX() - diffx);
                    sector.setLat2(llp1.getY() - diffy);
                    sector.setLon2(llp1.getX() + diffx);
                }
                sector.setNeedToRegenerate(true);
            }
        }

        boolean settingOffset = getStateMachine().getState() instanceof GraphicSetOffsetState
                && movingPoint == gpo;

        if (renderType == OMGraphic.RENDERTYPE_OFFSET) {

            llp1 = projection.inverse(gpo.getX(), gpo.getY(), new LatLonPoint.Double());

            sector.setLat1(llp1.getY());
            sector.setLon1(llp1.getX());

            if (settingOffset || movingPoint == gpc) {
                int halfheight = (gpse.getY() - gpnw.getY()) / 2;
                int halfwidth = (gpse.getX() - gpnw.getX()) / 2;

                sector.setX1(gpc.getX() - halfwidth - gpo.getX());
                sector.setY1(gpc.getY() - halfheight - gpo.getY());
                sector.setX2(gpc.getX() + halfwidth - gpo.getX());
                sector.setY2(gpc.getY() + halfheight - gpo.getY());
            }

            if (!settingOffset) {
                Debug.message("eomg", "EditableCustomSector: updating offset sector");
                if (movingPoint == gpnw || movingPoint == gpse) {
                    sector.setLocation(gpnw.getX() - gpo.getX(), gpnw.getY() - gpo.getY(), gpse.getX()
                            - gpo.getX(), gpse.getY() - gpo.getY());
                } else if (movingPoint == gpne || movingPoint == gpsw) {
                    sector.setLocation(gpsw.getX() - gpo.getX(), gpne.getY()
                            - gpo.getY(), gpne.getX() - gpo.getX(), gpsw.getY() - gpo.getY());
                }
                sector.setNeedToRegenerate(true);
            }

            sector.setRenderType(OMGraphic.RENDERTYPE_OFFSET);
        }

        if (renderType == OMGraphic.RENDERTYPE_XY) {
            Debug.message("eomg", "EditableCustomSector: updating x/y sector");

            if (movingPoint == gpc) {
                int halfheight = (gpse.getY() - gpnw.getY()) / 2;
                int halfwidth = (gpse.getX() - gpnw.getX()) / 2;
                sector.setLocation(gpc.getX() - halfwidth, gpc.getY() - halfheight, gpc.getX()
                        + halfwidth, gpc.getY() + halfheight);
            } else if (movingPoint == gpnw || movingPoint == gpse) {
                sector.setLocation(gpnw.getX(), gpnw.getY(), gpse.getX(), gpse.getY());
            } else if (movingPoint == gpne || movingPoint == gpsw) {
                sector.setLocation(gpsw.getX(), gpne.getY(), gpne.getX(), gpsw.getY());
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
        Debug.message("eomgdetail", "EditableCustomSector.generate()");
        if (sector != null)
            sector.generate(proj);

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
        Debug.message("eomg", "EditableCustomSector.regenerate()");
        if (sector != null)
            sector.regenerate(proj);

        setGrabPoints(sector);
        generate(proj);
    }

    /**
     * Draw the EditableCustomSector parts into the java.awt.Graphics object. The grab
     * points are only rendered if the rect machine state is
     * RectSelectedState.RECT_SELECTED.
     *
     * @param graphics java.awt.Graphics.
     */
    @Override
    public void render(java.awt.Graphics graphics) {
        Debug.message("eomgdetail", "EditableCustomSector.render()");

        State state = getStateMachine().getState();

        if (sector == null) {
            Debug.message("eomg", "EditableCustomSector.render: null sector.");
            return;
        }

        sector.setVisible(true);
        sector.render(graphics);
        sector.setVisible(false);

        int renderType = sector.getRenderType();

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

    /**
     * Modifies the gui to not include line type adjustments, and adds widgets
     * to control sector settings.
     *
     * @param graphicAttributes the GraphicAttributes to use to get the GUI
     *        widget from to control those parameters for this EOMG.
     * @return java.awt.Component to use to control parameters for this EOMG.
     */
    @Override
    public Component getGUI(GraphicAttributes graphicAttributes) {
        Debug.message("eomg", "EditableCustomPoint.getGUI");
        if (graphicAttributes != null) {
            // JComponent gaGUI = (JComponent) graphicAttributes.getGUI();
            JComponent toolbar = createAttributePanel(graphicAttributes);
            // ((JComponent) gaGUI).add(getTextGUI());
            getCustomPointGUI(graphicAttributes.getOrientation(), toolbar);
            return toolbar;
        } else {
            return getCustomPointGUI();
        }
    }

    /** Commands for changing the name, coordinates of the center-point and radiuses. */
    public final static String NameFieldCommand = "NameField";
    public final static String CenterLatitudeCommand = "CenterLatitude";
    public final static String CenterLongitudeCommand = "CenterLongitude";
    public final static String LatitudeRadiusCommand = "LatitudeRadius";
    public final static String LongitudeRadiusCommand = "LongitudeRadius";

    protected JComponent getCustomPointGUI() {
        return getCustomPointGUI(SwingConstants.HORIZONTAL, null);
    }

    JComponent attributeBox;

    /**
     * Get the GUI associated with changing the CustomSector.
     *
     * @param orientation SwingConstants.HORIZONTAL/VERTICAL
     * @param guiComp the JComponent to add stuff to. If the orientation is
     *        HORIZONTAL, the components will be added directly to this
     *        component, or to a new JComponent that is returned if null. If the
     *        orientation is Vertical, a button will be added to the guiComp, or
     *        returned. This button will call up a dialog box with the settings,
     *        since they don't really lay out vertically.
     * @return JComponent with text controls.
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
            JButton launchButton = new JButton("CustomSector");
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
        if (sector != null) {
            textString = sector.getName();
        }

        attributeBox.add(PaletteHelper.getToolBarFill(SwingConstants.HORIZONTAL));

        JTextField textField = new JTextField(textString, 25);
        textField.setActionCommand(NameFieldCommand);
        textField.addActionListener(this);
        textField.setMinimumSize(new java.awt.Dimension(100, 20));
        textField.setPreferredSize(new java.awt.Dimension(100, 20));
        attributeBox.add(textField);

        attributeBox.add(PaletteHelper.getToolBarFill(SwingConstants.HORIZONTAL));

        javax.swing.Box latPalette = javax.swing.Box.createHorizontalBox();
        JTextField latTextField = new JTextField(String.valueOf(Math.min(sector.getLat1(), sector.getLat2())), 25);
        latTextField.setActionCommand(CenterLatitudeCommand);
        latTextField.setToolTipText(i18n.get(EditableCustomSector.class, "latTextField", I18n.TOOLTIP, "Center latitude, decimal degrees."));
        latTextField.addActionListener(this);
        latTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        latTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        latPalette.add(latTextField);
        latPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(latPalette);

        javax.swing.Box lonPalette = javax.swing.Box.createHorizontalBox();
        JTextField lonTextField = new JTextField(String.valueOf(Math.min(sector.getLon1(), sector.getLon2())), 25);
        lonTextField.setActionCommand(CenterLongitudeCommand);
        lonTextField.setToolTipText(i18n.get(EditableCustomSector.class, "lonTextField", I18n.TOOLTIP, "Center longitude, decimal degrees."));
        lonTextField.addActionListener(this);
        lonTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        lonTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        lonPalette.add(lonTextField);
        lonPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(lonPalette);

        javax.swing.Box latRadPalette = javax.swing.Box.createHorizontalBox();
        JTextField latRadTextField = new JTextField(String.valueOf(Math.abs(sector.getLat2() - sector.getLat1())), 25);
        latRadTextField.setActionCommand(LatitudeRadiusCommand);
        latRadTextField.setToolTipText(i18n.get(EditableCustomSector.class, "latTextField", I18n.TOOLTIP, "Radius of sector from south to north, decimal degrees."));
        latRadTextField.addActionListener(this);
        latRadTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        latRadTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        latPalette.add(latRadTextField);
        latPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(latRadPalette);

        javax.swing.Box lonRadPalette = javax.swing.Box.createHorizontalBox();
        JTextField lonRadTextField = new JTextField(String.valueOf(Math.abs(sector.getLon2() - sector.getLon1())), 25);
        lonRadTextField.setActionCommand(LongitudeRadiusCommand);
        lonRadTextField.setToolTipText(i18n.get(EditableCustomSector.class, "lonTextField", I18n.TOOLTIP, "Radius of sector from west to east, decimal degrees."));
        lonRadTextField.addActionListener(this);
        lonRadTextField.setMinimumSize(new java.awt.Dimension(100, 20));
        lonRadTextField.setPreferredSize(new java.awt.Dimension(100, 20));
        lonPalette.add(lonRadTextField);
        lonPalette.add(new JLabel("\u00b0 "));
        attributeBox.add(lonRadPalette);

        return guiComp;
    }

    /**
     * Method for applying changes from the GUI fields to the sector.
     * @param e event this class listens to, i.e. changes in GUI fields.
     */
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        String command = e.getActionCommand();

        if (Objects.equals(command, NameFieldCommand)) {
            sector.setName(((JTextField) source).getText());
            sector.regenerate(projection);
            repaint();
        }  else if (Objects.equals(command, CenterLatitudeCommand)) {
            double latitude = new Double(((JTextField) source).getText());
            if(Math.abs(latitude) < 90) {
                double latRad = Math.abs(sector.getLat2() - sector.getLat1());
                sector.setLat1(latitude);
                sector.setLat2(latitude + latRad);
                sector.regenerate(projection);
                repaint();
            }
        } else if (Objects.equals(command, CenterLongitudeCommand)) {
            double longitude = new Double(((JTextField) source).getText());
            if(Math.abs(longitude) < 180) {
                double lonRad = Math.abs(sector.getLon2()  - sector.getLon1());
                sector.setLon1(longitude);
                sector.setLon2(longitude + lonRad);
                sector.regenerate(projection);
                repaint();
            }
        } else if (Objects.equals(command, LatitudeRadiusCommand)){
            double latRad = new Double(((JTextField) source).getText());
            if(latRad > 0 && latRad < 90) {
                double latitude = sector.getLat1() + latRad;
                if(latitude > 90){
                    latitude = 90;
                }
                sector.setLat2(latitude);
                sector.regenerate(projection);
                repaint();
            }
        } else if (Objects.equals(command, LongitudeRadiusCommand)){
            double lonRad = new Double(((JTextField) source).getText());
            if(lonRad < 180 && lonRad > 0) {
                double longitude = sector.getLon1() + lonRad;
                if(longitude > 180){
                    longitude = 180;
                }
                sector.setLon2(longitude);
                sector.regenerate(projection);
                repaint();
            }
        }
    }
}
