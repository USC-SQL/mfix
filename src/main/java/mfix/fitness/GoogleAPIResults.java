package mfix.fitness;

import mfix.Util;

public class GoogleAPIResults
{
	private MobileFriendlyAPIParser mobileFriendlyAPIObj;
	private UsabilityScoreAPIParser usabilityScoreAPIObj;
	
	private static double mobileFriendlyAPITotalTimeInSec;
	private static double usabilityScoreAPITotalTimeInSec;
	private static int mobileFriendlyAPICalls;
	private static int usabilityScoreAPICalls;
	
	public static double getMobileFriendlyAPITotalTimeInSec()
	{
		return mobileFriendlyAPITotalTimeInSec;
	}
	public static void setMobileFriendlyAPITotalTimeInSec(double mobileFriendlyAPITotalTimeInSec)
	{
		GoogleAPIResults.mobileFriendlyAPITotalTimeInSec = mobileFriendlyAPITotalTimeInSec;
	}
	public static double getUsabilityScoreAPITotalTimeInSec()
	{
		return usabilityScoreAPITotalTimeInSec;
	}
	public static void setUsabilityScoreAPITotalTimeInSec(double usabilityScoreAPITotalTimeInSec)
	{
		GoogleAPIResults.usabilityScoreAPITotalTimeInSec = usabilityScoreAPITotalTimeInSec;
	}
	public static int getMobileFriendlyAPICalls()
	{
		return mobileFriendlyAPICalls;
	}
	public static void setMobileFriendlyAPICalls(int mobileFriendlyAPICalls)
	{
		GoogleAPIResults.mobileFriendlyAPICalls = mobileFriendlyAPICalls;
	}
	public static int getUsabilityScoreAPICalls()
	{
		return usabilityScoreAPICalls;
	}
	public static void setUsabilityScoreAPICalls(int usabilityScoreAPICalls)
	{
		GoogleAPIResults.usabilityScoreAPICalls = usabilityScoreAPICalls;
	}
	public MobileFriendlyAPIParser getMobileFriendlyAPIObj()
	{
		return mobileFriendlyAPIObj;
	}
	public UsabilityScoreAPIParser getUsabilityScoreAPIObj()
	{
		return usabilityScoreAPIObj;
	}

	public void processUsabilityScoreAPIResult()
	{
		System.out.println("Usability Score API call");
		long startTime = System.nanoTime();
		usabilityScoreAPIObj = new UsabilityScoreAPIParser();
		usabilityScoreAPIObj.processAPIResults();
		System.out.println("Usability Score = " + usabilityScoreAPIObj.getUsabilityScore());
		long endTime = System.nanoTime();
		usabilityScoreAPICalls++;
		usabilityScoreAPITotalTimeInSec = usabilityScoreAPITotalTimeInSec + Util.convertNanosecondsToSeconds((endTime - startTime));
	}
	
	public void processMobileFriendlyAPIResult()
	{
		int count = 0;
		do
		{
			System.out.println("Mobile friendly API call " + (count + 1));
			long startTime = System.nanoTime();
			mobileFriendlyAPIObj = new MobileFriendlyAPIParser();
			mobileFriendlyAPIObj.processAPIResults();
			long endTime = System.nanoTime();
			mobileFriendlyAPICalls++;
			mobileFriendlyAPITotalTimeInSec = mobileFriendlyAPITotalTimeInSec + Util.convertNanosecondsToSeconds((endTime - startTime));
			count++;
			
			if(!mobileFriendlyAPIObj.isStatusComplete())
			{
				try
				{
					Thread.sleep(100000);	// quota: 1 request per 100 seconds for one user
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			
		} while(!mobileFriendlyAPIObj.isStatusComplete() && count < 3);
		System.out.println("Mobile friendly API json = \n" + mobileFriendlyAPIObj.getJsonString());
	}
}
