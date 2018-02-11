package mfix.approxAlgo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.WebDriverSingleton;
import mfix.domTree.DependencyGraph;
import mfix.domTree.HtmlDomTree;
import mfix.fitness.FitnessFunction;
import mfix.fitness.GoogleAPIResults;
import mfix.segmentation.Segment;
import mfix.segmentation.SegmentModel;

import org.apache.commons.io.FileUtils;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.resources.ServiceBuilder;
import com.amazonaws.resources.ec2.EC2;
import com.amazonaws.resources.ec2.Instance;

public class Runner {
	
	private Chromosome chromosome;

	public void runIterator(String url, String filePath) {
		long startInitTime = System.nanoTime();

		init(url, filePath);
		
		// Build segment model for the original page
		WebDriverSingleton.loadPage(MFix.getFilepath());
		HtmlDomTree domTree = HtmlDomTree.getInstance(MFix.getFilepath());
		domTree.buildHtmlDomTree();
		
		// take screenshot
		WebDriverSingleton.takeScreenshot(MFix.getOutputFolderPath() + File.separatorChar + "index-before.png");
		
		SegmentModel segModel = new SegmentModel();
		segModel.buildSegmentModel();
		System.out.println("\nSegment Model: " + segModel);
		MFix.setOriginalPageSegmentModel(segModel);

		/* Stage 1: initial mobile friendly problems detection */
		System.out.println("++++++++++++++++ Stage 1: initial mobile friendly problems detection ++++++++++++++++");
		GoogleAPIResults gar = new GoogleAPIResults();
		gar.processUsabilityScoreAPIResult();
		double usabilityScore = gar.getUsabilityScoreAPIObj().getUsabilityScore();
		MFix.setBeforeUsabilityScore(usabilityScore);
		MFix.setAfterUsabilityScore(usabilityScore);
		System.out.println("Usability score API json = \n" + gar.getUsabilityScoreAPIObj().getJsonString());

		/* Stage 2: extract root causes */
		System.out.println("\n++++++++++++++++ Stage 2: extract genes ++++++++++++++++");
		Wrapper gaw = new Wrapper(gar);
		chromosome = gaw.extractGenesBasedOnIssueType();
		
		System.out.println("\n **************** SEGMENT DG MAP ************");
		System.out.println(MFix.getSegmentToDG());
		long endInitTime = System.nanoTime();
		System.out.println("Init time = " + Util.convertNanosecondsToSeconds((endInitTime - startInitTime)) + " sec");

		// Stage 3: run search
		System.out.println("\n++++++++++++++++ Stage 3: run search ++++++++++++++++");
		Search search = new Search(chromosome);

		search.runGA();
		chromosome = search.getSolutionByHeuristic();
		System.out.println("\n\nSolution chromosome based on heuristic = " + chromosome);

		MFix.setAfterUsabilityScore(chromosome.getFitnessFunctionObj().getUsabilityScore());
		if(MFix.getAfterUsabilityScore() >= Constants.USABILITY_SCORE_THRESHOLD)
		{
			MFix.setMobileFriendly(true);
		}
		postProcesing();
	}

	private void init(String url, String filepath) {
		
		FitnessFunction.setFitnessCalls(0);
		FitnessFunction.setFitnessTimeInSec(0);
		
		GoogleAPIResults.setMobileFriendlyAPITotalTimeInSec(0.0);
		GoogleAPIResults.setUsabilityScoreAPITotalTimeInSec(0.0);
		GoogleAPIResults.setMobileFriendlyAPICalls(0);
		GoogleAPIResults.setUsabilityScoreAPICalls(0);
		HtmlDomTree.resetInstance();
		MFix.setMobileFriendly(false);
		MFix.setSegmentToDG(new HashMap<String, DependencyGraph>());
		MFix.setOriginalPageSegmentModel(new SegmentModel());
		MFix.setOriginalPageSegments(new ArrayList<Segment>());
		String outputFolder = new File(filepath).getParent() + File.separatorChar + "output_" + Util.getFormattedTimestamp();
		new File(outputFolder).mkdir();
		MFix.setOutputFolderPath(outputFolder);
		Util.setElementPropValueCache(new HashMap<String, String>());

		if (Constants.RUN_IN_DEBUG_MODE) {
			try {
				System.setOut(new PrintStream(new FileOutputStream(outputFolder + File.separatorChar + "log.txt")));
			} catch (Exception e) {
			}
		}

		MFix.setUrl(url);
		MFix.setFilepath(filepath);

		// copy original file
		String copyFilePath = new File(filepath).getParent() + File.separatorChar + "index-copy.html";
		MFix.setCopiedFilepath(copyFilePath);
		try {
			FileUtils.copyFile(new File(filepath), new File(copyFilePath));
		} catch (IOException e) {
			e.printStackTrace();
		}

		// store list of available Amazon cloud instances
		List<String> instances = new ArrayList<String>();
		EC2 ec2 = ServiceBuilder.forService(EC2.class)
		        .withRegion(Region.getRegion(Regions.US_WEST_2))
		        .build();
		for(Instance instance : ec2.getInstances())
		{
			if(instance.getState().getName().equalsIgnoreCase("running"))
			{
				instances.add(instance.getPublicDnsName());
			}
		}
		System.out.println("Available instances (size = " + instances.size() + ") = {");
		for(String instance : instances)
		{
			System.out.println("\t" + instance);
		}
		System.out.println("}");
		MFix.setAwsInstances(instances);
	}

	private void postProcesing() 
	{
		Util.applyNewValues(chromosome);
		
		// take screenshot
		WebDriverSingleton.takeScreenshot(MFix.getOutputFolderPath() + File.separatorChar + "index-after.png");
		
		// create fixed test page
		String originalFile = MFix.getFilepath();
		new File(MFix.getFilepath())
				.renameTo(new File(new File(MFix.getFilepath()).getParent() + File.separatorChar + "index-fixed-" + Math.round(chromosome.getFitnessFunctionObj().getUsabilityScore()) + "-" + new File(MFix.getOutputFolderPath()).getName() + ".html"));
		new File(MFix.getCopiedFilepath()).renameTo(new File(originalFile));

		WebDriverSingleton.closeBrowser();
	}
}
