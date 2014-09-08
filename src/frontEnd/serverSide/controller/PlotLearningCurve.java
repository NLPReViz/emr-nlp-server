/**
 * 
 */
package frontEnd.serverSide.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.pitt.cs.nih.backend.featureVector.ColonoscopyDS_SVMLightFormat;
import edu.pitt.cs.nih.backend.featureVector.FeatureVector;
import edu.pitt.cs.nih.backend.utils.RunCmdLine;
import edu.pitt.cs.nih.backend.utils.Util;
import edu.pitt.cs.nih.backend.utils.XMLUtil;
import emr_vis_nlp.ml.LibLinearPredictor;

/**
 * Explore the data set to choose a good starting number of initial training instances
 * @author Phuong Pham
 *
 */
public class PlotLearningCurve {
	
	String fn_testFeature;
	
	public PlotLearningCurve() {
		try {
			fn_testFeature = Util.getOSPath(
					new String[]{Storage_Controller.getLibSVMPath(),
							"test-feature.txt"});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runExp() {
		try {			
			String fn_test = Util.getOSPath(new String[]{
					Storage_Controller.getDocumentListFolder(), "devIDList.xml"});
			String fn_train = Util.getOSPath(new String[]{
					Storage_Controller.getDocumentListFolder(), "fullTrainIDList.xml"});
			
//			// create full training list and test feature file
//			String fn_full = Util.getOSPath(new String[]{
//					Storage_Controller.getDocumentListFolder(), "fullIDList.xml"});
//			createFullTrainingSet(fn_full, fn_test, fn_train);
			
			// plot the learning curve
			List<String> testIDList = XMLUtil.getReportIDFromXMLList(fn_test);
			String[] varIDList = new String[]{
					"any-adenoma", "appendiceal-orifice", "asa", "biopsy", "cecum",
					"ileo-cecal-valve", "indication-type", "informed-consent",
					"nursing-report", "prep-adequateNo", "prep-adequateNot",
					"prep-adequateYes", "proc-aborted", "withdraw-time"};
			for(String varID : varIDList) {
				// generate perf for all points on the curve (300 points)
				runLearningCurve(varID, fn_train, testIDList);
//				// extract and plot
//				int startWith = 200;
//				plotLearningCurve(varID, startWith);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void runLearningCurve(String varID, String fn_train,
			List<String> testIDList) throws Exception {
		List<String> fullTrainIDList;
//		fullTrainIDList = XMLUtil.getReportIDFromXMLList(fn_train);
		fullTrainIDList = stratifyTrainSet(XMLUtil.getReportIDFromXMLList(fn_train),
				varID);
		
//		int[] nInstanceList = new int[]{30, 60, 90, 120, 150, 180,
//				210, 240, 270, 300};
		int[] nInstanceList = generateAllDataPoint(300);
		
		double[][] perfList = new double[nInstanceList.length][];
		for(int i = 0; i < nInstanceList.length; i++) {
			int nInstance = nInstanceList[i];
			System.out.println("#=" + nInstance);
			perfList[i] = runWith(varID, nInstance, fullTrainIDList, testIDList);
			
//			// debug
//			System.out.print(nInstance + ":");
//			for(double perf : perfList[i]) {
//				System.out.print(perf + ",");
//			}
//			System.out.println();
		}
		// save data
		savePerfData(nInstanceList, perfList, varID);
	}
	
	public void plotLearningCurve(String varID, int nMax) throws Exception {
		slicePerformanceData(varID, nMax);
		plot(varID);
	}
	
	public int[] generateAllDataPoint(int total) throws Exception {
		int[] allPoints = new int[total];
		for(int i = 0; i < total; i++) {
			allPoints[i] = i + 1;
		}
		
		return allPoints;
	}
	
	public void slicePerformanceData(String varID, int max) throws Exception {
		String[] originalList = Util.loadList(getFullDataFn(varID));
		StringBuilder sb = new StringBuilder(originalList[0]);
		sb.append("\n");
		for(int i = 1; i < max; i++) {
			sb.append(originalList[i]).append("\n");
		}
		Util.saveTextFile(getDataFn(varID), sb.toString());
	}
	
	public List<String> stratifyTrainSet(List<String> originalTrainSet, 
			String varID) throws Exception {
		Map<String,List<String>> labelMap = new HashMap<>();
		String fn_class = Storage_Controller.getClassFn(varID);
		String[][] labelTable = Util.loadTable(fn_class);
		for(int i = 1; i < labelTable.length; i++) {
			String[] labelRow = labelTable[i];
			try {
				List<String> labelList = labelMap.get(labelRow[1]);
				labelList.add(labelRow[0]);
				labelMap.put(labelRow[1], labelList);
			} catch(NullPointerException e) {
				List<String> labelList = new ArrayList<>();
				labelList.add(labelRow[0]);
				labelMap.put(labelRow[1], labelList);
			}
		}
		
		// compute ratio
		double labelRatio01 = labelMap.get("0").size() * 1.0 / labelMap.get("1").size();
		
		int[] labelCount = new int[2];
		int minIndex, maxIndex;
		if(labelRatio01 > 1) { // label 0 > label 1
			labelCount[0] = (int) Math.floor(labelRatio01);
			labelCount[1] = 1;
			minIndex = 1;
			maxIndex = 0;
		}
		else {// label 1 > label 0
			labelCount[0] = 1;
			labelCount[1] = (int) Math.floor(labelMap.get("1").size() * 1.0 / labelMap.get("0").size());
			minIndex = 0;
			maxIndex = 1;
		}
		
		// distribute report IDs according to the ratio
		// consider this is a n-fold stratify where n is 
		// the number of the min label
		List<String> stratifiedList = new ArrayList<>();
		int nCount = 0;
		int nMaxCount = 0;
		
		List<String> minList = labelMap.get(Integer.toString(minIndex));
		List<String> maxList = labelMap.get(Integer.toString(maxIndex));
		for(int iMin = 0; iMin < minList.size(); iMin++) {
			stratifiedList.add(minList.get(iMin));
			nCount++;
			for(int iMax = 0; iMax < labelCount[maxIndex]; iMax++) {
				if(nMaxCount < maxList.size()) {
					stratifiedList.add(maxList.get(nMaxCount++));
					nCount++;
				}
			}
		}
		
		while(nMaxCount < maxList.size()) {
			stratifiedList.add(maxList.get(nMaxCount++));
		}
		
//		// debug
//		Map<String,String> labelVarMap = new HashMap<>();
//		String[][] classValueTable = Util.loadTable(Storage_Controller.getClassFn(varID));
//		for(int i = 1; i < classValueTable.length; i++) {
//			labelVarMap.put(classValueTable[i][0], classValueTable[i][1]);
//		}
//		for(String reportID : stratifiedList) {
//			System.out.println("Report " + reportID + " , " + labelVarMap.get(reportID));
//		}
		
		return stratifiedList;
	}
	
	public double[] runWith(String varID, int nInstance, List<String> fullTrainIDList,
			List<String> testIDList) throws Exception {
		List<String> trainIDList = extractTrainIDList(fullTrainIDList, nInstance);
		// create learning files
		createLearningFileSet(varID, trainIDList);
		// build model
		buildModel();
		// create test file
		createTestFiles(varID, testIDList);
		// evaluate
		double[] perfMeasure = getPerfomanceMeasure();
		
		return perfMeasure;
	}
	
	public void savePerfData(int[] nInstances, double[][] perfMeasure, String varID) throws Exception {
		StringBuilder sb = new StringBuilder();
		// add header
		sb.append("nInstances,");
		for(String perf : perfName) {
			sb.append(perf).append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append("\n");
		
		// add content
		for(int i = 0; i < nInstances.length; i++) {
			sb.append(nInstances[i]).append(",");
			for(double perf : perfMeasure[i]) {
				sb.append(perf * 100).append(",");
			}
			sb.setLength(sb.length() - 1);
			sb.append("\n");
		}
		
		Util.saveTextFile(getFullDataFn(varID), sb.toString());
	}
	
	public String getFullDataFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{Storage_Controller.getLibSVMPath(), "latex", varID + "-full-data.csv"});
	}
	
	public String getDataFn(String varID) throws Exception {
		return Util.getOSPath(new String[]{Storage_Controller.getLibSVMPath(), "latex", varID + "-data.csv"});
	}
	
	public String getDataShortFn(String varID) throws Exception {
		return varID + "-data.csv";
	}
	
	public List<String> extractTrainIDList(List<String> fullTrainIDList, int nInstance)
			throws Exception {
		ArrayList<String> trainIDList = new ArrayList<>();
		
		for(int i = 0; i < nInstance; i++) {
			trainIDList.add(fullTrainIDList.get(i));
		}
		
		return trainIDList;
	}
	
	/**
	 * Extract all reportIDs not belong to the final test set to create the training set
	 * 
	 * @param fn_full
	 * @param fn_test
	 * @param fn_train
	 * @throws Exception
	 */
	public void createFullTrainingSet(String fn_full, String fn_test,
			String fn_train) throws Exception {
		List<String> fullIDList = XMLUtil.getReportIDFromXMLList(fn_full);
		List<String> testIDList = XMLUtil.getReportIDFromXMLList(fn_test);
		ArrayList<String> trainIDList = new ArrayList<>();
		
		for(String id : fullIDList) {
			if(!testIDList.contains(id)) {
				trainIDList.add(id);
			}
		}
		
		// save train set		
		XMLUtil.createXMLDatasetFileFromList("Full_Train_LearningCurve",
				trainIDList.toArray(new String[trainIDList.size()]),
				"Document", fn_train);
		System.out.println("Train list: " + trainIDList.size());
		System.out.println("Test list: " + testIDList.size());
	}
	
	public void createTestFiles(String varID, List<String> testIDList) throws Exception {
		String fn_index = Storage_Controller.getTempLearningIndexFn();
		String fn_instanceWeight = Util.getOSPath(
				new String[]{Storage_Controller.getLibSVMPath(), "test-weight.txt"});
		boolean includeBiasFeature = true;
		String fn_globalFeatureVector = Storage_Controller.getGlobalFeatureVectorFn();
		String docsFolder = Storage_Controller.getDocsFolder();
		double C = 1.0;
		double C_contrast = 0;
		double mu = 0;
		
		ColonoscopyDS_SVMLightFormat libSVM = new ColonoscopyDS_SVMLightFormat();
		libSVM.setClassValueMap(varID);
		FeatureVector fv = libSVM.getFeatureVectorFromReportList(fn_globalFeatureVector, docsFolder,
				testIDList);
		libSVM.createLearningFileFromFeatureVector(fv, fn_testFeature, fn_index,
				includeBiasFeature, fn_globalFeatureVector);
		libSVM.mergeCostList(fn_index, fn_testFeature, fn_instanceWeight, C, C_contrast, mu);
		// delete the instance weight file because it will not be used anymore
		Util.deleteFile(fn_instanceWeight);
	}
	
	public double[] getPerfomanceMeasure() throws Exception {
		String fn_prediction = Storage_Controller.getPredictionFn();
		LibLinearPredictor libSVM = new LibLinearPredictor();
    	String[] svmTestParams = new String[] {Storage_Controller.getLibSVMPath(),
                 fn_testFeature, Storage_Controller.getTempModelFn(), fn_prediction};
    	double[][] predictionMatrix;
    	int[][] confusionMatrix;
    	double[] perfMeasures;
    	double[] fullMeasure = new double[8];
    	
    	predictionMatrix = libSVM.predict((Object)svmTestParams);
		
		confusionMatrix = libSVM.getConfusionMatrix(fn_testFeature, predictionMatrix);

		perfMeasures = libSVM.getPerformanceMeasure(confusionMatrix, 0);
		for(int i = 0; i < perfMeasures.length; i++) {
			fullMeasure[i] = perfMeasures[i];
		}
		
		perfMeasures = libSVM.getPerformanceMeasure(confusionMatrix, 1);
		for(int i = 0; i < perfMeasures.length; i++) {
			fullMeasure[i + 4] = perfMeasures[i];
		}
		
    	return fullMeasure;
	}
	
	public void createLearningFileSet(String varID, List<String> instanceIDList) throws Exception {
		String fn_featureVector = Storage_Controller.getTempLearningFeatureFn();
		String fn_index = Storage_Controller.getTempLearningIndexFn();
		String fn_instanceWeight = Storage_Controller.getTempLearningWeightFn();
		boolean includeBiasFeature = true;
		String fn_globalFeatureVector = Storage_Controller.getGlobalFeatureVectorFn();
		String docsFolder = Storage_Controller.getDocsFolder();
		double C = 1.0;
		double C_contrast = 0;
		double mu = 0;
		
		ColonoscopyDS_SVMLightFormat libSVM = new ColonoscopyDS_SVMLightFormat();
		libSVM.setClassValueMap(varID);
		FeatureVector fv = libSVM.getFeatureVectorFromReportList(fn_globalFeatureVector, docsFolder,
				instanceIDList);
		libSVM.createLearningFileFromFeatureVector(fv, fn_featureVector, fn_index,
				includeBiasFeature, fn_globalFeatureVector);
		libSVM.mergeCostList(fn_index, fn_featureVector, fn_instanceWeight, C, C_contrast, mu);
	}
	
	public void buildModel() throws Exception {
		// get the training file
    	String fn_featureVectorOut = Storage_Controller.getTempLearningFeatureFn();
    	String fn_instanceWeight = Storage_Controller.getTempLearningWeightFn();
    	String fn_model = Storage_Controller.getTempModelFn();
    	LibLinearPredictor svm = new LibLinearPredictor();
    	String[] svmTrainParams = new String[] {Storage_Controller.getLibSVMPath(),
                fn_featureVectorOut, fn_model, fn_instanceWeight};
        // train the model
    	svm.train((Object)svmTrainParams);
        
        // save model
    	svm.saveModel(fn_model);
	}
	
	// ------------------------------LATEX PLOT------------------------------------------ //
	
	public void plot(String varID) throws Exception {
		StringBuilder sbLatex = new StringBuilder();
//		int[] perfIndexList = new int[]{2,3,6}; // F-, F+, Acc
		int[] perfIndexList = new int[]{6}; // F+
		
		// add header
		addLatexHeader(varID, sbLatex);
//		// add data
//		addLatexDataContent(nInstanceList, perfList, perfIndex, sbLatex);
//		createPlotDataContent(varID, nInstanceList, perfList, perfIndex);
		
		// plot the graph
//		addLatexPlot(sbLatex, maxInstance, perfIndex);
		for(int perfIndex : perfIndexList) {
			addLatexPlotCSV(sbLatex, varID, perfIndex);
		}
		// add footer
		addLatexFooter(sbLatex);
		
		// create tex file
		String fn_latex = Util.getOSPath(new String[]{
				Storage_Controller.getLibSVMPath(), "latex",
				"plot-" + varID + ".tex"});
		Util.saveTextFile(fn_latex, sbLatex.toString());
		
//		// create pdf file
//		String cmd = "cd " + Util.getOSPath(new String[]{Storage_Controller.getLibSVMPath(), "latex"});
//		RunCmdLine.runCommand(cmd, 1);
//		cmd = "pdflatex " + fn_latex;
//		RunCmdLine.runCommand(cmd, 1);
	}
	
	public void addLatexHeader(String varID, StringBuilder sbLatex) throws Exception {
		sbLatex.append("\\documentclass{minimal}\n")
			.append("% learning curve plot of variable ")
			.append(varID).append("\n")
			.append("% Author: Phuong Pham\n")
			.append("\\usepackage{pgfplots}\n")
			.append("\\usepackage{pdflscape}\n\n")
//			.append("\\usepackage{tikz}\n")
//			.append("\\usetikzlibrary{plotmarks}\n\n")
			.append("\\begin{document}\n")
			.append("\\begin{landscape}\n")
			.append("\t\\begin{tikzpicture}\n")
			.append("\t\t\\begin{axis}[xlabel=\\# instances,\n")
			.append("\t\t\tylabel=Performance,\n")
			.append("\t\t\tlegend pos= south east,\n")
			.append("\t\t\tscaled ticks=false,\n")
			.append("\t\t\ttick label style={/pgf/number format/fixed},\n")
			.append("\t\t\twidth=0.96\\linewidth]\n\n");
	}	
	
	String[] perfName = new String[]{"PreNeg", "RecNeg", "FNeg", "Acc", "PrePos", "RecPos", "FPos", "Acc"};
	String[] perfColorList = new String[]{};
	String[] perfMarkerList = new String[]{};
	
	public void createPlotDataContent(String varID, int[] nInstanceList, double[][] perfList,
			int perfIndex) throws Exception {
		StringBuilder sb = new StringBuilder();
		// header
		sb.append("nInstances,").append(perfName[perfIndex]).append("\n");
		// data
		for(int i = 0; i < nInstanceList.length; i++) {
			sb.append(nInstanceList[i])
				.append(",")
				.append(perfList[i][perfIndex])
				.append("\n");
		}
		
		Util.saveTextFile(getPlotDataFullFn(varID, perfIndex), sb.toString());
	}
	
	public String getPlotDataFn(String varID, int perfIndex) throws Exception {
		return varID + perfName[perfIndex] + ".csv";
	}
	
	public String getPlotDataFullFn(String varID, int perfIndex) throws Exception {
		return Util.getOSPath(new String[]{Storage_Controller.getLibSVMPath(),
				"latex", varID + perfName[perfIndex] + ".csv"});
	}
	
	public void addLatexDataContent(int[] nInstanceList, double[][] perfList,
			int perfIndex, StringBuilder sbLatex) throws Exception {
		sbLatex.append("% The data file for ")
			.append(perfName[perfIndex])
			.append("\n")
			.append("\\begin{filecontents}{div_")
			.append(perfName[perfIndex])
			.append(".data}\n")
			.append("\\# instances\t")
			.append(perfName[perfIndex])
			.append("\n");
		
		for(int i = 0; i < nInstanceList.length; i++) {
			sbLatex.append(nInstanceList[i])
				.append("\t")
				.append(perfList[i][perfIndex])
				.append("\n");
		}
		
		sbLatex.append("\\end{filecontents}\n\n");
	}
	
	public void addLatexPlotCSV(StringBuilder sbLatex, String varID, int perfIndex) throws Exception {
		sbLatex
//			.append("\t\t\\begin{axis}\n")
			.append("\t\t\t\\addplot table [x=nInstances, y=")
			.append(perfName[perfIndex])
			.append(", col sep=comma] {")
			.append(getDataShortFn(varID))
			.append("};\n")
			.append("\t\t\t\t\\addlegendentry{")
			.append(perfName[perfIndex])
			.append("}\n\n");
//			.append("\t\t\\end{axis}\n")
			
	}
	
	public void addLatexPlot(StringBuilder sbLatex, int maxInstance, int perfIndex) throws Exception {
		sbLatex.append("\\begin{document}\n")
			.append("\\begin{tikzpicture}[y=.2cm, x=.7cm,font=\\sffamily]\n");
		
		// draw axis
		sbLatex.append("%axis\n")
			.append("\t\\draw (0,0) -- coordinate (x axis mid) (")
			.append(maxInstance)
			.append(",0);\n")
			.append("\t\\draw (0,0) -- coordinate (y axis mid) (0,1);\n");
		
//		// draw tick
//		sbLatex.append("%ticks\n");
//    	\foreach \x in {0,...,10}
//     		\draw (\x,1pt) -- (\x,-3pt)
//			node[anchor=north] {\x};
//    	\foreach \y in {0,5,...,30}
//     		\draw (1pt,\y) -- (-3pt,\y) 
//     			node[anchor=east] {\y}; ");
		
		// draw label
		sbLatex.append("%labels\n")      
			.append("\t\\node[below=0.8cm] at (x axis mid) {\\# instances};\n")
			.append("\t\\node[rotate=90, above=0.8cm] at (y axis mid) {")
			.append(perfName[perfIndex])
			.append("};\n");
		
		// draw graph
		sbLatex.append("%plots\n")
			.append("\t\\draw plot[mark=*, mark options={fill=white}]\n") 
			.append("\t\tfile {div_")
			.append(perfName[perfIndex])
			.append(".data};\n");
//			.append("\t\\draw plot[mark=triangle*, mark options={fill=white} ]\n") 
//			.append("\t\tfile {div_")
//			.append(perfName[perfIndex])
//			.append(".data};\n")
//			.append("\t\\draw plot[mark=square*, mark options={fill=white}]\n")
//			.append("\t\tfile {div_")
//			.append(perfName[perfIndex])
//			.append(".data};\n");
			
		sbLatex.append("\\end{tikzpicture}\n")
			.append("\\end{document}\n");
		
	}
	
	public void addLatexFooter(StringBuilder sbLatex) throws Exception {
		sbLatex
			.append("\t\t\\end{axis}\n")
			.append("\t\\end{tikzpicture}\n")
			.append("\\end{landscape}\n")
			.append("\\end{document}");
	}
}
