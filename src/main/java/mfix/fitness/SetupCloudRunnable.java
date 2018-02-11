package mfix.fitness;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import mfix.Constants;
import mfix.MFix;

public class SetupCloudRunnable implements Runnable 
{
	private String instancePublicDNS;
	private String MFPRJsonFilepath;
	
	public SetupCloudRunnable(String instancePublicDNS, String MFPRJsonFilepath) 
	{
		this.instancePublicDNS = instancePublicDNS;
		this.MFPRJsonFilepath = MFPRJsonFilepath;
	}

	public void runSetupScript() throws IOException
	{
		String runInstanceFile = Constants.AWS_SCRIPTS_FOLDER_PATH + File.separatorChar + "setup.sh";
		/*# $1 -> key-pair path
		# $2 -> instance dns
		# $3 -> subject path
		# $4 -> MFPR object json path
		# #5 -> MFPR jar path*/
		String cmd = runInstanceFile + " " + Constants.KEY_PAIR_PATH + " " + instancePublicDNS + " " + new File(MFix.getFilepath()).getParent() + "/" + " " + MFPRJsonFilepath + " " + Constants.MFIX_JAR_PATH;
		System.out.println("**** OUTPUT FROM INSTANCE " + instancePublicDNS + " for setup ****");
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
			runSetupScript();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
