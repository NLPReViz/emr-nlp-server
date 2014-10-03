/**
 *
 */
package edu.pitt.cs.nih.backend.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

/**
 * @author Phuong Pham
 *
 * This class provides basic operations (most of them are IO operations)
 */
public class Util {

    /**
     * Mimic the join operator in Python, using this methods to create file path
     * faster
     *
     * @param text An array of which elements will be concatenate
     * @param token A token inserted between elements of text
     * @return String where elements in text are separated by the token
     * parameter
     * @throws Exception
     */
    public static String joinString(String[] text, String token) throws Exception {
        String joinedText = "";
        if (text.length >= 1) {
            // approach 1
            for (int i = 0; i < text.length - 1; i++) {
                //			joinedText += text[i] + token;
                // faster executing time
                joinedText = joinedText.concat(text[i] + token);
            }
            joinedText += text[text.length - 1];

            //		// approach 2: Arrays.toString gives [val1, val2, ...],
            //		// we get substring to have val1, val2 ...
            //		joinedText = Arrays.toString(text);
            //		joinedText = joinedText.substring(1, joinedText.length() - 1);
            //		joinedText = joinedText.replace(",", token);
        }
        return joinedText;
    }

    /**
     * Save text into a file
     *
     * @param fileName Full path of the output file
     * @param text Content needs to be saved
     */
    public static void saveTextFile(String fileName, String text) throws Exception {
        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(text);

        if (writer != null) {
            writer.close();
        }
    }

    /**
     * Load text content of a file
     *
     * @param fileName Full path of the loaded file
     * @return String content of the file fileName
     * @throws Exception
     */
    public static String loadTextFile(String fileName) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String text = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }
            text = sb.toString();
        } finally {
            br.close();
        }

        return text;
    }

    /**
     * Build full path of a file or a folder
     *
     * @param paths A string array where each component of the full path are
     * listed in order
     * @return the full path
     * @throws Exception
     */
    public static String getOSPath(String[] paths) throws Exception {
        String path = "";
        for (int iFile = 0; iFile < paths.length - 1; iFile++) {
            path += paths[iFile] + File.separator;
        }
        path += paths[paths.length - 1];

        return path;
    }

    /**
     * Load all files under a folder
     *
     * @param folder The folder contains files
     * @return An array where each element is a child file name (full path is
     * not included)
     * @throws Exception
     */
    public static String[] loadFileList(String folder) throws Exception {
        File directory = new File(folder);
        File[] files = directory.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();

        for (int iFile = 0; iFile < files.length; iFile++) {
            if (!files[iFile].isDirectory()) { 
                String fileName = files[iFile].toString();
                int startIndex = fileName.lastIndexOf(File.separatorChar) + 1;
                fileNames.add(fileName.substring(startIndex));
            }
        }
        String[] array = new String[fileNames.size()];
        array = fileNames.toArray(array);
        return array;
    }

    /**
     * Load all subfolders of a folder
     *
     * @param folder folder The folder contains subfolders
     * @return An array where each element is a subfolder name (full path is not
     * included)
     * @throws Exception
     */
    public static String[] loadSubFolderList(String folder) throws Exception {
        File directory = new File(folder);
        File[] files = directory.listFiles();
        ArrayList<String> folderNames = new ArrayList<String>();

        for (int iFile = 0; iFile < files.length; iFile++) {
            if (files[iFile].isDirectory()) {
                String folderName = files[iFile].toString();
                int startIndex = folderName.lastIndexOf(File.separatorChar) + 1;
                folderNames.add(folderName.substring(startIndex));
            }
        }
        String[] array = new String[folderNames.size()];
        array = folderNames.toArray(array);
        return array;
    }

    /**
     * Get an ancestor of a folder or file
     *
     * @param directory The folder or file to start with
     * @param offset How many levels we want to go up
     * @return The full path of the ancestor
     * @throws Exception
     */
    public static String getParentDirectoryRecursive(String directory, int offset) throws Exception {
        File fParent = new File(directory);
        for (int i = 0; i < offset; i++) {
            fParent = fParent.getParentFile();
        }
        return fParent.getAbsolutePath();
    }

    /**
     * Get the parent of a folder or file
     *
     * @param directory The folder or file to start with
     * @return The full path of the parent folder
     * @throws Exception
     */
    public static String getParentDirectory(String directory) throws Exception {
        return getParentDirectoryRecursive(directory, 1);
    }

    /**
     * Get the executing folder
     *
     * @return Full path of the executing folder
     * @throws Exception
     */
    public static String getExecutingPath() throws Exception {    	
        return System.getProperty("catalina.base");
    }
    
    /**
     * Does the file exist?
     *
     * @param fileName Full path of the file we want to check
     * @return True if the file exists, False otherwise
     */
    public static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    public static void createFolder(String folderPath) {
        File dir = new File(folderPath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Save an 2-dimension array (table) into a file
     * <p>
     * We will join columns by ',' and rows by '\n'. CSV format is used
     *
     * @param fileName Full path of the output file
     * @param table The array we want to save
     * @throws Exception
     */
    public static void saveTable(String fileName, String[][] table) throws Exception {
        String text = "";
        for (int iRow = 0; iRow < table.length; iRow++) {
            text = text.concat(joinString(table[iRow], ",") + "\n");
        }
        saveTextFile(fileName, text);
    }

    /**
     * Load a 2-dimension array (table) stored in CSV format
     *
     * @param fileName Full path of the input file
     * @return A 2-dimension String array
     * @throws Exception
     */
    public static String[][] loadTable(String fileName) throws Exception {
        return loadTable(fileName, ",");
    }
    
    public static String[][] loadTable(String fileName, String splitter) throws Exception {
        String text = loadTextFile(fileName);
        String[][] table;
        if (text.equals("")) {
            table = new String[0][];
        } else {
            String[] lines = text.split("\n");
            table = new String[lines.length][];
            for (int iRow = 0; iRow < lines.length; iRow++) {
                String line = lines[iRow].trim();
                table[iRow] = line.split(splitter);
            }
        }

        return table;
    }

    /**
     * Save a 1-dimension array into a file
     *
     * @param fileName Full path of the output file
     * @param list The array we want to save
     * @throws Exception
     */
    public static void saveList(String fileName, String[] list) throws Exception {
        String text = joinString(list, "\n");
        saveTextFile(fileName, text);
    }

    /**
     * Load a 1-dimension array from a file
     *
     * @param fileName Full path of the input file
     * @return A 1-dimension String array
     * @throws Exception
     */
    public static String[] loadList(String fileName) throws Exception {
        return Util.loadList(fileName, "\n");
    }

    /**
     * Load a list but delimiter token is passed to the method
     *
     * @param fileName Full path of the input file
     * @param delimiter The separate token
     * @return An array of String
     * @throws Exception
     */
    public static String[] loadList(String fileName, String delimiter) throws Exception {
        String text = loadTextFile(fileName);
        return text.split(delimiter);
    }

    /**
     * Convert a String array into an Integer array
     *
     * @param stringList The input String array
     * @return an Integer array
     * @throws Exception
     */
    public static Integer[] stringList2intList(String[] stringList) throws Exception {
        Integer[] intList = new Integer[stringList.length];
        for (int i = 0; i < stringList.length; i++) {
            intList[i] = Integer.parseInt(stringList[i].trim());
        }
        return intList;
    }

    public static double[] stringArray2doubleArray(String[] strArray)
            throws Exception {
        double[] doubleArray = new double[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            doubleArray[i] = Double.parseDouble(strArray[i]);
        }

        return doubleArray;
    }
    
    public static void moveFile(String fn_src, String fn_dest) throws Exception {
        File file = new File(fn_src);
        file.renameTo(new File(fn_dest));
    }
    
    public static void copyFile(String fn_src, String fn_dest) throws Exception {
        // overwrite if fn_dest exists
        CopyOption[] options = new CopyOption[] { StandardCopyOption.REPLACE_EXISTING};
        Files.copy(new File(fn_src).toPath(), new File(fn_dest).toPath(), options);
    }
    
    public static void deleteFile(String fn) throws Exception {
        File f = new File(fn);
        if(f.isDirectory()) {
            File[] subFileList = f.listFiles();
            if(subFileList != null && subFileList.length > 0) {
                for(File subFile : subFileList) {
                    deleteFile(subFile.getPath());
                }
            }
            f.delete();
        }
        else {
            f.delete();
        }
    }
    
    /**
    * Compare binary files. Both files must be files (not directories) and exist.
    * 
    * @param first  - first file
    * @param second - second file
    * @return boolean - true if files are binery equal
    * @throws IOException - error in function
    * 
    * source: http://www.java2s.com/Code/Java/File-Input-Output/Comparebinaryfiles.htm
    */
    public static final int BUFFER_SIZE = 65536;
    public static boolean isFileBinaryEqual(
            String fn_first, String fn_second) throws IOException {
        // TODO: Test: Missing test
        boolean retval = false;
        File first = new File(fn_first);
        File second = new File(fn_second);

        if ((first.exists()) && (second.exists())
                && (first.isFile()) && (second.isFile())) {
            if (first.getCanonicalPath().equals(second.getCanonicalPath())) {
                retval = true;
            } else {
                FileInputStream firstInput = null;
                FileInputStream secondInput = null;
                BufferedInputStream bufFirstInput = null;
                BufferedInputStream bufSecondInput = null;

                try {
                    firstInput = new FileInputStream(first);
                    secondInput = new FileInputStream(second);
                    bufFirstInput = new BufferedInputStream(firstInput, BUFFER_SIZE);
                    bufSecondInput = new BufferedInputStream(secondInput, BUFFER_SIZE);

                    int firstByte;
                    int secondByte;

                    while (true) {
                        firstByte = bufFirstInput.read();
                        secondByte = bufSecondInput.read();
                        if (firstByte != secondByte) {
                            break;
                        }
                        if ((firstByte < 0) && (secondByte < 0)) {
                            retval = true;
                            break;
                        }
                    }
                } finally {
                    try {
                        if (bufFirstInput != null) {
                            bufFirstInput.close();
                        }
                    } finally {
                        if (bufSecondInput != null) {
                            bufSecondInput.close();
                        }
                    }
                }
            }
        }

		return retval;
	}

	/**
	 * Get a descriptive String about how long the time tick is
	 * 
	 * @param totalTime
	 *            number of time tick (in seconds)
	 * @return a descriptive string
	 * @throws Exception
	 */
	public static String convertTimeTick2String(long totalTime)
			throws Exception {
		long hours = totalTime / 3600;
		long minutes = (totalTime % 3600) / 60;
		long seconds = totalTime % 60;

		String runTimeStr = "Run time: ";
		if (hours > 0) {
			runTimeStr = runTimeStr.concat(String.valueOf(hours));
			if (hours > 1) {
				runTimeStr = runTimeStr.concat(" hours ");
			} else {
				runTimeStr = runTimeStr.concat(" hour ");
			}
		}
		if (minutes > 0) {
			runTimeStr = runTimeStr.concat(String.valueOf(minutes));
			if (minutes > 1) {
				runTimeStr = runTimeStr.concat(" minutes ");
			} else {
				runTimeStr = runTimeStr.concat(" minute ");
			}
		}
		if (seconds > 0) {
			runTimeStr = runTimeStr.concat(String.valueOf(seconds));
			if (seconds > 1) {
				runTimeStr = runTimeStr.concat(" seconds ");
			} else {
				runTimeStr = runTimeStr.concat(" second");
			}
		}
		if (runTimeStr.equals("Run time: ")) {
			runTimeStr = runTimeStr.concat(String.valueOf(totalTime)
					+ " milliseconds");
		}
		return runTimeStr;
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
	public static void clearFolder(String folder) throws Exception {
		if(fileExists(folder)) {
			String[] fnList = loadFileList(folder);
			for(int i = 0; i < fnList.length; i++) {
				deleteFile(getOSPath(new String[]{folder, fnList[i]}));
			}
		}
	}
	
	public static String getOSName() throws Exception {
		return System.getProperty("os.name");
	}
}
