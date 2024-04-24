package graphics;

/**
 * Interface for naming Objects.
 */
public interface Nameable {
    /**
     * Method for setting the name of the object.
     * @param name the name to be set for the object.
     */
    void setName(String name);

    /**
     * Method for retrieving name of named object.
     * @return name of the object
     */
    String getName();
}
