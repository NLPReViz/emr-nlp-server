/**
 * 
 */
package frontEnd.serverSide.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class Classifier_Model {
	public Classifier_Model() {};
	
//	private String m_classifierName;
	private int m_numNegative;
	private int m_numPositive;
	private List<FeatureWeight> m_topNegative;
	private List<FeatureWeight> m_topPositive;
	
//	public String getClassifierName() {
//		return m_classifierName;
//	}
//	
//	public void setClassifierName(String classifierName) {
//		m_classifierName = classifierName;
//	}
	
	public int getNumNegative() {
		return m_numNegative;
	}
	
	public void setNumNegative(int numNegative) {
		m_numNegative = numNegative;
	}
	
	public int getNumPositive() {
		return m_numPositive;
	}
	
	public void setNumPositive(int numPositive) {
		m_numPositive = numPositive;
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
	
	public void normalizeTopFeatures(double totalWeight) {
		FeatureWeight.normalizeFeatureWeights(m_topNegative, totalWeight);
		FeatureWeight.normalizeFeatureWeights(m_topPositive, totalWeight);
	}
	
	public void normalizeTopFeatures() {
		List<List<FeatureWeight>> weightList = new ArrayList<>();
		weightList.add(m_topNegative);
		weightList.add(m_topPositive);
		double totalWeight = FeatureWeight.getTotalWeight(weightList);
		FeatureWeight.normalizeFeatureWeights(m_topNegative, totalWeight);
		FeatureWeight.normalizeFeatureWeights(m_topPositive, totalWeight);
	}
}
