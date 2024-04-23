import com.bbn.openmap.app.Main;

/**
 * App class.
 * Run main() to launch customised OpenMap application or change propertiesPath variable to run with another configuration.
 */
public class App {
    /**
     * Variable containing path to properties file, used to configure the application.
     * <p>
     * openmap.properties - initial version of the file, provided by the authors of the OpenMap framework.
     * </p>
     * <p>
     * openmap_edited.properties - custom version of the file, used in this task, containing custom layers.
     * </p>
     */
    private static final String propertiesPath = "./openmap_edited.properties";
    public static void main(String[] args) {
        Main.create(propertiesPath);
    }
}
