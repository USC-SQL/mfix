package mfix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mfix.WebDriverSingleton.Browser;
import mfix.segmentation.EdgeLabel;

public class Constants {
	/* GENERAL CONFIG PARAMETERS */
	public static boolean RUN_IN_DEBUG_MODE = false;

	/* BROWSER CONFIG PARAMETERS */
	public static Browser BROWSER = Browser.EMULATED_MOBILE;
	public static String FIREFOX_DRIVER_FILEPATH = ""; // enter gecko driver full filepath
	public static String INTERNET_EXPLORER_DRIVER_FILEPATH = "";
	public static String CHROME_DRIVER_FILEPATH = ""; // enter chrome driver full filepath
	public static boolean HEADLESS_FIREFOX = false;
	public static boolean HEADLESS_CHROME = false;

	/* MFPR CONFIG PARAMATERS */
	// sleep time between consecutive API invocations
	public static long SLEEP_TIME_IN_MILLISECONDS = 30000;

	// problem types
	public static final String TAP_TARGET_PROBLEM = "SizeTapTargetsAppropriately";
	public static final String FONT_SIZE_PROBLEM = "UseLegibleFontSizes";
	public static final String CONTENT_SIZE_PROBLEM = "SizeContentToViewport";
	public static final Map<String, String> PROBLEM_TYPES_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put(FONT_SIZE_PROBLEM, "UseLegibleFontSizes");
			put(TAP_TARGET_PROBLEM, "SizeTapTargetsAppropriately");
			put(CONTENT_SIZE_PROBLEM, "SizeContentToViewport");
		}
	};

	// Genetic Algorithm Search
	public static int POPULATION_SIZE = 100;
	
	// API for the other API keys
	public static final List<String> GOOGLE_API_KEYS = Arrays.asList(""); // enter google pagespeed insights and mobile friendly API keys

	// Google mobile friendly and usability score API urls
	public static String MOBILE_FRIENDLY_API = "https://searchconsole.googleapis.com/v1/urlTestingTools/mobileFriendlyTest:run?key=%s";
	public static String USABILITY_SCORE_API = "https://www.googleapis.com/pagespeedonline/v2/runPagespeed?url=%s&strategy=mobile&key=%s";

	// Google suggested values for passing mobile friendly test
	private static HashMap<String, String> GOOGLE_SUGGESTED_VALUES_FOR_FONT_SIZE_ISSUE = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			// legible font-sizes
			put("font-size", "16px");
		}
	};
	private static HashMap<String, String> GOOGLE_SUGGESTED_VALUES_FOR_TAP_TARGET_ISSUE = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			// tap targets
			put("height", "48px");
			put("width", "48px");
			put("padding", "32px");
			put("margin", "32px");
		}
	};

	public static Map<String, HashMap<String, String>> GOOGLE_SUGGESTED_VALUES = new HashMap<String, HashMap<String, String>>() {
		private static final long serialVersionUID = 1L;
		{
			put(FONT_SIZE_PROBLEM, GOOGLE_SUGGESTED_VALUES_FOR_FONT_SIZE_ISSUE);
			put(TAP_TARGET_PROBLEM, GOOGLE_SUGGESTED_VALUES_FOR_TAP_TARGET_ISSUE);
			put(CONTENT_SIZE_PROBLEM, new HashMap<String, String>());
			// expected value of content (viewport) width is added dynamically from the API
		}
	};

	public static final Map<String, List<String>> CSS_PROPERTIES_DEPENDENCY = new HashMap<String, List<String>>() {
		private static final long serialVersionUID = 1L;
		{
			put("font-size", Arrays.asList("line-height"));
			put("width", Arrays.asList("min-width"));
		}
	};

	public static final double TAP_TARGETS_RADIUS = 32.0; // 32px

	public static final double USABILITY_SCORE_THRESHOLD = 80.0;

	public static final List<String> ELEMENTS_WITH_DEFAULT_FONT_SIZE = Arrays.asList("h1", "h2", "h3", "h4", "h5", "h6",
			"sup", "sub", "small", "tt", "input", "select", "font", "body");
	public static final List<String> ELEMENTS_WITH_DEFAULT_WIDTH = Arrays.asList("body", "img", "table", "td", "th");

	public static final List<String> TAP_TARGET_ELEMENTS = Arrays.asList("a", "button", "input");

	public static final List<String> IGNORE_TAGS = Arrays.asList("svg");

	// Weights for edge labels
	public static final double EDGE_LABEL_INTERSECTION_WEIGHT = 1;
	public static final double EDGE_LABEL_CONTAINED_BY_WEIGHT = 0.25;
	public static final double EDGE_LABEL_CONTAINS_WEIGHT = 0.25;
	public static final double EDGE_LABEL_ABOVE_WEIGHT = 0.5;
	public static final double EDGE_LABEL_BELOW_WEIGHT = 0.5;
	public static final double EDGE_LABEL_LEFT_WEIGHT = 0.5;
	public static final double EDGE_LABEL_RIGHT_WEIGHT = 0.5;

	public static final Map<EdgeLabel, Double> EDGE_LABEL_WEIGHTS = new HashMap<EdgeLabel, Double>() {
		private static final long serialVersionUID = 1L;
		{
			put(EdgeLabel.INTERSECTION, EDGE_LABEL_INTERSECTION_WEIGHT);
			put(EdgeLabel.CONTAINED_BY, EDGE_LABEL_CONTAINED_BY_WEIGHT);
			put(EdgeLabel.CONTAINS, EDGE_LABEL_CONTAINS_WEIGHT);
			put(EdgeLabel.ABOVE, EDGE_LABEL_ABOVE_WEIGHT);
			put(EdgeLabel.BELOW, EDGE_LABEL_BELOW_WEIGHT);
			put(EdgeLabel.LEFT, EDGE_LABEL_LEFT_WEIGHT);
			put(EdgeLabel.RIGHT, EDGE_LABEL_RIGHT_WEIGHT);
		}
	};

	// viewport (ghost) segment ID
	public static final int VIEWPORT_SEGMENT_ID = 0;
	
	// AMAZON CLOUD PARAMETERS
	public static String KEY_PAIR_PATH = "";	// enter amazon public keypair
    public static String AWS_SCRIPTS_FOLDER_PATH = "src/main/resources";
    public static String MFPR_JAR_PATH = "";	// enter jar file absolute path
    
    // segmentation
    public static final double SEGMENT_TERMINATE_THRESHOLD = 4;
    
    // Fitness function
    public static final boolean IS_FITNESS_SCORE_MAXIMIZING = false;
}
