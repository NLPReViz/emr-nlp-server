/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.utils;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wrapper call command line from Java code. Get from 
 * @author Phuong Pham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class RunCmdLine {

//    public static void runCommand(String cmdArgs, int verbolity) throws Exception {
//        Runtime rt = Runtime.getRuntime();
//
//        Process pr = rt.exec(cmdArgs);
//
//        BufferedReader input = new BufferedReader(new InputStreamReader(pr.getInputStream()));
//
//        String line = null;
//
//        while ((line = input.readLine()) != null) {
//            System.out.println(line);
//        }
//
//        int exitVal = pr.waitFor();
//        if(verbolity > 0)
//            System.out.println("Exited with error code " + exitVal);
//        
//        // close input stream
//        input.close();
//        // kill the process
//        pr.destroy();
//    }
    public static void runCommand(String cmdArgs, int verbolity) throws Exception {
        Runtime rt = Runtime.getRuntime();

        Process pr = rt.exec(cmdArgs);


        // get the error stream of the process and print it
        InputStream error = pr.getErrorStream();
        for (int i = 0; i < error.available(); i++) {
            System.out.println("" + error.read());
        }

        // get the output stream
        OutputStream out = pr.getOutputStream();
        
        // close the output stream
        out.close();

        // get the input stream of the process and print it
        InputStream in = pr.getInputStream();
        for (int i = 0; i < in.available(); i++) {
            System.out.println("" + in.read());
        }

        int exitVal = pr.waitFor();
        if (verbolity > 0) {
            System.out.println("Exited with error code " + exitVal);
        }

        // kill the process
        pr.destroy();
    }
}