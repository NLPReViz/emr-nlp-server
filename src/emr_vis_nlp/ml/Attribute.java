
package emr_vis_nlp.ml;

import java.util.List;

/**
 * Represents a general attribute / property of interest which a back-end model
 * may be predicting.
 *
 * @author alexander.p.conrad@gmail.com
 */
public class Attribute {

    // kind of attribute that this is
    private AttributeType attributeType;
    // name of this attribute
    private String name;
    // extended name of this attribute
    private String elaboration;
    // legal values for this attribute
    private List<String> legalValues;
    
    public Attribute(AttributeType attributeType, String name, String elaboration, List<String> legalValues) {
        this.attributeType = attributeType;
        this.name = name;
        this.elaboration = elaboration;
        this.legalValues = legalValues;
    }
    
    public AttributeType getAttributeType() {
        return attributeType;
    }

    public void setAttributeType(AttributeType attributeType) {
        this.attributeType = attributeType;
    }

    public String getElaboration() {
        return elaboration;
    }

    public void setElaboration(String elaboration) {
        this.elaboration = elaboration;
    }

    public List<String> getLegalValues() {
        return legalValues;
    }

    public void setLegalValues(List<String> legalValues) {
        this.legalValues = legalValues;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Attribute{" + "attributeType=" + attributeType + ", name=" + name + ", elaboration=" + elaboration + ", legalValues=" + legalValues + '}';
    }
    
    /**
     * Represents the general categories of attributes we may encounter, with respect to kinds of things represented and kinds of values possible.
     */
    public static enum AttributeType {
        
        VARIABLE_CATEGORICAL,
        VARIABLE_CONTINUOUS,
        INDICATOR_CATEGORICAL,
        INDICATOR_CONTINUOUS,
        OTHER;
        
        public static AttributeType getType(String name) {
            if (name.trim().equalsIgnoreCase(AttributeType.VARIABLE_CATEGORICAL.name())) {
                return VARIABLE_CATEGORICAL;
            } else if (name.trim().equalsIgnoreCase(AttributeType.VARIABLE_CONTINUOUS.name())) {
                return VARIABLE_CONTINUOUS;
            } else if (name.trim().equalsIgnoreCase(AttributeType.INDICATOR_CATEGORICAL.name())) {
                return INDICATOR_CATEGORICAL;
            } else if (name.trim().equalsIgnoreCase(AttributeType.INDICATOR_CONTINUOUS.name())) {
                return INDICATOR_CONTINUOUS;
            } else {
                return OTHER;
            }
        }
    }
    
}
