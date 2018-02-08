package mfix.fitness;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import mfix.Constants;

public class FitnessFunctionCloudRunnable implements Runnable
{
	private String instancePublicDNS;
	private String chromosomeJsonFilepath;
	private String localDirectoryPathToStoreOutput;
	
	public FitnessFunctionCloudRunnable(String instancePublicDNS, String chromosomeJsonFilepath, String localDirectoryPathToStoreOutput)
	{
		this.instancePublicDNS = instancePublicDNS;
		this.chromosomeJsonFilepath = chromosomeJsonFilepath;
		this.localDirectoryPathToStoreOutput = localDirectoryPathToStoreOutput;
	}

	public void runFitnessFunctionScript() throws IOException
	{
		/*# $1 -> key pair path
		# $2 -> instance dns
		# $3 -> chromosome json path
		# $4 -> local/host directory path to store output files*/
		
		String runInstanceFile = Constants.AWS_SCRIPTS_FOLDER_PATH + File.separatorChar + "runFitnessFunctionOnInstance.sh";
		String cmd = runInstanceFile + " " + Constants.KEY_PAIR_PATH + " " + instancePublicDNS + " " + chromosomeJsonFilepath + " " + localDirectoryPathToStoreOutput;
		System.out.println("\n**** OUTPUT FROM INSTANCE " + instancePublicDNS + " for " + new File(chromosomeJsonFilepath).getName() + " ****");
		final Process p = Runtime.getRuntime().exec(cmd);

		Runnable consumeIn = new Runnable() {
		  public void run() {
		    InputStream in = p.getInputStream();
		    InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(in, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		    BufferedReader br = new BufferedReader(isr);
		    String line;
		    try {
				while ( (line = br.readLine()) != null ) {
				  System.out.println("inStream: " + line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		    try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		  }
		};

		Runnable consumeErr = new Runnable() {
		  public void run() {
		    InputStream in = p.getErrorStream();
		    InputStreamReader isr = null;
			try {
				isr = new InputStreamReader(in, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		    BufferedReader br = new BufferedReader(isr);
		    String line;
		    try {
				while ( (line = br.readLine()) != null ) {
				  System.out.println("errStream: " + line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		    try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		    try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		  }
		};

		new Thread(consumeIn).start();
		new Thread(consumeErr).start();
		
		try {
			p.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() 
	{
		try {
			runFitnessFunctionScript();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
