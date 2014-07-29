/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import frontEnd.serverSide.model.MLModel;
import frontEnd.serverSide.model.DataSet;


/**
 * @author Phuong Pham
 *
 */
public enum Dataset_MLModel_Controller {
	instance;
	
	private String m_documentListFolder;
	private String m_modelListFolder;
	
	private Dataset_MLModel_Controller() {
		try {
			m_documentListFolder = Storage_Controller.getDocumentListFolder();
			m_modelListFolder = Storage_Controller.getModelListFolder();			
		}
		catch (Exception e) {
		}
	}
	
	public Map<String, Object> getAllDataSetModel() throws Exception {
		List<MLModel> modelList = getMLModelList();
		List<DataSet> dataSetList = getDataSetList();
		
		Map<String, Object> dataSet_ModelList = new HashMap<>();
		dataSet_ModelList.put("dataset", dataSetList);
		dataSet_ModelList.put("model", modelList);
		return dataSet_ModelList;
	}
	
	protected List<MLModel> getMLModelList() throws Exception {
		String[] fn_MLModelList = Util.loadFileList(m_modelListFolder);
		List<MLModel> MLModelList = new ArrayList<>();
		for(int i = 0; i < fn_MLModelList.length; i++) {
			MLModel mlModel = new MLModel();
			mlModel.setName(fromFnModel2ModelName(fn_MLModelList[i]));
			mlModel.setId(Integer.toString(i + 1));
			MLModelList.add(mlModel);
		}
		
		// sort model list
		Collections.sort(MLModelList);
		
		return MLModelList;
	}
	
	protected List<MLModel> sortMLModelList(List<MLModel> inputList) throws Exception {
		int[] sessionIDList = new int[inputList.size()];
		for(int i = 0; i < sessionIDList.length; i++) {
			
		}
		
		List<MLModel> sortedList = new ArrayList<>();
		return sortedList;
	}
	
	protected List<DataSet> getDataSetList() throws Exception {
		String[] fn_dataSetList = Util.loadFileList(m_documentListFolder);
		List<DataSet> dataSetList = new ArrayList<>();
		for(int i = 0; i < fn_dataSetList.length; i++) {
			DataSet dataSet = new DataSet();
			dataSet.setName(fromFnModel2ModelName(fn_dataSetList[i]));
			dataSet.setDocIds(XMLUtil.getReportIDFromXMLList(
					Util.getOSPath(new String[]{m_documentListFolder,
							fn_dataSetList[i]})));
			dataSet.setId(Integer.toString(i + 1));
			dataSetList.add(dataSet);
		}
		
		return dataSetList;
	}
	
	protected String fromFnModel2ModelName(String fn_model) throws Exception {
		return fn_model.substring(0, fn_model.lastIndexOf(".")); // 0..biopsy.model -> 0..biopsy or initialIDList.xml -> initialID
	}
	
	public static String toJSONString(List<MLModel> model, List<DataSet> dataset) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		// dataset list
		sb.append("\t\"dataset\": [\n");
		for(int i = 0; i < dataset.size(); i++) {
			sb.append("\t\t{\n");
			sb.append("\t\t\t\"name\": \"").append(dataset.get(i).getName()).append("\",\n");
			sb.append("\t\t\t\"docIds\": [\n");
			for(int j = 0; j < dataset.get(i).getDocIds().size(); j++) {
				sb.append("\t\t\t\t\"").append(dataset.get(i).getDocIds().get(j)).append("\",\n");
			}
			// delete the last ,
			sb.setLength(sb.length() - 2);
			sb.append("\n");
			sb.append("\t\t\t]\n");
			sb.append("\t\t},\n");
		}
		sb.setLength(sb.length() - 2);
		sb.append("\n");
		sb.append("\t],\n");
		// model list
		sb.append("\t\"model\": [\n");
		sb.append("\t\t{\n");
		for (int i = 0; i < model.size(); i++) {
			sb.append("\t\t\t\"name\": \"").append(model.get(i).getName()).append("\",\n");			
		}
		// delete the last ,
		sb.setLength(sb.length() - 2);
		sb.append("\n");
		sb.append("\t\t}\n");		
		sb.append("\t]\n");
		sb.append("}");
		
		return sb.toString();
	}
}
