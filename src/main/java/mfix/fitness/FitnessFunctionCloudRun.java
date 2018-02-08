package mfix.fitness;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Modifier;

import mfix.MFix;
import mfix.WebDriverSingleton;
import mfix.approxAlgo.Chromosome;
import mfix.domTree.HtmlDomTree;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class FitnessFunctionCloudRun
{
	public static void main(String[] args)
	{
		// args[0] -> chromosome json
		// args[1] -> cloud instance DNS (for building the URL)
		
		String chromosomeFilename = new File(args[0]).getName();
		
		// convert chromosome and MFPR jsons to objects
		Chromosome chromosome = null;
		GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);
		Gson gson = builder.create();
        try (Reader reader = new FileReader(args[0])) {
            chromosome = gson.fromJson(reader, Chromosome.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String putDirectory = "/home/ubuntu/put";
        MFix MFPRObject = new MFix();
        try (Reader reader = new FileReader(putDirectory + File.separatorChar + "MFPR.json")) {
        	MFPRObject = gson.fromJson(reader, MFix.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
        // overwrite MFPR.url and filepath with the respective cloud instance paths
        MFix.setUrl("http://" + args[1] + "/put/");
        MFix.setFilepath(putDirectory + File.separatorChar + "index.html");
        
        // run fitness function
        WebDriverSingleton.loadPage(MFix.getFilepath());
        HtmlDomTree.resetInstance();
        HtmlDomTree instance = HtmlDomTree.getInstance(MFix.getFilepath());
		instance.buildHtmlDomTree();
		FitnessFunction ff = new FitnessFunction();
		ff.calculateFitnessScore(chromosome);
		// get screenshot
		String imageName = chromosomeFilename.split("\\.")[0] + "-screenshot.png";
		WebDriverSingleton.takeScreenshot(putDirectory + File.separatorChar + imageName);
		WebDriverSingleton.closeBrowser();
		
		// store updated chromosome from fitness function as json
		String chromsomeJsonFilepath = putDirectory + File.separatorChar + chromosomeFilename;
		try (FileWriter writer = new FileWriter(chromsomeJsonFilepath))
		{
			gson.toJson(chromosome, writer);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		System.out.println("\nURL = " + MFix.getUrl());
		System.out.println("Chromosome on " + args[1] + ":" + chromosome);
	}
}
