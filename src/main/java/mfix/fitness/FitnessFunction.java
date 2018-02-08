package mfix.fitness;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.approxAlgo.Chromosome;
import mfix.segmentation.EdgeLabel;
import mfix.segmentation.InterSegmentEdge;
import mfix.segmentation.IntraSegmentEdge;
import mfix.segmentation.Segment;
import mfix.segmentation.SegmentModel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FitnessFunction {
	private transient static int fitnessCalls;
	private static double fitnessTimeInSec;
	
	private double usabilityScore;
	private double aestheticScore;
	private double fitnessScore;

	private String aestheticScoreBreakdown;
	private String fitnessScoreBreakdown;
	
	public FitnessFunction() 
	{
		aestheticScoreBreakdown = "";
		fitnessScoreBreakdown = "";
	}
	
	public static void setFitnessCalls(int fitnessCalls) {
		FitnessFunction.fitnessCalls = fitnessCalls;
	}

	public static void setFitnessTimeInSec(double fitnessTimeInSec) {
		FitnessFunction.fitnessTimeInSec = fitnessTimeInSec;
	}

	public static int getFitnessCalls() {
		return fitnessCalls;
	}

	public static double getFitnessTimeInSec() {
		return fitnessTimeInSec;
	}
	
	public double getFitnessScore()
	{
		return fitnessScore;
	}

	public void setFitnessScore(double fitnessScore)
	{
		this.fitnessScore = fitnessScore;
	}

	public void setUsabilityScore(double usabilityScore)
	{
		this.usabilityScore = usabilityScore;
	}

	public void setAestheticScore(double aestheticScore)
	{
		this.aestheticScore = aestheticScore;
	}
	
	public double getUsabilityScore()
	{
		return usabilityScore;
	}

	public double getAestheticScore()
	{
		return aestheticScore;
	}

	public String getAestheticScoreBreakdown()
	{
		return aestheticScoreBreakdown;
	}

	public void setAestheticScoreBreakdown(String aestheticScoreBreakdown)
	{
		this.aestheticScoreBreakdown = aestheticScoreBreakdown;
	}

	public String getFitnessScoreBreakdown()
	{
		return fitnessScoreBreakdown;
	}

	public void setFitnessScoreBreakdown(String fitnessScoreBreakdown)
	{
		this.fitnessScoreBreakdown = fitnessScoreBreakdown;
	}

	public List<Chromosome> calculateFitnessScoreForPopulation(List<Chromosome> population, String searchStep)
	{
		// directory to store chromsome jsons to be sent to the cloud instances
		String jsonOutDirPath = MFix.getOutputFolderPath() + File.separatorChar + "aws_out";
		if(!new File(jsonOutDirPath).exists())
		{
			new File(jsonOutDirPath).mkdir();
		}
		
		// directory to store jsons coming from cloud instances
		String jsonInDirPath = MFix.getOutputFolderPath() + File.separatorChar + "aws_in";
		if(!new File(jsonInDirPath).exists())
		{
			new File(jsonInDirPath).mkdir();
		}

		GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
		Gson gson = builder.create();
		
		long startTimeOnCloud = System.nanoTime(); 
		ExecutorService executor = Executors.newFixedThreadPool(MFix.getAwsInstances().size());
		int chromosomeCount = 0;
		for(Chromosome chromosome : population)
		{
			// prepare chromosome to be sent to the cloud by creating its json
			String chromosomeIdentifier = "chromosome_" + searchStep + "_" + chromosomeCount;
			chromosome.setChromosomeIdentifier(chromosomeIdentifier);
			String chromsomeJsonFilepath = jsonOutDirPath + File.separatorChar + chromosomeIdentifier + ".json";
			try (FileWriter writer = new FileWriter(chromsomeJsonFilepath))
			{
				gson.toJson(chromosome, writer);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			// run the gene on the cloud instance
			String instance = Util.getInstance(chromosomeCount);
			System.out.println("Sending " + chromosomeIdentifier + " to " + instance);
			Runnable worker = new FitnessFunctionCloudRunnable(instance, chromsomeJsonFilepath, jsonInDirPath);
			executor.execute(worker);
			
			chromosomeCount++;
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
 
		}
		long endTimeOnCloud = System.nanoTime();
		System.out.println("Time spent on cloud for fitness function calculations = " + Util.convertNanosecondsToSeconds((endTimeOnCloud - startTimeOnCloud)) + " sec");
		System.out.println("\nFinished all threads of runInstance for fitness function calculations");
		
		// convert all updated chromosome json obtained from cloud to objects
		Set<String> chromosomeFilePaths = new HashSet<String>();
		for(File json : new File(jsonInDirPath).listFiles())
		{
			if(json.isFile() && json.getName().startsWith("chromosome_" + searchStep) && json.getName().endsWith("json"))
			{
				chromosomeFilePaths.add(json.getAbsolutePath());
			}
		}
		
		// update population with the new chromosomes
		List<Chromosome> updatedPopulation = new ArrayList<>();
		for(String json : chromosomeFilePaths)
		{
	        try (Reader reader = new FileReader(json)) {
	        	Chromosome chromosome = gson.fromJson(reader, Chromosome.class);
	        	fitnessCalls++;
	        	fitnessTimeInSec = fitnessTimeInSec + chromosome.getFitnessFunctionObj().fitnessTimeInSec;
	        	updatedPopulation.add(chromosome);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
		}
		return updatedPopulation;
	}
	
	public void calculateFitnessScore(Chromosome chromosome) {
		long startTime = System.nanoTime();
		Util.applyNewValues(chromosome);
		computeFitnessScore();
		chromosome.setFitnessFunctionObj(this);

		long endTime = System.nanoTime();

		fitnessCalls++;
		fitnessTimeInSec = fitnessTimeInSec
				+ Util.convertNanosecondsToSeconds((endTime - startTime));
	}

	private double calculateUsabilityScore()
	{
		// get usability score API result
		GoogleAPIResults gar = new GoogleAPIResults();
		gar.processUsabilityScoreAPIResult();
		double usabilityScore = gar.getUsabilityScoreAPIObj().getUsabilityScore();
		
		// if usability score is above threshold, invoke mobile friendly API
		/*if (usabilityScore >= Constants.USABILITY_SCORE_THRESHOLD) {
			// MFPR.setMobileFriendly(true);
			gar.processMobileFriendlyAPIResult();
			if (gar.getMobileFriendlyAPIObj().isStatusComplete()) {
				System.out
						.println("Mobile friendly API reported MOBILE_FRIENDLY");
				MFPR.setMobileFriendly(gar.getMobileFriendlyAPIObj()
						.isMobileFriendly());
			} else {
				System.out
						.println("Mobile friendly API failed to complete the test, but setting the status of the page as "
								+ "mobile friendly as it is above the threshold score of "
								+ Constants.USABILITY_SCORE_THRESHOLD);
				MFPR.setMobileFriendly(true);
			}
		}*/
		
		return usabilityScore;
	}
	
	private double[] calculateIntraSegmentAestheticScore(SegmentModel updatedSegmentModel)
	{
		double[] intraSegmentViolation = new double[]{0, 0};
		double intraSegmentViolatedScore = 0;
		//double totalPossibleScore = 0;
		
		for (Segment originalIntrasegmentModel: MFix.getOriginalPageSegments())
		{
			//totalPossibleScore = totalPossibleScore + originalIntrasegmentModel.getEdges().size();
			
			Segment updatedIntrasegmentModel = originalIntrasegmentModel.getUpdatedIntraSegmentEdges();
			System.out.println("\nProcessing for segment S" + originalIntrasegmentModel.getId());
			//System.out.println("Original segment intra edges = " + originalIntrasegmentModel.getEdges());
			//System.out.println("Updated segment intra edges = " + updatedIntrasegmentModel.getEdges());
			List<IntraSegmentEdge> violatedEdges = originalIntrasegmentModel.compareToSegment(updatedIntrasegmentModel);
			for(IntraSegmentEdge e : violatedEdges)
			{
				for(EdgeLabel label : e.getLabels())
				{
					System.out.println("Intra-segment violation for segment S" + originalIntrasegmentModel.getId() + ": " + e);
					intraSegmentViolatedScore = intraSegmentViolatedScore + e.getLabels().size();
					intraSegmentViolatedScore = intraSegmentViolatedScore + Constants.EDGE_LABEL_WEIGHTS.get(label);
				}
			}
		}
		intraSegmentViolation[0] = intraSegmentViolatedScore;
		/*intraSegmentViolation[1] = totalPossibleScore * (Constants.EDGE_LABEL_WEIGHTS.get(EdgeLabel.INTERSECTION) +
									Constants.EDGE_LABEL_WEIGHTS.get(EdgeLabel.CONTAINED_BY) + Constants.EDGE_LABEL_WEIGHTS.get(EdgeLabel.CONTAINS));*/
		System.out.println("Intra-segment violation score = " + intraSegmentViolation[0]);
		//System.out.println("Total possible intra-segment violation score = " + intraSegmentViolation[1]);
		return intraSegmentViolation;
	}
	
	private double calculateAestheticScore()
	{
		double aestheticScore = 0;
		
		// get updated segment model and violated edges
		SegmentModel updatedSegmentModel = MFix.getOriginalPageSegmentModel().getUpdatedSegmentModel();
		
		// get intra segment violations
		double intraSegmentViolation[] = calculateIntraSegmentAestheticScore(updatedSegmentModel);
		
		// calculate inter segment violations
		List<InterSegmentEdge> interSegmentViolatedEdges = MFix.getOriginalPageSegmentModel().compareToSegmentModel(updatedSegmentModel);
		double interSegmentViolatedScore = 0.0;
		String interSegmentViolatedScoreBreakdown = "(";
		for(InterSegmentEdge e : interSegmentViolatedEdges)
		{
			System.out.println("Inter-segment violation: " + e);
			for(EdgeLabel label : e.getLabels())
			{
				interSegmentViolatedScore = interSegmentViolatedScore + Constants.EDGE_LABEL_WEIGHTS.get(label);
				interSegmentViolatedScoreBreakdown = interSegmentViolatedScoreBreakdown + Constants.EDGE_LABEL_WEIGHTS.get(label) + " + ";
			}
		}
		if(interSegmentViolatedScoreBreakdown.length() > 3)
		{
			interSegmentViolatedScoreBreakdown = interSegmentViolatedScoreBreakdown.substring(0, interSegmentViolatedScoreBreakdown.length() - 3) + ")";
		}
		
		// calculate the possible total violated score = no. of edges * (sum of label weights)
		double sumOfLabelWeights = 0.0;
		for(double w : Constants.EDGE_LABEL_WEIGHTS.values())
		{
			sumOfLabelWeights = sumOfLabelWeights + w;
		}
		double possibleTotalViolatedScore = MFix.getOriginalPageSegmentModel().getEdges().size() * sumOfLabelWeights;
		
		// calculate normalized aesthetic score in the range [0, 100]
		//aestheticScore = ((interSegmentViolatedScore + intraSegmentViolation[0]) / (possibleTotalViolatedScore + intraSegmentViolation[1])) * 100.0;
		aestheticScore = interSegmentViolatedScore + intraSegmentViolation[0];
		
		aestheticScoreBreakdown = interSegmentViolatedEdges.toString();
		aestheticScoreBreakdown = aestheticScoreBreakdown + " => " + interSegmentViolatedScoreBreakdown + " / "
								+ "(" + MFix.getOriginalPageSegmentModel().getEdges().size() + "*" + sumOfLabelWeights + ")";
		
		return aestheticScore;
	}
	
	private void computeFitnessScore() {
		
		long startTimeUsabilityScore = System.nanoTime();
		usabilityScore = calculateUsabilityScore();
		long endTimeUsabilityScore = System.nanoTime();
		
		long startTimeAestheticScore = System.nanoTime();
		aestheticScore = calculateAestheticScore();
		long endTimeAestheticScore = System.nanoTime();
		
		System.out.println("Fitness score = " + this);
		System.out.println("Usability score time = " + Util.convertNanosecondsToSeconds(endTimeUsabilityScore - startTimeUsabilityScore) + " sec");
		System.out.println("Aesthetic score time = " + Util.convertNanosecondsToSeconds(endTimeAestheticScore - startTimeAestheticScore) + " sec");
	}

	@Override
	public String toString() {
		String ret = fitnessScore + " => " + fitnessScoreBreakdown;
		return ret;
	}
}
