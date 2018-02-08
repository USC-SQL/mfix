package mfix.fitness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MobileFriendlyAPIParser
{
	private String jsonString;
	private JsonObject json;
	
	private boolean isStatusComplete;
	
	public String getJsonString()
	{
		return jsonString;
	}

	public JsonObject getJson()
	{
		return json;
	}

	public boolean isStatusComplete()
	{
		return isStatusComplete;
	}

	public void setStatusComplete(boolean isStatusComplete)
	{
		this.isStatusComplete = isStatusComplete;
	}

	private void getAPIResults()
	{
		// copy API url and add url and API key parameters
		String httpsUrl = String.format(Constants.MOBILE_FRIENDLY_API, Util.getRandomAPIKey());
		
		// invoke API command and store output in json
		try
		{
			jsonString = Util.getAPIOutputForPostRequest(httpsUrl, MFix.getUrl());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//jsonString = "{  \"testStatus\": {    \"status\": \"COMPLETE\"  },  \"mobileFriendliness\": \"NOT_MOBILE_FRIENDLY\",  \"mobileFriendlyIssues\": [    {      \"rule\": \"TAP_TARGETS_TOO_CLOSE\"    },    {      \"rule\": \"CONFIGURE_VIEWPORT\"    },    {      \"rule\": \"USE_LEGIBLE_FONT_SIZES\"    }  ]}";
		
		JsonParser parser = new JsonParser();
		json = (JsonObject) parser.parse(jsonString);
	}
	
	public boolean isMobileFriendly()
	{
		if(json.get("mobileFriendliness").getAsString().equals("MOBILE_FRIENDLY"))
			return true;
		return false;
	}
	
	public List<String> getMobileFriendlyIssues()
	{
		List<String> mobileFriendlyIssues = new ArrayList<>();
		JsonArray arr = json.get("mobileFriendlyIssues").getAsJsonArray();
		for (int i = 0; i < arr.size(); i++)
		{
			JsonObject obj = arr.get(i).getAsJsonObject();
			mobileFriendlyIssues.add(obj.get("rule").getAsString());
		}
		return mobileFriendlyIssues;
	}
	
	public void parseStatus()
	{
		if(json.get("testStatus").getAsJsonObject() == null)
		{
			isStatusComplete = false;
			return;
		}
		
		String status = json.get("testStatus").getAsJsonObject().get("status").getAsString();
		System.out.println("Mobile friendly API call status = " + status);
		if(status.equalsIgnoreCase("COMPLETE"))
		{
			isStatusComplete = true;
		}
		else
		{
			isStatusComplete = false;
		}
	}
	
	public void processAPIResults()
	{
		getAPIResults();
		parseStatus();
	}
}
