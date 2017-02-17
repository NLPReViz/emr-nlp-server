
package io.github.nlpreviz.ml;

/**
 * Tuple representing back-end prediction and associated certainty for a given attribute.
 *
 * @author alexander.p.conrad@gmail.com
 */
public class PredictionCertaintyTuple {
    
    // name of attribute being predicted
    private String attribute;
    // value of this prediction
    private String value;
    // certainty score on range (0,1) representing confidence of prediction
    private double cert;
    private Integer documentId;

    public PredictionCertaintyTuple(String attribute, String value, double cert) { //Integer globalDocumentId
        this.attribute = attribute;
        this.value = value;
        this.cert = cert;
        //this.documentId = globalDocumentId;
    }

    public Integer getDocumentId(){
        return documentId;
    }
    
    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public double getCert() {
        return cert;
    }

    public void setCert(double cert) {
        this.cert = cert;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}
