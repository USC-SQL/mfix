package eval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mfix.MFix;
import mfix.Util;
import mfix.approxAlgo.Runner;
import mfix.fitness.FitnessFunction;
import mfix.fitness.GoogleAPIResults;

import org.xml.sax.SAXException;

public class TestMFix
{
	public void runApproach(String[] args) throws SAXException, IOException
	{
		// args[0] -> test url
		// args[1] -> original test page disk path
		
		// run search
		long startTime = System.nanoTime();
		Runner mi = new Runner();
		mi.runIterator(args[0], args[1]);
		long endTime = System.nanoTime();

		System.out.println("\n\n--------------------- FINAL RESULTS ---------------------------------");
		System.out.println("Is page mobile friendly? = " + MFix.isMobileFriendly());
		System.out.println("# before usability score = " + MFix.getBeforeUsabilityScore());
		System.out.println("# after usability score = " + MFix.getAfterUsabilityScore());
		System.out.println("Improvement in usability score = " + Math.round(((double)(MFix.getAfterUsabilityScore() - MFix.getBeforeUsabilityScore()) / (double)MFix.getBeforeUsabilityScore()) * 100.0) + "%");
		System.out.println("Total time = " + Util.convertNanosecondsToSeconds(endTime - startTime) + " sec");
		System.out.println("No. of fitness calls = " + FitnessFunction.getFitnessCalls());
		System.out.println("Avg. time for fitness call = " + (FitnessFunction.getFitnessTimeInSec() / (double) FitnessFunction.getFitnessCalls()) + " sec");
		System.out.println("No. of mobile friendly API calls = " + GoogleAPIResults.getMobileFriendlyAPICalls());
		System.out.println("Avg. time for mobile friendly API call = " + (GoogleAPIResults.getMobileFriendlyAPITotalTimeInSec() / (double) GoogleAPIResults.getMobileFriendlyAPICalls()) + " sec");
		System.out.println("No. of usability score API calls = " + GoogleAPIResults.getUsabilityScoreAPICalls());
		System.out.println("Avg. time for usability score API call = " + (GoogleAPIResults.getUsabilityScoreAPITotalTimeInSec() / (double) GoogleAPIResults.getUsabilityScoreAPICalls()) + " sec");
		System.out.println("-------------------------------------------------------------------------------");
	}

	public static void main(String[] args) throws SAXException, IOException
	{ 
		List<String> subjectsList = new ArrayList<>();
		subjectsList.add("aamc");
		subjectsList.add("arxiv");
		subjectsList.add("battle");
		subjectsList.add("bitcointalk");
		subjectsList.add("blizzard");
		subjectsList.add("boardgamegeek");
		subjectsList.add("bulbagarden");
		subjectsList.add("coinmarketcap");
		subjectsList.add("correios");
		subjectsList.add("dictcc");
		subjectsList.add("discogs");
		subjectsList.add("drudgereport");
		subjectsList.add("finalfantasyxiv");
		subjectsList.add("flashscore");
		subjectsList.add("fragrantica");
		subjectsList.add("gsmhosting");
		subjectsList.add("intellicast");
		subjectsList.add("irctc");
		subjectsList.add("irs");
		subjectsList.add("leo");
		subjectsList.add("letour");
		subjectsList.add("lolcounter");
		subjectsList.add("mmochampion");
		subjectsList.add("myway");
		subjectsList.add("ncbi");
		subjectsList.add("nexusmods");
		subjectsList.add("nvidia");
		subjectsList.add("rotoworld");
		subjectsList.add("sigmaaldrich");
		subjectsList.add("soccerway");
		subjectsList.add("squareenix");
		subjectsList.add("travel");
		subjectsList.add("weather");
		subjectsList.add("weatherau");
		subjectsList.add("wiley");
		subjectsList.add("wileyonlinelibrary");
		subjectsList.add("wowprogress");
		subjectsList.add("xkcd");
		
		
		int NUMBER_OF_RUNS = 1;
		
		for(int i = 0; i < NUMBER_OF_RUNS; i++)
		{
			for(String subject : subjectsList)
			{
				System.err.println("\n" + subject);
				String url = "<server URL>" + subject + "/";
				String filepath = "<tomcat file location>" + subject + "/index.html";
				
				String[] testXbiArgs = {url, filepath};
				TestMFix tm = new TestMFix();
				tm.runApproach(testXbiArgs);
			}
		}
	}
}
