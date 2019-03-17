package pl.poznan.put.cs.idss.jrs.jmaf.runner;

import java.io.IOException;
import java.util.ArrayList;

import pl.poznan.put.cs.idss.jrs.approximations.MonotonicUnionContainer;
import pl.poznan.put.cs.idss.jrs.approximations.StandardUnionContainer;
import pl.poznan.put.cs.idss.jrs.approximations.UnionContainer;
import pl.poznan.put.cs.idss.jrs.classifiers.AllRulesVCDRSAClassificationMethod;
import pl.poznan.put.cs.idss.jrs.classifiers.ClassificationResultsContainer;
import pl.poznan.put.cs.idss.jrs.classifiers.ClassificationResultsValidationContainer;
import pl.poznan.put.cs.idss.jrs.classifiers.Classifier;
import pl.poznan.put.cs.idss.jrs.classifiers.RulesVCDRSAClassificationMethod;
import pl.poznan.put.cs.idss.jrs.core.InvalidTypeException;
import pl.poznan.put.cs.idss.jrs.core.InvalidValueException;
import pl.poznan.put.cs.idss.jrs.core.SimpleParseLog;
import pl.poznan.put.cs.idss.jrs.core.mem.MemoryContainer;
import pl.poznan.put.cs.idss.jrs.output.OM;
import pl.poznan.put.cs.idss.jrs.output.SystemOut;
import pl.poznan.put.cs.idss.jrs.rules.FileInfo;
import pl.poznan.put.cs.idss.jrs.rules.MonotonicVCDomLem;
import pl.poznan.put.cs.idss.jrs.rules.Rule;
import pl.poznan.put.cs.idss.jrs.rules.RuleConstants;
import pl.poznan.put.cs.idss.jrs.rules.RulesContainer;
import pl.poznan.put.cs.idss.jrs.rules.SimpleConditionValidator;
import pl.poznan.put.cs.idss.jrs.rules.StandardVCDomLem;
import pl.poznan.put.cs.idss.jrs.rules.UnionDecisionsPredictor;
import pl.poznan.put.cs.idss.jrs.rules.VCDomLem;
import pl.poznan.put.cs.idss.jrs.types.Attribute;
import pl.poznan.put.cs.idss.jrs.types.Field;
import pl.poznan.put.cs.idss.jrs.utilities.ISFLoader;

/**
 * Command-line runner generating jMAF (<a href="http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html">http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html</a>) readable files.
 * Offers two compatibility modes: jMAF-compatible mode (default for data without missing values),
 * and jRS-compatible mode (which can be optionally set for data without missing values, and is always set for data with missing values).
 * Although jMAF can read ISF files with missing attribute values, it cannot properly show dominance cones, calculate approximations of unions of
 * decision classes, and induce decision rules.
 * This runner allows to bypass this problem as for learning data with missing attribute values it can generate *.apx files with dominance cones
 * and approximations of unions of decision classes, as well as *.rules files with decision rules induced using VC-DomLEM algorithm.
 * 
 * @author Marcin Szeląg
 */
public class Runner {

	static enum UnionType {
		STANDARD,
		MONOTONIC;
		
		public static UnionType parse(String type) {
			if (type.equalsIgnoreCase("standard")) {
				return STANDARD;
			} else {
				if (type.equalsIgnoreCase("monotonic")) {
					return MONOTONIC;
				} else {
					throw new InvalidValueException("Could not recognize union type.");
				}
			}
		}
		
		public String toString() {
			return this == STANDARD ? "standard" : "monotonic";
		}
	}
	
	static enum RuleType {
		CERTAIN,
		POSSIBLE;
		
		public static RuleType parse(String type) {
			if (type.equalsIgnoreCase("certain")) {
				return CERTAIN;
			} else {
				if (type.equalsIgnoreCase("possible")) {
					return POSSIBLE;
				} else {
					throw new InvalidValueException("Could not recognize rule type.");
				}
			}
		}
		
		public String toString() {
			return this == CERTAIN ? "certain" : "possible";
		}
		
		public int getType() {
			switch(this) {
			case CERTAIN:
				return Rule.CERTAIN;
			case POSSIBLE:
				return Rule.POSSIBLE;
			default:
				throw new InvalidTypeException("Could not get rule type.");
			}
		}
	}
	
	static enum CompatibilityMode {
		JMAF,
		JRS;
		
		public static CompatibilityMode parse(String type) {
			if (type.equalsIgnoreCase("jMAF")) {
				return JMAF;
			} else {
				if (type.equalsIgnoreCase("jRS")) {
					return JRS;
				} else {
					throw new InvalidValueException("Could not recognize compatibility mode.");
				}
			}
		}
		
		public static CompatibilityMode getDefaultMonMissingValuesMode() {
			return JMAF;
		}
		
		public static CompatibilityMode getMissingValuesMode() {
			return JRS;
		}
		
		public String toString() {
			return this == JMAF ? "jMAF" : "jRS";
		}
		
		public void applyTo(VCDomLem vcDomLem) {
			switch(this) {
			case JMAF:
				vcDomLem.setConsistencyIn(RuleConstants.CONSISTENCY_IN_POS);
				vcDomLem.setModeOfPositiveExamplesForVCDRSA(VCDomLem.ALL_DIFFERENT_BORDER_POSITIVE_EXAMPLES);
				break;
			case JRS:
				//vcDomLem.setConsistencyIn(RuleConstants.CONSISTENCY_IN_SET); //set default value
				//vcDomLem.setModeOfPositiveExamplesForVCDRSA(VCDomLem.ALL_DIFFERENT_SUFFICIENTLY_CONSISTENT_POSITIVE_EXAMPLES); //set default value
				break;
			}
		}
	}
	
	final String version = "0.1.0";
	final int mandatoryParamsCount = 4;
	final boolean applyRules = false; //informs if classification with rules should be performed too
	
	//handles:
	//learning data file
	//union type: standard/monotonic
	//consistencyLevel
	//rules: certain/possible
	//TODO: handle:
	//test data file
	//classification: reclassification / test set / cross-validation
	//classifier: drsa, vc-drsa, hybrid
	
	/**
	 * Application entry point
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		Runner runner = new Runner();
		runner.setLogging();
		
		if (args.length < runner.mandatoryParamsCount) {
			runner.printSynopsis();
			return;
		}
		
		System.out.println();
		System.out.println("---------- jMAFrunner "+runner.version+" ----------");
		
		//----- <process input parameters> -----
		int parameterIndex = 0;
		
		String learningDataFile = args[parameterIndex++];
		UnionType unionType = UnionType.parse(args[parameterIndex++]);
		double parsedConsistencyThreshold = Double.valueOf(args[parameterIndex++]);
		
		if (parsedConsistencyThreshold > 1 || parsedConsistencyThreshold < 0) {
			System.out.println("Consistency level has to belong to interval [0, 1].");
			return;
		}
		double consistencyThreshold = (unionType == UnionType.STANDARD ? parsedConsistencyThreshold : 1 - parsedConsistencyThreshold); //adjust for cost-type measure (1, denoting 100% consistency, should be translated to threshold 0)
		boolean DRSA = (parsedConsistencyThreshold == 1);
		
		RuleType ruleType = RuleType.parse(args[parameterIndex++]);
		
		if (ruleType == RuleType.POSSIBLE && parsedConsistencyThreshold < 1) {
			System.out.println("Could not generate possible rules for consistency level lower than 1.");
			System.out.println("Assuming certain rules.");
			ruleType = RuleType.CERTAIN;
		}
		
		CompatibilityMode noMissingValuesCompatibilityMode; //refers to the case when data contain no missing value
		boolean noMissingValuesDefaultCompatibilityMode;
		if (args.length > runner.mandatoryParamsCount) {
			noMissingValuesCompatibilityMode = CompatibilityMode.parse(args[parameterIndex++]); //parse next (optional) parameter, after mandatory ones
			noMissingValuesDefaultCompatibilityMode = false;
		} else {
			noMissingValuesCompatibilityMode = CompatibilityMode.getDefaultMonMissingValuesMode();
			noMissingValuesDefaultCompatibilityMode = true;
		}
		CompatibilityMode missingValuesCompatibilityMode = CompatibilityMode.getMissingValuesMode();
		//----- </process input parameters> -----
		
		//----- <print parameters> -----
		System.out.println("Recognized calculation parameters:");
		System.out.println("- learning data file: "+learningDataFile);
		System.out.println("- type of unions: "+unionType);
		System.out.println("- consistency level: "+parsedConsistencyThreshold);
		System.out.println("- type of rules: "+ruleType);
		System.out.println("- compatibility mode for data without missing values: "+noMissingValuesCompatibilityMode
				+(noMissingValuesDefaultCompatibilityMode ? " (default)" : " (set by user)"));
		//TODO: print classification phase parameters (when used)
		//----- </print parameters> -----
		
		//----- <load decision table from file> -----
		MemoryContainer decisionTable = ISFLoader.loadISFIntoMemoryContainer(learningDataFile, new SimpleParseLog());
		if (decisionTable == null) {
			System.out.println("Could not read data from " + learningDataFile + " file.");
			return;
		}
		//----- </load decision table from file> -----
		
		//----- <create union container for learning data table> -----
		UnionContainer unionContainer;
		try {
			unionContainer = (unionType == UnionType.STANDARD ? new StandardUnionContainer(decisionTable) : new MonotonicUnionContainer(decisionTable)); //guess decision criterion number
		}
		catch (InvalidValueException exception) {
			System.out.println(exception.getMessage()); //!= 1 active decision criterion
			return;
		}
		//----- </create union container for learning data table> -----
		
		//----- <write approximations to file> -----
		String apxFile = learningDataFile.replaceFirst(".isf", new StringBuilder()
				.append("_").append(unionType).append("_").append(parsedConsistencyThreshold).append(".apx").toString()); //prepare name of the file
		try {
			unionContainer.writeApproximations(apxFile, consistencyThreshold, true, "Dominance cones and approximations generated with jRS library ("+unionType+" unions)");
			System.out.println("Approximations written to " + apxFile + " file.");
		}
		catch (IOException exception) {
			System.out.println("Could not write approximations to " + apxFile + "file.");
			System.out.println(exception.getMessage());
		}
		//----- </write approximations to file> -----
		
		RulesContainer rulesContainer = new RulesContainer(decisionTable, consistencyThreshold); //create empty rules container
		runner.updateFileInfo(rulesContainer, learningDataFile); //update learning data file directory and name
		
		//create rule generator
		VCDomLem vcDomLem = (unionType == UnionType.STANDARD ? new StandardVCDomLem() : new MonotonicVCDomLem());
		
		//----- <configure VC-DomLEM algorithm> -----
		if (runner.dataHaveMissingValues(decisionTable)) {
			missingValuesCompatibilityMode.applyTo(vcDomLem);
			System.out.println("Data contain missing values, which cannot be handled in jMAF compatibility mode. Applying "+missingValuesCompatibilityMode+" compatibility mode.");
		} else { //no missing value => one can chose calculation mode for certain rules
			noMissingValuesCompatibilityMode.applyTo(vcDomLem);
			System.out.println("Data do not contain missing values. Applying "+noMissingValuesCompatibilityMode+" compatibility mode.");
		}
		//----- </configure VC-DomLEM algorithm> -----
		
		//obsolete features of jMAF (w.r.t. develop version of jRS):
		//a) CONSISTENCY_IN_POS as default setting
		//b) only border examples in approximations for VC-DRSA (VCDomLem.getModeOfPositiveExamplesForVCDRSA() == ALL_DIFFERENT_BORDER_POSITIVE_EXAMPLES),
		//   both in standard and monotonic version
		//c) missing values not handled (due to  a) and b))
		//d) even for data without missing values, jMAF does not handle combination: 1.0 possible monotonic
		//   (Exception: Approximated entity number 0 is not compatible with VC-DOMLEM implementation.)
		//e) existing error in implementation of VC-DomLEM algorithm concerning initialization of rule's statistics (without respecting consistencyIn switch);
		//  this bug was corrected only in commit bc78e14689bc3cd64c202993e402151b54e5f2e8,
		//  but it does not affect jMAF because in jMAF there is: VCDomLEM.DEFAULT_CONSISTENCY_IN = RuleConstants.CONSISTENCY_IN_POS
		//f) c1-ConfirmationMeasure rule statistic not written to *.rules file
		
		ArrayList<Rule> rules;
		long previousCurrentTimeMillis;
		long currentTimeMillis;
		
		previousCurrentTimeMillis = System.currentTimeMillis(); //START TIMER
		rules = runner.generateRules(vcDomLem, unionContainer, consistencyThreshold, RuleType.CERTAIN, decisionTable, DRSA);
		if (ruleType == RuleType.POSSIBLE) {
			rules.addAll(runner.generateRules(vcDomLem, unionContainer, consistencyThreshold, RuleType.POSSIBLE, decisionTable, DRSA));
		}
		currentTimeMillis = System.currentTimeMillis(); //STOP TIMER
		
		rulesContainer.increaseDuration(currentTimeMillis - previousCurrentTimeMillis);
		
		//store induced rules in the rule container 
		for (Rule rule : rules) {
			rulesContainer.storeRule(rule);
		}
		
		//write rules to file
		String rulesFile = apxFile.replaceFirst(".apx", new StringBuilder()
				.append("_").append(ruleType).append(".rules").toString()); //prepare name of the file
		try {
			rulesContainer.writeRules(rulesFile, true, true);
			System.out.println("Induced rules written to " + rulesFile + " file" + " ("+rules.size()+" rules).");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (runner.applyRules) {
			runner.applyRules(rulesContainer, decisionTable, learningDataFile); //TODO: handle different ways of classification
		} //end of rule application section
		
	}
	
	private void printSynopsis() {
		System.out.println();
		System.out.println("---------- jMAFrunner "+version+" ----------");
		System.out.println("Wrongs number of program parameters (should be at least 4; 5th is optional). Synopsis:");
		System.out.println("jMAFrunner learning-data-file-path union-type consistency-level rule-type (compatibility-mode)");
	}
	
	boolean dataHaveMissingValues(MemoryContainer decisionTable) {
		for (int i = 0; i < decisionTable.size(); i++) {
			for (int j = 0; j < decisionTable.getAttrCount(); j++) {
				if (decisionTable.getAttribute(j).getActive() &&
						decisionTable.getAttribute(j).getKind() == Attribute.NONE) { //active condition attribute
					if (decisionTable.getExample(i).getField(j).isUnknown() != Field.KNOWN) { //missing value
						return true;
					}
				}
			}
		}
		return false;
	}
	
	void setLogging() {
		//set system console as default message output
		SystemOut systemOut = new SystemOut();
		OM.addOutput(systemOut);
		OM.setDefaultOutput(systemOut.getKey());
	}
	
	ArrayList<Rule> generateRules(VCDomLem vcDomLem, UnionContainer unionContainer, double consistencyThreshold, RuleType ruleType, MemoryContainer decisionTable, boolean DRSA) {
		//generate at least rules
		ArrayList<Rule> rules = vcDomLem.generateRules(
				unionContainer.getUpwardUnions(),
				consistencyThreshold,
				new UnionDecisionsPredictor(),
				ruleType.getType(),
				Rule.AT_LEAST,
				new SimpleConditionValidator(),
				decisionTable,
				VCDomLem.MIX_CONDITIONS_FROM_DIFFERENT_OBJECTS,
				DRSA ? VCDomLem.COVER_NONE_OF_NEGATIVE_EXAMPLES : VCDomLem.COVER_ONLY_INCONSISTENT_NEGATIVE_EXAMPLES);
		//generate at most rules
		rules.addAll(vcDomLem.generateRules(
				unionContainer.getDownwardUnions(),
				consistencyThreshold,
				new UnionDecisionsPredictor(),
				ruleType.getType(),
				Rule.AT_MOST,
				new SimpleConditionValidator(),
				decisionTable,
				VCDomLem.MIX_CONDITIONS_FROM_DIFFERENT_OBJECTS,
				DRSA ? VCDomLem.COVER_NONE_OF_NEGATIVE_EXAMPLES : VCDomLem.COVER_ONLY_INCONSISTENT_NEGATIVE_EXAMPLES));
		return rules;
	}
	
	void updateFileInfo(RulesContainer rulesContainer, String learningDataFile) {
		FileInfo fileInfo = rulesContainer.getFileInfo(); //gives direct reference
		int lastIndexOf = learningDataFile.lastIndexOf("/");
		if (lastIndexOf == -1) {
			lastIndexOf = learningDataFile.lastIndexOf("\\");
		}
		if (lastIndexOf != -1) {
			fileInfo.putParameterValue("DataFileDirectory", learningDataFile.substring(0, lastIndexOf)); //substring without char at lastIndexOf
			fileInfo.putParameterValue("DataFileName", learningDataFile.substring(lastIndexOf + 1));
		}
	}
	
	void applyRules(RulesContainer rulesContainer, MemoryContainer decisionTable, String testDataFile) {
		//perform classification using induced rules:
		
		//load test information table from file
		MemoryContainer testDecisionTable = ISFLoader.loadISFIntoMemoryContainer(testDataFile);
		if (testDecisionTable == null) {
			System.out.println("Could not read data from " + testDataFile + " file.");
			return;
		} else {
			System.out.println("Loaded " + testDataFile);
		}
		
		//create objects responsible for classification of examples from the test information table
		AllRulesVCDRSAClassificationMethod vcdrsaMethodOnlyCertain = 
				new AllRulesVCDRSAClassificationMethod(testDecisionTable, rulesContainer);
		vcdrsaMethodOnlyCertain.setRuleType(RulesVCDRSAClassificationMethod.ONLY_CERTAIN);
		
		Classifier classifierOnlyCertain = 
				new Classifier(vcdrsaMethodOnlyCertain, rulesContainer.getNumberOfUsedActiveDecisionAttribute());
		
		ClassificationResultsContainer classificationResultsContainerOnlyCertain =
				new ClassificationResultsValidationContainer(
						classifierOnlyCertain, decisionTable, testDecisionTable
				);
		
		//classify all examples from the test information table
		/*Vector<ClassificationResult> classificationResultsOnlyCertain = */
		classificationResultsContainerOnlyCertain.classifyAllObjects();
		
		String classificationFile = testDataFile.replaceFirst(".isf", ".cls"); //prepare name of the file
		String classificationMatrixFile = testDataFile.replaceFirst(".isf", ".mtx"); //prepare name of the file
		
		//write classification results and misclassification matrix
		try {
			((ClassificationResultsValidationContainer)classificationResultsContainerOnlyCertain)
					.writeClassificationResultsRAW(classificationFile);
			System.out.println("Classification results written to " + classificationFile + " file.");
			
			((ClassificationResultsValidationContainer)classificationResultsContainerOnlyCertain)
					.writeMisclassificationMatrix(classificationMatrixFile);
			System.out.println("Misclassification matrix written to " + classificationMatrixFile + " file.");
		} catch (IOException exception){
			System.out.println("Could not write classification results to " + classificationFile + "file.");
			System.out.println(exception.getMessage());
		}
	}

}
