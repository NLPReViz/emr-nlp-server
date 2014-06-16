package emr_vis_nlp.ml.colon_vars;

import java.io.*;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.filters.Filter;

/**
 * Custom SVM predictor using Phuong's modified WEKA library to extract feature
 * weights. Based heavily on Phuong's ``ColonsopyModel.java'' from summer 2012.
 * * 
 * @author pnvphuong@gmail.com, alexander.p.conrad@gmail.com
 */
/**
 * @deprecated 
 * @author phuongpham
 */
public class CertSVMPredictor {
    
    protected Classifier m_Classifier = null;

    public CertSVMPredictor() {
        super();
    }

    public void loadModel(String fnModel) {
        try {
            this.m_Classifier = (Classifier) weka.core.SerializationHelper.read(fnModel);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("err: "+this.getClass().getName()+": unable to load model: "+fnModel);
        }
    }

    public void saveModel(String fnModel) throws Exception {
        weka.core.SerializationHelper.write(fnModel, this.m_Classifier);
    }

    protected double[][] predictDataDistribution(Instances unlabeled) throws Exception {
        // set class attribute
        unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

        // distribution for instance
        double[][] dist = new double[unlabeled.numInstances()][unlabeled.numClasses()];

        // label instances
        for (int i = 0; i < unlabeled.numInstances(); i++) {
//            System.out.println("debug: "+this.getClass().getName()+": classifier: "+m_Classifier.toString());
            LibSVM libsvm = (LibSVM) m_Classifier;
            libsvm.setProbabilityEstimates(true);
            double[] instanceDist = libsvm.distributionForInstance(unlabeled.instance(i));
            dist[i] = instanceDist;
        }

        return dist;
    }
    
    public double[] predictInstanceDistribution(String fnInstances) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(fnInstances));
        return predictInstanceDistribution(reader);
    }
    
    public double[] predictInstanceDistribution(Reader reader) throws Exception {
        // assume that the file contains only 1 instance
        // load instances
        Instances data = new Instances(reader);
        // remove reportID attribute
        String[] options = weka.core.Utils.splitOptions("-R 1");  // removes the first attribute in instances (should be the document id?)
        String filterName = "weka.filters.unsupervised.attribute.Remove";
        Filter filter = (Filter) Class.forName(filterName).newInstance();
        if (filter instanceof OptionHandler) {
            ((OptionHandler) filter).setOptions(options);
        }
        filter.setInputFormat(data);
        // make the instances
        Instances unlabeled = Filter.useFilter(data, filter);

        double[][] dist = this.predictDataDistribution(unlabeled);
        return dist[0];
    }

    protected void trainModel(Instances trainData) throws Exception {
        // set class attribute
        trainData.setClassIndex(trainData.numAttributes() - 1);
        // set classifier: use linear SVM only
        String[] options = weka.core.Utils.splitOptions("-K 0");
        String classifierName = "weka.classifiers.functions.LibSVM";
        this.m_Classifier = Classifier.forName(classifierName, options);
        // get probability instead of explicit prediction
        LibSVM libsvm = (LibSVM) this.m_Classifier;
        libsvm.setProbabilityEstimates(true);
        // build model
        this.m_Classifier.buildClassifier(trainData);
    }

    public void trainModelFromFile(String fnTrainData) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fnTrainData)));
        // preprocess instances
        String[] options = weka.core.Utils.splitOptions("-R 1");
        String filterName = "weka.filters.unsupervised.attribute.Remove";
        Filter filter = (Filter) Class.forName(filterName).newInstance();
        if (filter instanceof OptionHandler) {
            ((OptionHandler) filter).setOptions(options);
        }
        filter.setInputFormat(data);
        // make the instances
        Instances unlabeled = Filter.useFilter(data, filter);
        // train model
        this.trainModel(unlabeled);
    }

    // get training set in K-fold cross validation
    // e.g the first fold of 10-fold CV would be 
    // foldNumber = 0; foldTotal = 10
    public Instances getTrainSet(int foldNumber, int foldTotal, String fnData) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fnData)));
        data.setClassIndex(data.numAttributes() - 1);
        Instances trainSet = data.trainCV(foldTotal, foldNumber);

        return trainSet;
    }

    public void saveTrainSet(int foldNumber, int foldTotal, String fnData, String fnTrainSet) throws Exception {
        Instances trainSet = this.getTrainSet(foldNumber, foldTotal, fnData);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fnTrainSet));
        writer.write(trainSet.toString());
        writer.flush();
        writer.close();
    }

    // get testing set in K-fold cross validation
    // e.g the first fold of 10-fold CV would be 
    // foldNumber = 0; foldTotal = 10
    public Instances getTestSet(int foldNumber, int foldTotal, String fnData) throws Exception {
        // load instances
        Instances data = new Instances(new BufferedReader(new FileReader(fnData)));
        data.setClassIndex(data.numAttributes() - 1);
        Instances testSet = data.trainCV(foldTotal, foldNumber);

        return testSet;
    }

    public void saveTestSet(int foldNumber, int foldTotal, String fnData, String fnTestSet) throws Exception {
        Instances testSet = this.getTestSet(foldNumber, foldTotal, fnData);
        BufferedWriter writer = new BufferedWriter(new FileWriter(fnTestSet));
        writer.write(testSet.toString());
        writer.flush();
        writer.close();
    }
    
}
