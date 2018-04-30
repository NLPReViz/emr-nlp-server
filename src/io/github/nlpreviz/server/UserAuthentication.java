package io.github.nlpreviz.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserAuthentication {
	
	private static String convertByteToHex(byte[] byteData) {

	    StringBuilder sb = new StringBuilder();
	    for (int i = 0; i < byteData.length; i++) {
	        sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
	    }

	    return sb.toString();
	}
	
	public static String getHash(final String inputString) {

	    MessageDigest md;
	    
		try {
			md = MessageDigest.getInstance("SHA");
			md.update(inputString.getBytes());

		    byte[] digest = md.digest();
		    
		    return convertByteToHex(digest);

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
    public static String authenticate(String user, String password) {
    	String uid = null; 
    	
//    	SHA for "password" is 5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8
    	
    	// if (user.equals("username") && getHash(password).equals("5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8"))
    	// 	uid = "1";
        
    	return "1";
    }
}