package mfix.approxAlgo;

import java.util.ArrayList;
import java.util.List;

import mfix.Constants;
import mfix.fitness.FitnessFunction;

public class Chromosome implements Comparable<Chromosome>
{
	private List<Gene> genes;
	private FitnessFunction fitnessFunctionObj;
	private String originTrace;
	private String chromosomeIdentifier;

	public Chromosome()
	{
		this.genes = new ArrayList<Gene>();
		this.originTrace = "";
	}

	public Chromosome(List<Gene> genes)
	{
		this.genes = new ArrayList<Gene>();
		for (Gene gene : genes)
		{
			addGene(gene);
		}
	}
	
	public List<Gene> getGenes()
	{
		return genes;
	}

	public void setGenes(List<Gene> genes)
	{
		this.genes = genes;
	}

	public FitnessFunction getFitnessFunctionObj()
	{
		return fitnessFunctionObj;
	}

	public void setFitnessFunctionObj(FitnessFunction fitnessFunctionObj)
	{
		this.fitnessFunctionObj = fitnessFunctionObj;
	}

	public String getOriginTrace() {
		return originTrace;
	}

	public void setOriginTrace(String originTrace) {
		this.originTrace = originTrace;
	}
	
	public void addOriginToTrace(String origin)
	{
		originTrace = originTrace + ", " + origin;
	}
	
	public String getChromosomeIdentifier()
	{
		return chromosomeIdentifier;
	}

	public void setChromosomeIdentifier(String chromosomeIdentifier)
	{
		this.chromosomeIdentifier = chromosomeIdentifier;
	}

	public void addGene(Gene gene)
	{
		// only add unique genes
		if (!genes.contains(gene))
		{
			genes.add(gene);
		}
	}

	public Gene getGene(int index)
	{
		return genes.get(index);
	}

	public Chromosome copy()
	{
		Chromosome c = new Chromosome();
		for (Gene g : this.genes)
		{
			c.addGene(g.copy());
		}
		c.originTrace = this.originTrace;
		FitnessFunction ff = new FitnessFunction();
		ff.setUsabilityScore(fitnessFunctionObj.getUsabilityScore());
		ff.setAestheticScore(fitnessFunctionObj.getAestheticScore());
		ff.setFitnessScore(fitnessFunctionObj.getFitnessScore());
		c.setFitnessFunctionObj(ff);
		return c;
	}

	public void replaceGene(Gene oldGene, Gene newGene)
	{
		int index = genes.indexOf(oldGene);
		genes.remove(index);
		genes.add(index, newGene);
	}

	@Override
	public int compareTo(Chromosome o)
	{
		if(Constants.IS_FITNESS_SCORE_MAXIMIZING)
		{
			// descending order
			if (o.fitnessFunctionObj.getFitnessScore() < this.fitnessFunctionObj.getFitnessScore())
				return -1;
			else if (o.fitnessFunctionObj.getFitnessScore() > this.fitnessFunctionObj.getFitnessScore())
				return 1;
			return 0;
		}
		else
		{
			// ascending order
			if (o.fitnessFunctionObj.getFitnessScore() > this.fitnessFunctionObj.getFitnessScore())
				return -1;
			else if (o.fitnessFunctionObj.getFitnessScore() < this.fitnessFunctionObj.getFitnessScore())
				return 1;
			return 0;
		}
	}

	@Override
	public String toString()
	{
		return chromosomeIdentifier + " [genes=" + genes + ", fitnessFunction=" + fitnessFunctionObj + ", originTrace={" + originTrace + "}]";
	}
}
