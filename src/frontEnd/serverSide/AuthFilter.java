package frontEnd.serverSide;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.DatatypeConverter;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class AuthFilter implements ContainerRequestFilter {
	@Override
    public ContainerRequest filter(ContainerRequest containerRequest) throws WebApplicationException {

//        String method = containerRequest.getMethod();
//        String path = containerRequest.getPath(true);
 
        // String auth = containerRequest.getHeaderValue("authorization");
        // if(auth == null){
        //     throw new WebApplicationException(Status.UNAUTHORIZED);
        // }
 
        // String[] credentials = decode(auth);
 
        // if(credentials == null || credentials.length != 2){
        //     throw new WebApplicationException(Status.UNAUTHORIZED);
        // }
 
        String uid = "1";
 
        if(uid == null){
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
 
        containerRequest.getRequestHeaders().add("uid", uid);
        
        return containerRequest;
    }
    
    public static String[] decode(String auth) {
        //Replacing "Basic THE_BASE_64" to "THE_BASE_64" directly
        auth = auth.replaceFirst("[B|b]asic ", "");
 
        //Decode the Base64 into byte[]
        byte[] decodedBytes = DatatypeConverter.parseBase64Binary(auth);
 
        //If the decode fails in any case
        if(decodedBytes == null || decodedBytes.length == 0){
            return null;
        }
 
        //Now split the byte[] into an array :
        //  - the first one is login,
        //  - the second one password
        return new String(decodedBytes).split(":", 2);
    }
}
