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
	private List<String> m_docIDPositive;
	private List<String> m_docIDNegative;
	private List<String> m_docIDUnclassified;
	
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
	
	public List<String> getDocPositive() {
		return m_docIDPositive;
	}
	
	public void setDocPositive(List<String> docIDPositiveList) {
		m_docIDPositive = docIDPositiveList;
	}
	
	public List<String> getDocNegative() {
		return m_docIDNegative;
	}
	
	public void setDocNegative(List<String> docIDNegativeList) {
		m_docIDNegative = docIDNegativeList;
	}
	
	public List<String> getDocUnclassified() {
		return m_docIDUnclassified;
	}
	
	public void setDocUnclassified(List<String> docIDUnclassifiedList) {
		m_docIDUnclassified = docIDUnclassifiedList;
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
