package middleware;

public class Response {
	// static global variables for statistic purposes
	private static double hits;
	private static double misses;
	private static double numReceivedResponses;
	private static long sumRespTimes;
	private static double numErrors;
	
	//instance variables
	private long sentTime = -1;
	private StringBuffer content = null;	
	public boolean isComplete = false;
	
	
	public Response(StringBuffer content, Request request) {
		this.content = content;
		if (isComplete(request.getType(), content)) {
			Response.numReceivedResponses +=1;
			setStatistics(request.getType());
			getResponseTime(request.sendingTime);
			
		}
		else {
			this.content = content;
			this.sentTime = request.sendingTime;
			//this.reqType = request.getType();
			
		}
	}
	
	public void setStatistics(RequestType requestType) {
		if (this.content.toString().contains("ERROR")) {
			numErrors+=1;
			return;
		}
			
		if (requestType == RequestType.GET) {
			if(content.toString().equals("END\r\n")) {
				misses+=1;
			}else {
				hits+=1;
			}
		}
	}
	
	
	
	public void getResponseTime(long sentTime) {
		long respTime = sentTime - System.currentTimeMillis();
		Response.sumRespTimes += respTime;
	}
	
	public boolean isComplete(RequestType requestType, StringBuffer content) {
		if(requestType==RequestType.SET)
			return this.content.toString().equals("STORED\r\n");
		return content.toString().contains("END\r\n"); 
		
	}
	public void complete(StringBuffer end) {
		this.content.append(end);
		if (isComplete)
			this.getResponseTime(this.sentTime);
	}
	
	public double getNumErrorMessages() {
		return numErrors;
	}
	
	// Measurements methods
	
	public static void initMeasurements() {
		Response.hits = 0;
		Response.misses = 0;
		Response.numReceivedResponses = 0;
		Response.sumRespTimes = 0;
		Response.numErrors = 0;
	}
	public double getAvgServerTime() {
		return sumRespTimes/numReceivedResponses;
	}
	
	public double getMissRatio() {
		return Response.misses/(Response.hits + Response.misses);
	}
}
