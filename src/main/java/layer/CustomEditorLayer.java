package layer;

import com.bbn.openmap.layer.DrawingToolLayer;
import com.bbn.openmap.layer.editor.EditorLayer;
import com.bbn.openmap.omGraphics.OMGraphic;
import com.bbn.openmap.tools.drawing.OMDrawingTool;
import graphics.CustomPoint;
import graphics.CustomPoly;
import graphics.CustomSector;

public class CustomEditorLayer extends EditorLayer {
    public CustomEditorLayer(){
        super();
    }

    String editInstruction = i18n.get(DrawingToolLayer.class, "CLICK_TO_EDIT", "Click to edit.");


    /**
     * Query for what tooltip to display for an OMGraphic when the mouse is over
     * it.
     */
    @Override
    public String getToolTipTextFor(OMGraphic omgr) {
        OMDrawingTool dt = getDrawingTool();
        if(omgr instanceof CustomPoint){
            CustomPoint point = (CustomPoint) omgr;
            return point.getName();
        } else if(omgr instanceof CustomPoly){
            CustomPoly poly = (CustomPoly) omgr;
            return poly.getName();
        } else if(omgr instanceof CustomSector){
            CustomSector sector = (CustomSector) omgr;
            return sector.getName();
        }else if (shouldEdit(omgr) && dt.canEdit(omgr.getClass()) && !dt.isActivated()) {
            return editInstruction;
        } else {
            return null;
        }
    }
}
