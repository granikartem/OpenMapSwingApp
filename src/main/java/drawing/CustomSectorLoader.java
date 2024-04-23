package drawing;

import com.bbn.openmap.omGraphics.EditableOMGraphic;
import com.bbn.openmap.omGraphics.GraphicAttributes;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.tools.drawing.AbstractToolLoader;
import com.bbn.openmap.tools.drawing.EditClassWrapper;
import com.bbn.openmap.tools.drawing.EditToolLoader;
import graphics.CustomSector;
import graphics.EditableCustomSector;

/**
 * Loader that knows how to create/edit CustomSector objects.
 *  <p>P.S.: this loader doens't have custom icon for sector and uses the poly icon</p>
 */

public class CustomSectorLoader extends AbstractToolLoader implements EditToolLoader {

    /**
     * Variable that contains the classname of base graphic to be handled by this loader.
     */
    protected String graphicClassName = "graphics.CustomSector";

    public CustomSectorLoader() {
        init();
    }

    /**
     *  Create the instance of The EditClassWrapper that is used by the EditToolLoaders to keep graphic
     *  classes associated with their class names, the editable class name,
     *  a valid icon and pretty name to be used in a GUI.
     */

    public void init() {
        EditClassWrapper ecw = new EditClassWrapper(graphicClassName, "graphics.CustomEditableSector", "editablepoly.gif", i18n.get(CustomSectorLoader.class,
                "customsector",
                "Sectors"));

        addEditClassWrapper(ecw);
    }

    /**
     * Given the classname of a graphic to create, returns an
     * EditableOMGraphic for that graphic. The GraphicAttributes
     * object lets you set some of the initial parameters of the
     * graphic.
     * @param classname String classname of the graphic.
     * @param ga GraphicAttributes Object
     * @return Instance of EditableCustomSector if the graphic provided is indeed CustomSector and null otherwise.
     */
    public EditableOMGraphic getEditableGraphic(String classname,
                                                GraphicAttributes ga) {
        if (classname.intern().equals(graphicClassName)) {
            return new EditableCustomSector(ga);
        }
        return null;
    }

    /**
     * Give an OMGraphic to the EditToolLoader, which will create an
     * EditableOMGraphic for it.
     * @param graphic OMGraphic object
     * @return Instance of EditableCustomSector if the graphic provided is indeed CustomSector and null otherwise.
     */
    public EditableOMGraphic getEditableGraphic(OMGraphic graphic) {
        if (graphic instanceof CustomSector) {
            return new EditableCustomSector((CustomSector) graphic);
        }
        return null;
    }
}
