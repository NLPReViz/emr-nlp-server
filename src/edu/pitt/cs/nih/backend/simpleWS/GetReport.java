/**
 * 
 */
package edu.pitt.cs.nih.backend.simpleWS;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import edu.pitt.cs.nih.backend.simpleWS.model.Report;

/**
 * @author Phuong Pham
 *
 */
@Path("/report")
public class GetReport {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;
		
	@GET
	@Path("{fn_reportIDList}")
	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})	
//	@Produces(MediaType.APPLICATION_JSON)
	public Report[] getReportFromReportIDList(
			@PathParam("fn_reportIDList") String fn_reportIDList) throws Exception {
		List<Report> reportList = ReportDAO.instance.getReportFromListFile(
				fn_reportIDList, null); 
		return reportList.toArray(new Report[reportList.size()]);
	}
	
	@GET
	@Path("{fn_reportIDList}/{fn_modelFnList}")
//	@Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
	@Produces(MediaType.APPLICATION_JSON)
	public Report[] getReportFromList(
			@PathParam("fn_reportIDList") String fn_reportIDList,
			@PathParam("fn_modelFnList") String fn_modelFnList) throws Exception {
		List<Report> reportList = ReportDAO.instance.getReportFromListFile(
				fn_reportIDList, fn_modelFnList);
//		for(Report report : reportList) {
//			System.out.println(report.getPredConfidence());
//		}
		return reportList.toArray(new Report[reportList.size()]);
	}
}
