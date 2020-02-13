package alien4cloud.poc.plugin.lrm;

public class Constants {

    /**
     * The base type of datastore clients.
     */
    public static final String NODE_TYPE_TO_EXPORE = "org.alien4cloud.poc.lrm.pub.nodes.AbstractStandardClient";

    /**
     * The base type of relationship that are considered.
     */
    public static final String RELATIONSHIP_TYPE_TO_EXPORE = "org.alien4cloud.poc.lrm.pub.relationships.DatastoreRelationship";

    /**
     * This is the name of the relationship property that stores the mapping between capability properties and variable names.
     */
    public static final String VAR_MAPPING_PROPERTY = "var_mapping";

    /**
     * This is the name of the node property that stores variable values.
     */
    public static final String VAR_VALUES_PROPERTY = "var_values";

    /**
     * This key is used to exchange credential map between the 2 'MLS' modifiers.
     */
    public static final String MLS_TOKENS_KEY = "MLS_TOKENS_KEY";

}
