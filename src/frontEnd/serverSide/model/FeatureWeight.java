/**
 * 
 */
package frontEnd.serverSide.model;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import edu.pitt.cs.nih.backend.utils.Util;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class FeatureWeight implements Comparable<FeatureWeight>, Cloneable {
	public FeatureWeight() {};
	
	private String m_term;
	private double m_weight;
	private List<String> m_matchedList;
	
	public String getTerm() {
		return m_term;
	}
	
	public void setTerm(String term) {
		m_term = term;
	}
	
	public double getWeight() {
		return m_weight;
	}
	
	public void setWeight(double weight) {
		m_weight = weight;
	}
	
	public List<String> getMatchedList() {
		return m_matchedList;
	}
	
	public void setMatchedList(List<String> matchedList) {
		m_matchedList = matchedList;
	}
	
	/**
	 * Normalize weights of features
	 * 
	 * @param topFeatures
	 * @return
	 */
	public static void normalizeFeatureWeights(List<FeatureWeight> topFeatures,
			double totalWeight) {
		if(totalWeight > 0) {
			int numDec = 2;
			for(FeatureWeight fw : topFeatures) {
				fw.setWeight(Util.round(Math.abs(fw.getWeight()) / totalWeight, numDec));
			}
		}
	}
	
	public static double getTotalWeight(String[][] featureWeightTable) {
		double totalWeight = 0;
		for(int i = 0; i < featureWeightTable.length; i++) {
			totalWeight += Math.abs(Double.parseDouble(featureWeightTable[i][1]));
		}
		
		return totalWeight;
	}
	
	public static double getTotalWeight(List<List<FeatureWeight>> weightList) {
		double totalWeight = 0;
		for(List<FeatureWeight> subList : weightList) {
			for(FeatureWeight fw : subList) {
				totalWeight += Math.abs(fw.getWeight());
			}
		}
		return totalWeight;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
	    if (o == this) return true;
	    if (!(o instanceof FeatureWeight))return false;
	    FeatureWeight otherFeatureWeight = (FeatureWeight) o;
	    if(m_term.equals(otherFeatureWeight.m_term) &&
	    		m_weight == otherFeatureWeight.m_weight) {
	    	return true;
	    }
	    else {
	    	return false;
	    }
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(FeatureWeight o) {
		if(Math.abs(this.m_weight) > Math.abs(o.m_weight)) {
//			return 1; // ascending
			return -1; // descending
		}
		else if(this.m_weight == o.m_weight) {
			return 0;
		}
		else {
//			return -1; // ascending
			return 1; // descending
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		FeatureWeight cloned = new FeatureWeight();
		cloned.m_term = m_term;
		cloned.m_weight = m_weight;
		
		return cloned;
	}
}
