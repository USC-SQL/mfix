package mfix;

import java.util.List;
import java.util.Map;

import mfix.domTree.DependencyGraph;
import mfix.segmentation.Segment;
import mfix.segmentation.SegmentModel;

public class MFix
{
	private transient static String url;
	private transient static String filepath;
	private transient static String copiedFilepath;
	private transient static String outputFolderPath;
	//map of <segment_issue_ID, DG>
	private static Map<String, DependencyGraph> segmentToDG;
	private static SegmentModel originalPageSegmentModel;
	private static List<Segment> originalPageSegments;
	
	private transient static double beforeUsabilityScore;
	private transient static double afterUsabilityScore;
	private transient static boolean isMobileFriendly;
	
	// List of available Amazon cloud instances 
	private transient static List<String> awsInstances;

	public static String getUrl()
	{
		return url;
	}

	public static void setUrl(String url)
	{
		MFix.url = url;
	}

	public static String getFilepath()
	{
		return filepath;
	}

	public static void setFilepath(String filepath)
	{
		MFix.filepath = filepath;
	}

	public static double getBeforeUsabilityScore()
	{
		return beforeUsabilityScore;
	}

	public static void setBeforeUsabilityScore(double beforeUsabilityScore)
	{
		MFix.beforeUsabilityScore = beforeUsabilityScore;
	}

	public static double getAfterUsabilityScore()
	{
		return afterUsabilityScore;
	}

	public static void setAfterUsabilityScore(double afterUsabilityScore)
	{
		MFix.afterUsabilityScore = afterUsabilityScore;
	}

	public static boolean isMobileFriendly()
	{
		return isMobileFriendly;
	}

	public static void setMobileFriendly(boolean isMobileFriendly)
	{
		MFix.isMobileFriendly = isMobileFriendly;
	}

	public static String getCopiedFilepath()
	{
		return copiedFilepath;
	}

	public static void setCopiedFilepath(String copiedFilepath)
	{
		MFix.copiedFilepath = copiedFilepath;
	}

	public static String getOutputFolderPath() {
		return outputFolderPath;
	}

	public static void setOutputFolderPath(String outputFolderPath) {
		MFix.outputFolderPath = outputFolderPath;
	}

	public static Map<String, DependencyGraph> getSegmentToDG()
	{
		return segmentToDG;
	}

	public static void setSegmentToDG(Map<String, DependencyGraph> segmentToDG)
	{
		MFix.segmentToDG = segmentToDG;
	}
	
	public static SegmentModel getOriginalPageSegmentModel()
	{
		return originalPageSegmentModel;
	}

	public static void setOriginalPageSegmentModel(SegmentModel originalPageSegmentModel)
	{
		MFix.originalPageSegmentModel = originalPageSegmentModel;
	}

	public static List<Segment> getOriginalPageSegments()
	{
		return originalPageSegments;
	}

	public static void setOriginalPageSegments(List<Segment> originalPageSegments)
	{
		MFix.originalPageSegments = originalPageSegments;
	}

	public static List<String> getAwsInstances()
	{
		return awsInstances;
	}

	public static void setAwsInstances(List<String> awsInstances)
	{
		MFix.awsInstances = awsInstances;
	}
}
