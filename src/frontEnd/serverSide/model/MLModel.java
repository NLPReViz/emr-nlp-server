/**
 * 
 */
package frontEnd.serverSide.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Phuong Pham
 *
 */
@XmlRootElement
public class MLModel implements Comparable<MLModel>{
	public MLModel(){};
	
	protected String m_id; // can we use name as an Id?
	protected String m_name;
	
	public String getId() {
		return m_id;
	}
	
	public void setId(String id) {
		m_id = id;
	}
	
	public String getName() {
		return m_name;
	}
	
	public void setName(String name) {
		m_name = name;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(MLModel model) {
		return this.extractSessionID() - model.extractSessionID();
	}
	
	protected int extractSessionID() {
		return Integer.parseInt(m_name.substring(m_name.indexOf('.') + 1, m_name.lastIndexOf('.')));
	}
}
