package mfix.approxAlgo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mfix.Constants;
import mfix.MFix;
import mfix.Util;
import mfix.fitness.FitnessFunction;
import mfix.fitness.SetupCloudRunnable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Search {
	private Chromosome initialChromosome;
	private List<Chromosome> currentPopulation;

	public Search(Chromosome chromosome) {
		this.initialChromosome = chromosome;
		this.currentPopulation = new ArrayList<Chromosome>();
	}

	private void initialize(int populationSize) {
		// initialize first chromosome with all genes as suggested values from Google
		Chromosome c = initialChromosome.copy();
		for (Gene g : initialChromosome.getGenes()) {
			Gene newGene = g.copy();
			newGene.setValue(Constants.GOOGLE_SUGGESTED_VALUES.get(g.getIssueType()).get(g.getCssProperty()));
			c.replaceGene(g, newGene);
		}
		c.addOriginToTrace("initialization");
		currentPopulation.add(c);

		// initialize other chromosomes in the population
		for (int i = 1; i < populationSize; i++) {
			Chromosome temp = initialChromosome.copy();
			for (Gene g : initialChromosome.getGenes()) {
				
					Gene newGene = g.copy();
					List<Double> inputs = Util.generateGaussianInputs(g.getIssueType());
					double val = Util.getGaussianValue(inputs.get(0), inputs.get(1), inputs.get(2));
					newGene.setValue(String.valueOf(val) + "px");
					temp.replaceGene(g, newGene);
			}
			temp.addOriginToTrace("initialization");
			currentPopulation.add(temp);
		}
	}

	public void printPopulation(List<Chromosome> population)
	{
		int count = 1;
		System.out.println("(Size = " + population.size() + ")");
		for(Chromosome c : population)
		{
			System.out.println(count + ". " + c);
			count++;
		}
	}
	
	public void runGA() {
		
		doCloudSetup();
		
		// initialize population with suggested values from Google
		System.out.println("\nRunning initialization");
		
		initialize(Constants.POPULATION_SIZE);

		FitnessFunction ff = new FitnessFunction();

		// for approximation algorithm, break down the total population in groups of number of AWS instances
		int cnt = 0;
		List<Chromosome> batch = new ArrayList<>();
		List<Chromosome> tempPopulation = new ArrayList<>();
		int batchCnt = 0;
		for(Chromosome c : currentPopulation)
		{
			if(cnt < MFix.getAwsInstances().size())
			{
				batch.add(c.copy());
				cnt++;
			}
			else
			{
				tempPopulation.addAll(ff.calculateFitnessScoreForPopulation(batch, "initialization_batch" + batchCnt));
				cnt = 0;
				batch = new ArrayList<>();
				batchCnt++;
			}
		}
		tempPopulation.addAll(ff.calculateFitnessScoreForPopulation(batch, "initialization_batch" + batchCnt));
		currentPopulation = new ArrayList<>(tempPopulation);

		System.out.println("Initial population = ");
		printPopulation(currentPopulation);
	}
	
	public Chromosome getSolutionByHeuristic()
	{
		Chromosome solution = new Chromosome();
		
		// first: find all solutions with usability score >= the threshold
		List<Chromosome> usabilityChromosomesList = new ArrayList<>();
		for(Chromosome c : currentPopulation)
		{
			if(c.getFitnessFunctionObj().getUsabilityScore() < Double.MAX_VALUE && c.getFitnessFunctionObj().getUsabilityScore() >= Constants.USABILITY_SCORE_THRESHOLD)
			{
				usabilityChromosomesList.add(c);
			}
		}
		if(usabilityChromosomesList.size() == 0)
		{
			// find the chromosome with the highest usabiity score
			double usabilityScore = Double.MIN_VALUE;
			for(Chromosome c : currentPopulation)
			{
				if(c.getFitnessFunctionObj().getUsabilityScore() < Double.MAX_VALUE && c.getFitnessFunctionObj().getUsabilityScore() > usabilityScore)
				{
					solution = c;
					usabilityScore = c.getFitnessFunctionObj().getUsabilityScore();
				}
			}
		}
		else
		{
			// second: find chromosome with the lowest aesthtic score
			double aestheticScore = Double.MAX_VALUE;
			for(Chromosome c : usabilityChromosomesList)
			{
				if(c.getFitnessFunctionObj().getAestheticScore() < aestheticScore)
				{
					aestheticScore = c.getFitnessFunctionObj().getAestheticScore();
					solution = c;
				}
				// if same aesthetic score then select one that has a higher usability score
				else if(c.getFitnessFunctionObj().getAestheticScore() == aestheticScore)
				{
					if(c.getFitnessFunctionObj().getUsabilityScore() < Double.MAX_VALUE && c.getFitnessFunctionObj().getUsabilityScore() > solution.getFitnessFunctionObj().getUsabilityScore())
					{
						solution = c;
					}
				}
			}
		}
		return solution;
	}
	
	private void doCloudSetup()
	{
		long startTimeOnCloud = System.nanoTime(); 
		
		// store MFPR object as Json
		String jsonOutDirPath = MFix.getOutputFolderPath() + File.separatorChar + "aws_out";
		if(!new File(jsonOutDirPath).exists())
		{
			new File(jsonOutDirPath).mkdir();
		}
		GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
		Gson gson = builder.create();
		MFix MFPRObj = new MFix();
		String MFPRJsonFilepath = jsonOutDirPath + File.separatorChar + "MFPR.json";
		try (FileWriter writer = new FileWriter(MFPRJsonFilepath))
		{
			gson.toJson(MFPRObj, writer);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(MFix.getAwsInstances().size());
		for(String instance : MFix.getAwsInstances())
		{
			// run the gene on the cloud instance
			System.out.println("Sending " + MFPRJsonFilepath + " to " + instance);
			Runnable worker = new SetupCloudRunnable(instance, MFPRJsonFilepath);
			executor.execute(worker);
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {
 
		}
		long endTimeOnCloud = System.nanoTime();
		System.out.println("Time spent on cloud for setup = " + Util.convertNanosecondsToSeconds((endTimeOnCloud - startTimeOnCloud)) + " sec");
		System.out.println("\nFinished all threads of runInstance for setup");
	}
}
