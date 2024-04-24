package layer;

import com.bbn.openmap.layer.DrawingToolLayer;
import com.bbn.openmap.layer.editor.EditorLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.tools.drawing.OMDrawingTool;
import graphics.Nameable;

/**
 * Custom Version of EditorLayer used for displaying object names as tooltips.
 */

public class CustomEditorLayer extends EditorLayer {
    public CustomEditorLayer(){
        super();
    }

    /**
     * Value to be displayed for objects that do not have a name.
     */

    String editInstruction = i18n.get(DrawingToolLayer.class, "CLICK_TO_EDIT", "Click to edit.");


    /**
     *  Query for what tooltip to display for an OMGraphic when the mouse is over
     * it. If the graphic has a name (implements nameable interface) display its name,
     * display default tooltip String otherwise.
     * @param omgr instance of graphic for which a tooltip is requested.
     * @return Tooltip string.
     */
    @Override
    public String getToolTipTextFor(OMGraphic omgr) {
        OMDrawingTool dt = getDrawingTool();
        if(omgr instanceof Nameable){
            Nameable nameableGraphic = (Nameable) omgr;
            return nameableGraphic.getName();
        } else if (shouldEdit(omgr) && dt.canEdit(omgr.getClass()) && !dt.isActivated()) {
            return editInstruction;
        } else {
            return null;
        }
    }
}
