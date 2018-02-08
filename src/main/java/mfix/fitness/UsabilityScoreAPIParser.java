package mfix.fitness;

import java.io.IOException;
import java.util.ArrayList;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UsabilityScoreAPIParser
{
	private String jsonString;
	private JsonObject json;
	
	ArrayList<Coordinate> coordinates = new ArrayList<>();
	ArrayList<ArrayList<Coordinate>> clusters = new ArrayList<>();
	
	public String getJsonString()
	{
		return jsonString;
	}

	public JsonObject getJson()
	{
		return json;
	}

	private void getAPIResults()
	{
		// copy API url and add url and API key parameters
		String httpsUrl = String.format(Constants.USABILITY_SCORE_API, MFix.getUrl(), Util.getRandomAPIKey());
		
		// invoke API command and store output in json
		try
		{
			jsonString = Util.getAPIOutputForGetRequest(httpsUrl);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
		JsonParser parser = new JsonParser();
		json = (JsonObject) parser.parse(jsonString);
	}
	
	public void processAPIResults()
	{
		long sleepStartTime = System.nanoTime();
		int sleepTimeSec = Util.getRandomIntValueInRange(30, 60);
		long sleepTime = sleepTimeSec * 1000;
		try
		{
			Thread.sleep(sleepTime);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		long sleepEndTime = System.nanoTime();
		System.out.println("Sleep time before invoking Google's PageSpeed Insights API = " + Util.convertNanosecondsToSeconds(sleepEndTime - sleepStartTime) + " sec");
		
		getAPIResults();
	}

	public double getUsabilityScore()
	{
		JsonElement jsonEl = json.get("ruleGroups");

		double score = jsonEl.getAsJsonObject().get("USABILITY").getAsJsonObject().get("score").getAsDouble();
		return score;
	}

	public double getRuleImpactScore(String problemType)
	{
		double impact = json.get("formattedResults").getAsJsonObject().get("ruleResults").getAsJsonObject().get(problemType).getAsJsonObject().get("ruleImpact").getAsDouble();
		return impact;
	}

	public int[] getContentWidth()
	{

		JsonArray vals = json.get("formattedResults").getAsJsonObject().get("ruleResults").getAsJsonObject().get(Constants.PROBLEM_TYPES_MAP.get(Constants.CONTENT_SIZE_PROBLEM)).getAsJsonObject().get("urlBlocks").getAsJsonArray().get(0).getAsJsonObject().get("header").getAsJsonObject().get("args").getAsJsonArray();
		String size1 = vals.get(0).getAsJsonObject().get("value").getAsString();
		String size2 = vals.get(1).getAsJsonObject().get("value").getAsString();
		size1 = size1.replaceAll(",", "");	// actual rendered content width
		size2 = size2.replaceAll(",", "");  // viewport width
		int[] result = { Integer.parseInt(size1), Integer.parseInt(size2) };

		return result;
	}
}
