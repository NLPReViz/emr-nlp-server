/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class test {
    public static void main(String[] args) {
        test t = new test();
        try {t.runTest();} catch (Exception e) {System.out.println(e.getMessage());}
    }
    
    //public static String[] varNameList = new String[] {"any-adenoma",
    //    "appendiceal-orifice", "asa", "biopsy", "cecum", "ileo-cecal-valve",
    //    "indication-type", "informed-consent", "nursing-report", "prep-adequateNo",
    //    "prep-adequateNot", "prep-adequateYes", "proc-aborted", "withdraw-time"};

    public static String[] varNameList = new String[]{
                        "public-health"};

    public static int[] foldList = new int[] {0, 1, 2, 3, 4};
//    public static String[] varNameList = new String[] {"any-adenoma"};
//    public static int[] foldList = new int[] {0};
    
    protected static void runTest() throws Exception {
//        testSaveFeatureWeight();
//        testTextUtil();
        testPattern();
    }
    
    protected static void testPattern() throws Exception {
        String text = "waef wef abd\ndefgtr           \n\t\nytjut 67i ";
        String span = "abd\tdefgtr\nytjut";
        Pattern pattern = Pattern.compile(span.replaceAll("\\s+", "\\\\s+"));
        Matcher m = pattern.matcher(text);
        System.out.println(m.find());
    }
    
    protected static void testTextUtil() throws Exception {
//        String sentence = "COLONOSCOPY REPORT\n" +
//"\n" +
//"ID#:  **ID-NUM  PROCEDURE:   Colonoscopy, diagnostic\n" +
//"NAME:  **NAME[AAA, BBB]  **NAME[CCC:   YYY ZZZ], MD\n" +
//"SEX:  female  FELLOW:\n" +
//"ASSISTANT:    **NAME[XXX WWW], RN and **NAME[VVV UUU], RN\n" +
//"DOB:  05/17/1951  WARD:   outpatient\n" +
//"AGE:  56  DATE:   03/27/2008\n" +
//"\n" +
//"ASA CLASS:  Class II\n" +
//"INDICATIONS:  1) change in bowel habits\n" +
//"MEDICATION:  200 micrograms Fentanyl IV, 6 milligrams Versed IV, 50 milligrams\n" +
//"Benadryl IV\n" +
//"PREPARATION OF COLON:  good\n" +
//"\n" +
//"FINDINGS: After the risks benefits and alternatives of the procedure were\n" +
//"thoroughly explained, informed consent was obtained.  Digital rectal exam was\n" +
//"performed and revealed no abnormalities.   The Pentax EC-3870LK endoscope was\n" +
//"introduced through the anus and advanced to the ascending colon.  The quality\n" +
//"of the prep was good.  The instrument was then slowly withdrawn as the colon\n" +
//"was fully examined. It was not possible to reach the cecum.\n" +
//"\n" +
//"The ascending, transverse, descending, sigmoid colon, and rectum appeared\n" +
//"unremarkable.   Retroflexed views in the rectum revealed no abnormalities.\n" +
//"The scope was then completely withdrawn from the patient and the procedure\n" +
//"terminated.\n" +
//"\n" +
//"COMPLICATIONS:  None\n" +
//"ENDOSCOPIC IMPRESSION:  1) Normal colon\n" +
//"\n" +
//"RECOMMENDATIONS:\n" +
//"REPEAT EXAM:  No\n" +
//"INSTRUCTIONS:\n" +
//"\n" +
//"______________________________";
//        List<String[]> tokensPerLineList = TextUtil.extractWordsByStandfordParser(sentence);
//        int temp = 0;
        String test = "I be eat an apple";
        System.out.println(test.indexOf("apple"));
    }
    
    protected static void testSaveFeatureWeight() throws Exception {
        String folder = Util.getOSPath(new String[] {Util.getExecutingPath(),
                "alaska-data", "emr-vis-nlp_colonoscopy2", "modelsWeights"});
        String fn_featureWeight;
        String fn_featureWeightDist;
        String[][] featureWeightTable;
        HashMap<Double, Integer> valueCountMap = new HashMap<>();
        double value;
        int count;
        StringBuilder sb;
        Iterator iter;
        
        for(String varName : varNameList) {
            for(int iFold : foldList) {
                fn_featureWeight = getFeatureWeightFileName(folder, varName, iFold);
                fn_featureWeightDist = getFeatureWeightDistributionFileName(folder, varName, iFold);
                
                featureWeightTable = Util.loadTable(fn_featureWeight);
                valueCountMap.clear();
                
                for(int i = 0; i < featureWeightTable.length; i++) {
                    value = Math.abs(Double.parseDouble(featureWeightTable[i][1]));
                    try {
                        valueCountMap.put(value, valueCountMap.get(value) + 1);
                    }
                    catch (NullPointerException e) {
                        valueCountMap.put(value, 1);
                    }
                }
                
                sb = new StringBuilder();
                // sort the list by value
                List<Double> sortedKeys = new ArrayList(valueCountMap.keySet());
                Collections.sort(sortedKeys);
                // descending
                Collections.reverse(sortedKeys);
                for(double key : sortedKeys) {
                    count = valueCountMap.get(key);
                    sb.append(key);
                    sb.append(",");
                    sb.append(count);
                    sb.append("\n");
                }
                
                Util.saveTextFile(fn_featureWeightDist, sb.toString());
            }
        }
    }
    
    protected static String getFeatureWeightFileName(String folder, String varName,
            int iFold) throws Exception {
        return Util.getOSPath(new String[] {folder, varName + "-fold" + iFold + "of5.weight.csv"});
    }
    
    protected static String getFeatureWeightDistributionFileName(String folder, String varName,
            int iFold) throws Exception {
        return Util.getOSPath(new String[] {folder, varName + "-fold" + iFold + "of5.distribution.csv"});
    }
}
