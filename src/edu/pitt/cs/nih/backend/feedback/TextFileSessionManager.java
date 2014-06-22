/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.pitt.cs.nih.backend.feedback;

import edu.pitt.cs.nih.backend.utils.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage sessions (batch) of feedback. Session management allows users can roll backward, 
 * roll forward their updated models easily.
 * <p>
 * The session meta file will contains all sessions, each line has the following format
 * <p>
 * sessionID, userID, varID, active/inactive, valid/delete
 * <p>
 * The tuple (sessionID, userID, varID) will be the primary key for this file. Each sessionID 
 * is related to one userID only
 * 
 * @author phuongpham <a href="mailto:phuongpham@cs.pitt.edu">phuongpham@cs.pitt.edu</a>
 */
public class TextFileSessionManager {
    String fn_sessionMeta;

    /**
     * Initialize with specific meta file location
     * 
     * @param sessionMetaFilePath Full path of the session meta file (includes file name)
     */
    public TextFileSessionManager(String sessionMetaFilePath) {
        try {
            fn_sessionMeta = sessionMetaFilePath;
            if (!Util.fileExists(fn_sessionMeta)) {
                Util.saveTextFile(fn_sessionMeta, "");
            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Get the previous no (necessary to be active) sessionID of <code>sessionID</code> from user <code>userID</code> 
     * and use the same <code>varID</code>.
     * <p>
     * <code>userID</code> equals to empty string is considered as a baseline, the starting 
     * session that all users give feedback upon.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @return Empty string if the <code>sessionID</code> does not exist or cannot 
     * find the previous sessionID of <code>sessionID</code>
     */
    public String getPreviousSessionID(String sessionID, String userID, String varID) {
        String prevSessionID = "";
        String[][] metaTable = getMetaTable();
        int sessionIDNum = Integer.parseInt(sessionID);
        // go from the end to the beginning
        int i = metaTable.length - 1;
        String[] metaRow;        
        // looking for the sessionID
        while(i >= 0) {
            metaRow = metaTable[i--];
            if(sessionID.equals(metaRow[0])) {
                break;
            }
        }
        
        // looking backward for sessions have the same ID with sessionID and from the same 
        // user
        int sessionIDRow;
        while(i >= 0) {
            metaRow = metaTable[i--];
            sessionIDRow = Integer.parseInt(metaRow[0]);
            if(sessionIDRow < sessionIDNum && metaRow[2].equals(varID) && 
                    metaRow[4].equals("valid") &&
                    (metaRow[1].equals(userID) || metaRow[1].equals(""))) {
                prevSessionID = Integer.toString(sessionIDRow);
                break;
            }
        }
        
        return prevSessionID;
    }
    
    /**
     * Get the next sessionID of <code>sessionID</code> from user <code>userID</code> 
     * and use the same <code>varID</code>.
     * <code>userID</code> equals to empty string is considered as a baseline, the starting 
     * session that all users give feedback upon.
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @return Empty string if the <code>sessionID</code> does not exist or cannot 
     * find the next sessionID of <code>sessionID</code>
     */
    public String getNextSessionID(String sessionID, String userID, String varID) {
        String nextSessionID = "";
        String[][] metaTable = getMetaTable();
        int sessionIDNum = Integer.parseInt(sessionID);
        // go from the end to the beginning
        int i = metaTable.length - 1;
        String[] metaRow;        
        // looking for the sessionID
        while(i >= 0) {
            metaRow = metaTable[i--];
            if(sessionID.equals(metaRow[0])) {
                break;
            }
        }
        
        // looking forward for sessions have the same ID with sessionID and from the same 
        // user
        int sessionIDRow;
        i += 2;
        while(i < metaTable.length) {
            metaRow = metaTable[i++];
            sessionIDRow = Integer.parseInt(metaRow[0]);
            if(sessionIDRow > sessionIDNum && metaRow[2].equals(varID) && 
                    metaRow[4].equals("valid") &&
                    (metaRow[1].equals(userID)  || metaRow[1].equals(""))) {
                nextSessionID = Integer.toString(sessionIDRow);
                break;
            }
        }
        
        return nextSessionID;
    }
    
    /**
     * Get the current (active) sessionID of <code>sessionID</code> from user <code>userID</code> 
     * and use the same <code>varID</code>.
     * <code>userID</code> equals to empty string is considered as a baseline, the starting 
     * session that all users give feedback upon.
     * 
     * @param userID
     * @param varID
     * @return Empty string if the <code>userID</code> does not exist or cannot 
     * find the active sessionID of <code>userID, varID</code>
     */
    public String getCurrentSessionID(String userID, String varID) {
        String curSessionID = "";
        String[][] metaTable = getMetaTable();
        // go from the end to the beginning
        int i = metaTable.length - 1;
        String[] metaRow;        
        // looking for the sessionID
        while(i >= 0) {
            metaRow = metaTable[i--];
            if((metaRow[1].equals(userID) || metaRow[1].equals("")) && 
                    metaRow[4].equals("valid") &&
                    metaRow[2].equals(varID) && metaRow[3].equals("active")) {
                curSessionID = metaRow[0];
                break;
            }
        }
        
        return curSessionID;
    }
    
    /**
     * Get all current session states, one for each variable.
     * 
     * @param userID
     * @return 
     */
    public String[][] getCurrentState(String userID) {
        String[][] metaTable = getMetaTable();
        ArrayList<String[]> currentStateList = new ArrayList<>();
        
        for(int i = 0; i < metaTable.length; i++) {
            if((metaTable[i][1].equals(userID) || metaTable[i][1].equals("")) &&
                    metaTable[i][3].equals("active") && metaTable[i][4].equals("valid")) {
                // check if currentStateList has this var or not
                // because session from initial DS will be added first
                for(String[] sessionRow : currentStateList) {
                    if(sessionRow[2].equals(metaTable[i][2])) {
                        // remove the current (possibly the initial session
                        currentStateList.remove(sessionRow);
                        break;
                    }
                }
                currentStateList.add(metaTable[i]);
            }
        }
        
        return currentStateList.toArray(new String[currentStateList.size()][]);
    }
    
    /**
     * The the maximum session ID + 1 to create new batch.
     * Because our session meta file is append only, the last row will contain 
     * the maximum sessionID.
     * 
     * @return "0" if the meta file is empty
     */
    public String getNewSessionID() {
        String newSessionID = "0";
        String[][] metaTable = getMetaTable();
        if(metaTable.length > 0) {
            int sessionID = Integer.parseInt(metaTable[metaTable.length - 1][0]);
            newSessionID = Integer.toString(sessionID + 1);
        }
        
        return newSessionID;
    }
    
    /**
     * Roll the active state of a varID annotated by a user to a previous state. 
     * The roll back (if any) will be recored into the meta file.
     * <p>
     * We do not allow to roll backward variables annotated by the initialize user (userID = "").
     * <p>
     * Only de-active the current sessionID if there is a previous sessionID.
     * 
     * @param userID
     * @param varID 
     */
    public void rollBackward(String userID, String varID) {        
        String curSessionID = getCurrentSessionID(userID, varID);
        if(! curSessionID.equals("")) {            
            String prevSessionID = getPreviousSessionID(curSessionID, userID, varID);
            if(! prevSessionID.equals(""))
            {
                String[][] metaTable = getMetaTable();        
                // de-active the current session
                setStatus(curSessionID, userID, varID, "inactive", metaTable);
                // active the previous session
                setStatus(prevSessionID, userID, varID, "active", metaTable);
                try {
                    Util.saveTable(fn_sessionMeta, metaTable);
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
    
    /**
     * Roll back all varID of the latest session for a userID
     *
     * @param userID
     */
    public void rollBackward(String userID) {
        String[][] currentSessionState = getCurrentState(userID);
        // get the max session
        int maxSession = -1;
        for (String[] sessionByVariable : currentSessionState) {
            int varSession = Integer.parseInt(sessionByVariable[0]);
            if (varSession > maxSession) {
                maxSession = varSession;
            }
        }

        // roll back only variables belong to the latest session
        String latestSession = Integer.toString(maxSession);
        for (String[] sessionByVariable : currentSessionState) {
            if (sessionByVariable[0].equals(latestSession)) {
                rollBackward(userID, sessionByVariable[2]);
            }
        }
    }
    
    /**
     * Roll the active state of a varID annotated by a user to a next state.
     * The roll back (if any) will be recored into the meta file.
     * <p>
     * We do not allow to roll forward variables annotated by the initialize user (userID = "").
     * <p>
     * Only de-active the current sessionID if there is a next sessionID.
     * 
     * @param userID
     * @param varID 
     */
    public void rollForward(String userID, String varID) {        
        String curSessionID = getCurrentSessionID(userID, varID);
        if(! curSessionID.equals("")) {            
            String nextSessionID = getNextSessionID(curSessionID, userID, varID);
            if(! nextSessionID.equals(""))
            {
                String[][] metaTable = getMetaTable();
                // de-active the current session, but do not de-active initial session
                // other users may use it. Blocked in the setStatus function
                setStatus(curSessionID, userID, varID, "inactive", metaTable);
                // active the previous session
                setStatus(nextSessionID, userID, varID, "active", metaTable);
                try {
                    Util.saveTable(fn_sessionMeta, metaTable);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
    
    /**
     * Roll forward to the very next session of the latest active session
     *
     * @param userID
     */
    public void rollForward(String userID) {
        String[][] currentSessionState = getCurrentState(userID);
        // get the max + active session
        int maxSession = -1;
        for (String[] sessionByVariable : currentSessionState) {
            int varSession = Integer.parseInt(sessionByVariable[0]);
            if (varSession > maxSession) {
                maxSession = varSession;
            }
        }

        // roll backwark until we reach the maxSession
        String latestSession = Integer.toString(maxSession);
        int nextSessionLine = -1;
        String[][] metaTable = getMetaTable();
        int i = metaTable.length - 1;
        while (i >= 0) {
//            if (metaTable[i][0].equals(latestSession) && nextSessionLine < i) {
            if (metaTable[i][0].equals(latestSession)) {
                nextSessionLine = i + 1;
                break;
            }
            --i;
        }
        // roll forward until we reach the very next session
        i = nextSessionLine;
        int nextSession = -1;
        while(i < metaTable.length) {
            if((metaTable[i][1].equals(userID) || metaTable[i][1].equals("")) &&
                    maxSession < Integer.parseInt(metaTable[i][0]) &&
                    metaTable[i][4].equals("valid")) {
                nextSession = Integer.parseInt(metaTable[i][0]);
                break;
            }
            i++;
        }
        
        // active all vars at the nextSession
        if (nextSession > -1) {
            while (i < metaTable.length) {
                if ((metaTable[i][1].equals(userID) || metaTable[i][1].equals(""))
                        && nextSession == Integer.parseInt(metaTable[i][0]) &&
                        metaTable[i][4].equals("valid")) {
                    rollForward(userID, metaTable[i][2]);
                }
                if(++i < metaTable.length && Integer.parseInt(metaTable[i][0]) > nextSession) {
                    break;
                }
            }
        }
    }
    
    /**
     * Add new session for a user on a variable. Adding includes active the line and 
     * de-active the previous session for the user on the variable.
     * 
     * @param sessionID
     * @param userID
     * @param varID 
     */
    public void addSessionLine(String sessionID, String userID, String varID) {   
        // de-active all previous sessions having the varID active because the latest 
        // session may not have the variable or the system has been rolled back to 
        // several steps before the latest session.
        String prevSessionID = getCurrentSessionID(userID, varID);
        if (!prevSessionID.equals("")) {
            String[][] metaTable = getMetaTable();
            setStatus(prevSessionID, userID, varID, "inactive", metaTable);
            try {
                Util.saveTable(fn_sessionMeta, metaTable);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        // append the session meta file
        try {
            String metaText = Util.loadTextFile(fn_sessionMeta);
            Util.saveTextFile(fn_sessionMeta, metaText + sessionID + "," + userID + "," + varID + ",active,valid\n");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * Mark a session line as "delete". 
     * When a user submits a feedback batch but then rejects it after reviewing changes 
     * in the model.
     * 
     * @param sessionID
     * @param userID
     * @param varID 
     */
    public void deleteSessionLine(String sessionID, String userID, String varID) {
        String[][] metaTable = getMetaTable();
        int rowIndex = getRowIndex(sessionID, userID, varID, metaTable);
        if(rowIndex > -1) {
            if(metaTable[rowIndex][3].equals("active")) {
                // roll back one step if the deleted line is currently active
                rollBackward(userID, varID);
                // else we delete an inactive line, which will not affect the current active session
            }
            metaTable[rowIndex][4] = "delete";
            try
            {
                Util.saveTable(fn_sessionMeta, metaTable);
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
    
    /**
     * Delete all variables belonging to <code>sessionID</code>
     * 
     * @param sessionID
     * @param userID
     */
    public void deleteSession(String sessionID, String userID) {
        String[][] metaTable = getMetaTable();
        String varID;
        
        for(int i = metaTable.length - 1; i >= 0; i--) {
        	if(metaTable[i][0].equals(sessionID)) {
        		varID = metaTable[i][2];
        		deleteSessionLine(sessionID, userID, varID);
        	}
        }
    }
    
    /**
     * Load the meta table from file.
     * <p>
     * @return An empty table if the file is empty
     */
    protected String[][] getMetaTable() {
        String[][] metaTable = null;
        try {
            metaTable = Util.loadTable(fn_sessionMeta);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        return metaTable;
    }
    
    /**
     * Set status, i.e. active / inactive, for a session annotated by a user at a variable
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @param status
     * @param metaTable 
     */
    protected void setStatus(String sessionID, String userID, String varID, 
            String status, String[][] metaTable) {
        int rowIndex = getRowIndex(sessionID, userID, varID, metaTable);
        // do not de-active session belong to initial user
        if( rowIndex != -1 && ! (metaTable[rowIndex][1].equals("") && status.equals("inactive")))
            metaTable[rowIndex][3] = status;
    }
    
    /**
     * Get row index of a tuple <code>sessionID, userID, varID</code>
     * 
     * @param sessionID
     * @param userID
     * @param varID
     * @param metaTable
     * @return -1 if the tuple does not exist in the <code>metaTable</code>
     */
    protected int getRowIndex(String sessionID, String userID, String varID,
            String[][] metaTable) {
        int index = -1;
        
        for(int i = metaTable.length - 1; i >= 0; i--) {
            if(metaTable[i][0].equals(sessionID) &&
                    (metaTable[i][1].equals(userID) || metaTable[i][1].equals("")) &&
                    metaTable[i][4].equals("valid") && 
                    metaTable[i][2].equals(varID)) {
                index = i;
                break;
            }
        }
        
        return index;
    }
    
    /**
     * Delete all related file of the latest session of <code>userID</code>
     * 
     * @param userID
     * @throws Exception
     */
    public void deleteCurrentSessionID(String userID) throws Exception {
    	String curSessionID = getCurrentSessionID(userID);
    	// update the session manager file
    	deleteSession(curSessionID, userID);
    }
    
    /**
     * Get the latest (valid) session of <code>userID</code>
     * 
     * @param userID
     * @return
     * @throws Exception
     */
    protected String getCurrentSessionID(String userID) throws Exception {
    	String curSessionID = "";
        String[][] metaTable = getMetaTable();
        // go from the end to the beginning
        int i = metaTable.length - 1;
        String[] metaRow;        
        // looking for the sessionID
        while(i >= 0) {
            metaRow = metaTable[i--];
            if((metaRow[1].equals(userID) || metaRow[1].equals("")) && 
                    metaRow[4].equals("valid") &&
                    metaRow[3].equals("active")) {
                curSessionID = metaRow[0];
                break;
            }
        }
        
        return curSessionID;
    }
    
    public static List<String> getDeletedSessionIDList(String userID, String fn_sessionTable) throws Exception {
    	String[][] sessionTable = Util.loadTable(fn_sessionTable);
    	List<String> sessionIDList = new ArrayList<>(sessionTable.length);
    	for(int i = 0; i < sessionTable.length; i++) {
    		if(sessionTable[i][1].equals(userID) &&
    				sessionTable[i][4].equals("delete")) {
    			sessionIDList.add(sessionTable[i][0]);
    		}
    	}
    	return sessionIDList;
    }
}
