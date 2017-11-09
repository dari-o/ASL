package middleware;

public class Response {
	public static double hits = 0;
	public static double misses = 0;
	public static double numReceivedResponses = 0;
	private static long sumRespTimes = 0;
	private static double numErrors = 0;
	
	
	private StringBuffer content = null;	
	public boolean isComplete = false;
	
	public Response(StringBuffer content, Request request) {
		this.content = content;
		if (isComplete(content)) Response.numReceivedResponses +=1;
		else {
			this.content = content;
			//this.reqType = request.getType();
			setStatistics(request.getType());
			getResponseTime(request.sendingTime);
		}
	}
	
	public void setStatistics(RequestType requestType) {
		if (this.content.toString().contains("ERROR")) {
			numErrors+=1;
			return;
		}
			
		if (requestType == RequestType.GET) {
			if()
		}
	}
	
	public double getMissRatio() {
		return Response.misses/(Response.hits + Response.misses);
	}
	
	public void getResponseTime(long sentTime) {
		long respTime = sentTime - System.currentTimeMillis();
		Response.sumRespTimes += respTime;
	}
	public double getAvgServerTime() {
		return sumRespTimes/numReceivedResponses;
	}
	public boolean isComplete(StringBuffer content) {
		
		
		
	}
	public void complete(StringBuffer end) {
		this.content.append(end);
		if (isComplete)
			this.getResponseTime(sentTime);
	}
}
