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
public class TextSpan_Model {
	public TextSpan_Model(){};
	
	private int m_start;
	private int m_end;
	
	public int getStart() {
		return m_start;
	}
	
	public void setStart(int start) {
		m_start = start;
	}
	
	public int getEnd() {
		return m_end;
	}
	
	public void setEnd(int end) {
		m_end = end;
	}
}
