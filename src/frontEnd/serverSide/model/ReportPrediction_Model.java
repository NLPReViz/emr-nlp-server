/**
 * 
 */
package frontEnd.serverSide.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class ReportPrediction_Model {
	public ReportPrediction_Model() {};
	
//	private String m_classifierName;
	private String m_classification;
	private double m_confidence;
	private List<FeatureWeight> m_topNegative;
	private List<FeatureWeight> m_topPositive;
	
//	public String getClassifierName() {
//		return m_classifierName;
//	}
//	
//	public void setClassifierName(String classifierName) {
//		m_classifierName = classifierName;
//	}
	
	public String getClassification() {
		return m_classification;
	}
	
	public void setClassification(String classification) {
		m_classification = classification;
	}
	
	public double getConfidence() {
		return m_confidence;
	}
	
	public void setConfidence(double confidence) {
		m_confidence = confidence;
	}
	
	public List<FeatureWeight> getTopNegative() {
		return m_topNegative;
	}
	
	public void setTopNegative(List<FeatureWeight> topNegative) {
		m_topNegative = topNegative;
	}
	
	public List<FeatureWeight> getTopPositive() {
		return m_topPositive;
	}
	
	public void setTopPositive(List<FeatureWeight> topPositive) {
		m_topPositive = topPositive;
	}
	
	public String toString() {
		String objStr = "classification: " + m_classification + "\nconfidence: " + m_confidence + "\nNegative list:\n";
		for(FeatureWeight fw : m_topNegative) {
			objStr += fw.toString();
		}
		objStr += "Positive list: \n";
		for(FeatureWeight fw : m_topPositive) {
			objStr += fw.toString();
		}
		return objStr;
	}
}
