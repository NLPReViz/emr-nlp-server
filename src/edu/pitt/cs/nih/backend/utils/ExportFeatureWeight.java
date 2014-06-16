package edu.pitt.cs.nih.backend.utils;

public class ExportFeatureWeight {

    /**
     * Save the feature weight as a CSV file. The format is: 1st row = feature
     * name; 2nd row = feature weight
     *
     * @param fn_weight
     * @param featureNames
     */
    public static void saveFeatureWeightList(String fn_weight, double[] weights,
            String[] featureNames) throws Exception {
        
        String featureWeightCSVString = ExportFeatureWeight.getFeatureWeightsCSVString(featureNames, weights);
        
        Util.saveTextFile(fn_weight, featureWeightCSVString);
    }

//    // first row is feature name, second row is weight
//    public static String getFeatureWeightsCSVString(String[] featureList, double[] weights) throws Exception {
//        //sort result descending
//        FeatureWeight[] fWeights = sortWeights(weights);
//        //save into CSV
//        String firstRow = "";
////		String debugRow = "";
//        String secondRow = "";
//        for (int i = 0; i < fWeights.length - 1; i++) {
////			//first row is feature index
////			firstRow += Integer.toString(fWeights[i].index) + ",";
//            // first row is feature name
//            firstRow += featureList[fWeights[i].index] + ",";
//            //second row is feature weight
//            secondRow += Double.toString(fWeights[i].weight) + ",";
//        }
////		firstRow += Integer.toString(fWeights[fWeights.length - 1].index);
//        firstRow += featureList[fWeights[fWeights.length - 1].index];
//        secondRow += Double.toString(fWeights[fWeights.length - 1].weight);
//        String text = firstRow + "\n" + secondRow;
//
//        return text;
//    }
    
    
    // each row is a tuple featureName, featureWeight
    public static String getFeatureWeightsCSVString(String[] featureList, double[] weights) throws Exception {
        //sort result descending
        FeatureWeight[] fWeights = sortWeights(weights);
        //save into CSV
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fWeights.length; i++) {
            sb.append(featureList[fWeights[i].index]);
            sb.append(",");
            sb.append(Double.toString(fWeights[i].weight));
            sb.append("\n");
        }

        return sb.toString().replaceAll("\\s+$", "");
    }

    public static FeatureWeight[] sortWeights(double[] weights) {
        FeatureWeight[] fWeights = new FeatureWeight[weights.length];
        //init feature weight
        for (int i = 0; i < weights.length; i++) {
            fWeights[i] = new FeatureWeight(i, weights[i]);
        }
        //sort
        Mergesort.mergesort(fWeights, 0, weights.length, false);
        return fWeights;
    }
}
