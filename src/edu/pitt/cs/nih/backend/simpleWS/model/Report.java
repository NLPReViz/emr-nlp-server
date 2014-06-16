/**
 * 
 */
package edu.pitt.cs.nih.backend.simpleWS.model;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class Report {
	private String m_id;
	private String m_colonoscopyReport;
	private String m_pathologyReport;
	private String m_predConfidence;
	private HashMap<String, Double> m_trueClassConfidence;
	
	public Report() {}
	
	public Report(String id) {
		m_id = id;
	}
	
	public String getId() {
		return m_id;
	}
	
	public String getColonoscopyReport() {
		return m_colonoscopyReport;
	}
	
	public String getPathologyReport() {
		return m_pathologyReport;
	}
	
	public double getTrueClassConfidence(String varID) {
		return m_trueClassConfidence.get(varID);
	}
	
	public String getPredConfidence() {		
		return m_predConfidence;
	}
	
	public void setId(String id) {
		m_id = id;
	}
	
	public void setColonoscopyReport(String colonoscopyReport) {
		m_colonoscopyReport = colonoscopyReport;
	}
	
	public void setPathologyReport(String pathologyReport) {
		m_pathologyReport = pathologyReport;
	}
	
	public void setTrueClassConfidence(double trueClassConfidence,
			String varID) {
		try {
			m_trueClassConfidence.put(varID, trueClassConfidence);
		}
		catch(NullPointerException e) {
			m_trueClassConfidence = new HashMap<>();
			m_trueClassConfidence.put(varID, trueClassConfidence);
		}
	}
	
	public void setPredConfidence(String predConfidence) {
		m_predConfidence = predConfidence;
	}
}
