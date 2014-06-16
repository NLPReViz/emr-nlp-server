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
public class DataSet {
	public DataSet(){};
	
	protected String m_name;
	protected List<String> m_docIDList;
	protected String m_id;
	
	public String getName() {
		return m_name;
	}
	
	public void setName(String name) {
		m_name = name;
	}
	
	public List<String> getDocIds() {
		return m_docIDList;
	}
	
	public void setDocIds(List<String> docIdList) {
		m_docIDList = docIdList;
	}
	
	public String getId() {
		return m_id;
	}
	
	public void setId(String id) {
		m_id = id;
	}
}
