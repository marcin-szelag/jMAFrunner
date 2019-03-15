package pl.poznan.put.cs.idss.jrs.jmaf;

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
import pl.poznan.put.cs.idss.jrs.rules.MonotonicVCDomLem;
import pl.poznan.put.cs.idss.jrs.rules.Rule;
import pl.poznan.put.cs.idss.jrs.rules.RulesContainer;
import pl.poznan.put.cs.idss.jrs.rules.SimpleConditionValidator;
import pl.poznan.put.cs.idss.jrs.rules.StandardVCDomLem;
import pl.poznan.put.cs.idss.jrs.rules.UnionDecisionsPredictor;
import pl.poznan.put.cs.idss.jrs.rules.VCDomLem;
import pl.poznan.put.cs.idss.jrs.utilities.ISFLoader;

/**
 * Command-line runner generating jMAF (<a href="http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html">http://www.cs.put.poznan.pl/jblaszczynski/Site/jRS.html</a>) readable files.
 * 
 * @author Marcin Szeląg
 */
public class Runner {

	static enum UnionType {
		STANDARD,
		MONOTONIC;
		
		public String toString() {
			return this == STANDARD ? "standard" : "monotonic";
		}
		
		public static UnionType parse(String type) {
			if (type.equals("standard")) {
				return STANDARD;
			} else {
				if (type.equals("monotonic")) {
					return MONOTONIC;
				} else {
					throw new InvalidValueException("Could not recognize union type.");
				}
			}
		}
	}
	
	static enum RuleType {
		CERTAIN,
		POSSIBLE;
		
		public String toString() {
			return this == CERTAIN ? "certain" : "possible";
		}
		
		public static RuleType parse(String type) {
			if (type.equals("certain")) {
				return CERTAIN;
			} else {
				if (type.equals("possible")) {
					return POSSIBLE;
				} else {
					throw new InvalidValueException("Could not recognize rule type.");
				}
			}
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
	
	//TODO:
	//data file(s)
	//union type: standard/monotonic
	//consistencyLevel
	//rules: certain/possible
	//
	//classification: reclassification / test set / cross-validation
	//classifier: drsa, vc-drsa, hybrid
	
	/**
	 * Application entry point
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("---------- jMAFrunner v.0.1.0");
			System.out.println("Wrongs number of program parameters (should be 4). Synopsis:");
			System.out.println("jMAFrunner learning-data-file-path union-type consistency-level rule-type");
			System.out.println("----------");
			return;
		}
		
		//set system console as default message output
		SystemOut systemOut = new SystemOut();
		OM.addOutput(systemOut);
		OM.setDefaultOutput(systemOut.getKey()); 

		boolean applyRules = false; //informs if classification with rules should be performed too
		
		String learningDataFile = args[0];
		UnionType unionType = UnionType.parse(args[1]);
		double parsedConsistencyThreshold = Double.valueOf(args[2]);
		double consistencyThreshold = (unionType == UnionType.STANDARD ?
				parsedConsistencyThreshold :
				1 - parsedConsistencyThreshold); //adjust for cost-type measure (1, denoting 100% consistency, should be translated to threshold 0)
		RuleType ruleType = RuleType.parse(args[3]);
		
		if (ruleType == RuleType.POSSIBLE && parsedConsistencyThreshold < 1) {
			System.out.println("Could not generate possible rules for consistency threshold lower than 1.");
			return;
		}
		
		//TODO: handle classification phase parameters
		
		//print parameters
		System.out.println("---------- jMAFrunner v.0.1.0");
		System.out.println("Assuming the following calculation parameters:");
		System.out.println("Learning data file: "+learningDataFile);
		System.out.println("Type of unions: "+unionType);
		System.out.println("Consistency level: "+parsedConsistencyThreshold);
		System.out.println("Type of rules: "+ruleType);
		System.out.println("----------");
		
		MemoryContainer decisionTable;
		UnionContainer unionContainer;
		
		//load decision table from file
		decisionTable = ISFLoader.loadISFIntoMemoryContainer(learningDataFile, new SimpleParseLog());
		if (decisionTable == null) {
			System.out.println("Could not read data from " + learningDataFile + " file.");
			return;
		} else {
			System.out.println("Loaded "+learningDataFile);
		}
		
		//create union container for learning data table
		try {
			unionContainer = (unionType == UnionType.STANDARD ?
					new StandardUnionContainer(decisionTable) :
					new MonotonicUnionContainer(decisionTable)); //guess decision criterion number
		}
		catch (InvalidValueException exception) {
			System.out.println(exception.getMessage()); //!= 1 active decision criterion
			return;
		}
		
		//write approximations to file
		String apxFile = learningDataFile.replaceFirst(".isf", ".apx"); //prepare name of the file
		try {
			unionContainer.writeApproximations(apxFile, consistencyThreshold, true, "Dominance cones and approximations generated with jRS library ("+unionType+" unions)");
			System.out.println("Approximations written to " + apxFile + " file.");
		}
		catch (IOException exception) {
			System.out.println("Could not write approximations to " + apxFile + "file.");
			System.out.println(exception.getMessage());
		}
		
		//create empty rules container
		RulesContainer rulesContainer = new RulesContainer(decisionTable, consistencyThreshold);
		//create rule generator
		VCDomLem vcDomLem = (unionType == UnionType.STANDARD ?
				new StandardVCDomLem() :
				new MonotonicVCDomLem());
		
		//generate at least rule
		ArrayList<Rule> atLeastRules = vcDomLem.generateRules(
				unionContainer.getUpwardUnions(),
				consistencyThreshold,
				new UnionDecisionsPredictor(),
				ruleType.getType(),
				Rule.AT_LEAST,
				new SimpleConditionValidator(),
				decisionTable,
				VCDomLem.MIX_CONDITIONS_FROM_DIFFERENT_OBJECTS,
				parsedConsistencyThreshold == 1 ? VCDomLem.COVER_NONE_OF_NEGATIVE_EXAMPLES : VCDomLem.COVER_ONLY_INCONSISTENT_NEGATIVE_EXAMPLES);
		//generate at most rule
		ArrayList<Rule> atMostRules = vcDomLem.generateRules(
				unionContainer.getDownwardUnions(),
				consistencyThreshold,
				new UnionDecisionsPredictor(),
				ruleType.getType(),
				Rule.AT_MOST,
				new SimpleConditionValidator(),
				decisionTable,
				VCDomLem.MIX_CONDITIONS_FROM_DIFFERENT_OBJECTS,
				parsedConsistencyThreshold == 1 ? VCDomLem.COVER_NONE_OF_NEGATIVE_EXAMPLES : VCDomLem.COVER_ONLY_INCONSISTENT_NEGATIVE_EXAMPLES);
		
		//store induced rules in the rule container 
		for (int i = 0; i < atLeastRules.size(); i++) {
			rulesContainer.storeRule(atLeastRules.get(i));
		}
		for (int i = 0; i < atMostRules.size(); i++) {
			rulesContainer.storeRule(atMostRules.get(i));
		}
		
		//write rules to file
		String rulesFile = learningDataFile.replaceFirst(".isf", ".rules"); //prepare name of the file
		try {
			rulesContainer.writeRules(rulesFile, true, true);
			System.out.println("Induced rules written to " + rulesFile + " file.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (applyRules) {
			String testDataFile = learningDataFile; //TODO: handle ways of classification
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

}
