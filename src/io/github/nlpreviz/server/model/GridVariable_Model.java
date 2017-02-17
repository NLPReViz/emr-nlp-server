/**
 * 
 */
package io.github.nlpreviz.server.model;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class GridVariable_Model {
	private Map<String, Object> m_gridVar;
	
	public Map<String, Object> getGridVar() {
		return m_gridVar;
	}
	
	public void setGridVar(Map<String, Object> gridVar) {
		m_gridVar = gridVar;
	}
}
