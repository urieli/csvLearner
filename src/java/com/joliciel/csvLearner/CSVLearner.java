///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of csvLearner.
//
//csvLearner is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//csvLearner is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with csvLearner.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.csvLearner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

import com.joliciel.csvLearner.CSVEventListReader.TrainingSetType;
import com.joliciel.csvLearner.features.BestFeatureFinder;
import com.joliciel.csvLearner.features.FayyadIraniSplitter;
import com.joliciel.csvLearner.features.FeatureDiscreteLimitWriter;
import com.joliciel.csvLearner.features.FeatureEntropyWriter;
import com.joliciel.csvLearner.features.FeatureSplitter;
import com.joliciel.csvLearner.features.InformationGainSplitter;
import com.joliciel.csvLearner.features.NormalisationLimitReader;
import com.joliciel.csvLearner.features.NormalisationLimitWriter;
import com.joliciel.csvLearner.features.RealValueFeatureDiscretizer;
import com.joliciel.csvLearner.features.RealValueFeatureEvaluator;
import com.joliciel.csvLearner.features.RealValueFeatureNormaliser;
import com.joliciel.csvLearner.features.RegularIntervalSplitter;
import com.joliciel.csvLearner.features.FeatureSplitter.FeatureSplitterType;
import com.joliciel.csvLearner.features.RealValueFeatureNormaliser.NormaliseMethod;
import com.joliciel.csvLearner.maxent.MaxEntModelCSVWriter;
import com.joliciel.csvLearner.maxent.MaxentAnalyser;
import com.joliciel.csvLearner.maxent.MaxentBestFeatureObserver;
import com.joliciel.csvLearner.maxent.MaxentDetailedAnalysisWriter;
import com.joliciel.csvLearner.maxent.MaxentFScoreCalculator;
import com.joliciel.csvLearner.maxent.MaxentModelReader;
import com.joliciel.csvLearner.maxent.MaxentOutcomeCsvWriter;
import com.joliciel.csvLearner.maxent.MaxentOutcomeXmlWriter;
import com.joliciel.csvLearner.maxent.MaxentTrainer;
import com.joliciel.csvLearner.utils.CSVFormatter;
import com.joliciel.csvLearner.utils.FScoreCalculator;
import com.joliciel.csvLearner.utils.LogUtils;

import opennlp.model.MaxentModel;

/**
 * Entry point to train, evaluate and analyse based on
 * a set of CSV files indicating the features and the expected results.
 * @author Assaf Urieli
 *
 */
public class CSVLearner {
    private static final Log LOG = LogFactory.getLog(CSVLearner.class);
    
	String command = null;
	String resultFilePath = null;
	String featureDir = null;
	String groupedFeatureDir = null;
	String maxentModelFilePath = null;
	String maxentModelBaseName = null;
	String outfilePath = null;
	String outDirPath = null;
	FeatureSplitterType splitterType = FeatureSplitterType.REGULAR_INTERVALS;
	boolean generateEventFile = false;
	boolean generateDetailFile = false;
	int testSegment = -1;
	int iterations = 100;
	int cutoff=5;
	double sigma = 0.0;
	double smoothing = 0.0;
	boolean top100 = false;
	double informationGainThreshold = 0;
	int minNodeSize = 1;
	int maxDepth = -1;
	double minErrorRate = -1;
	boolean zipEntryPerEvent = false;
	String missingValueString = null;
	String singleFile = null;
	boolean includeOutcomes = false;
	NormaliseMethod normaliseMethod = NormaliseMethod.NORMALISE_BY_MAX;
	String identifierPrefix = null;
	String filePrefix = null;
	String preferredOutcome = null;
	double bias = 0.0;
	boolean crossValidation=false;
	Collection<String> excludedOutcomes = null;
	Collection<String> includedOutcomes = null;
	Collection<String> combineFiles = null;
	String combinedName = "";
	double minProbToConsider = 0.0;
	String unknownOutcomeName = "";
	boolean skipUnknownEvents = false;
	int featureCount=100;
	String featureFilePath = null;
	String desiredCountFilePath = null;
	boolean denominalise = false;
	boolean balanceOutcomes = false;
	
	public static final String NOMINAL_MARKER = ":::";
	
	public static void main(String[] args) throws Exception {
		
		if (args.length==0) {
	        InputStream usageStream = CSVLearner.class.getResourceAsStream("/com/joliciel/csvLearner/usage.txt");
	        
	        BufferedReader br = new BufferedReader(new InputStreamReader(usageStream));
	        String strLine;
	        while ((strLine = br.readLine()) != null)
		        System.out.println (strLine);
	        
			return;
		}
		
		CSVLearner learner = new CSVLearner(args);
		learner.run();
	}
	
	public CSVLearner(String[] args) {


		for (String arg : args) {
			int equalsPos = arg.indexOf('=');
			String argName = arg.substring(0, equalsPos);
			String argValue = arg.substring(equalsPos+1);
			if (argName.equals("command")) 
				command = argValue;
			else if (argName.equals("resultFile")) 
				resultFilePath = argValue;
			else if (argName.equals("featureDir")) {
				featureDir = argValue;
				File featureDirFile = new File(featureDir);
				if (!featureDirFile.exists())
					throw new RuntimeException("Cannot find featureDir directory: " + featureDir);
			} else if (argName.equals("outDir")) 
				outDirPath = argValue;
			else if (argName.equals("groupedFeatureDir")) {
				groupedFeatureDir = argValue;
				File groupedFeatureDirFile = new File(groupedFeatureDir);
				if (!groupedFeatureDirFile.exists())
					throw new RuntimeException("Cannot find groupedFeatureDir directory: " + groupedFeatureDir);
			} else if (argName.equals("maxentModel")) {
				if (!argValue.endsWith(".zip"))
					throw new RuntimeException("The maxentModel must end with the .zip suffix");
				maxentModelFilePath = argValue;
				maxentModelBaseName = argValue.substring(argValue.lastIndexOf('/')+1, argValue.lastIndexOf('.'));
			} else if (argName.equals("testSegment")) {
				if (argValue.equalsIgnoreCase("cross"))
					crossValidation = true;
				else
					testSegment = Integer.parseInt(argValue);
			} else if (argName.equals("iterations"))
				iterations = Integer.parseInt(argValue);
			else if (argName.equals("cutoff"))
				cutoff = Integer.parseInt(argValue);
			else if (argName.equals("sigma"))
				sigma = Double.parseDouble(argValue);
			else if (argName.equals("smoothing"))
				smoothing = Double.parseDouble(argValue);
			else if (argName.equals("minErrorRate"))
				minErrorRate = Double.parseDouble(argValue);
			else if (argName.equals("informationGainThreshold"))
				informationGainThreshold = Double.parseDouble(argValue);
			else if (argName.equals("minNodeSize"))
				minNodeSize = Integer.parseInt(argValue);
			else if (argName.equals("maxDepth"))
				maxDepth = Integer.parseInt(argValue);
			else if (argName.equals("eventFile")) 
				generateEventFile = argValue.equals("true");
			else if (argName.equals("detailFile")) 
				generateDetailFile = argValue.equals("true");
			else if (argName.equals("singleFile")) 
				singleFile = argValue;
			else if (argName.equals("includeOutcomes")) 
				includeOutcomes = argValue.equals("true");
			else if (argName.equals("zipEntryPerEvent")) 
				zipEntryPerEvent = argValue.equals("true");
			else if (argName.equals("outfile")) 
				outfilePath = argValue;
			else if (argName.equals("top100"))
				top100 = argValue.equals("true");
			else if (argName.equals("splitter")) {
				if (argValue.equalsIgnoreCase("FayyadIrani"))
					splitterType = FeatureSplitterType.FAYYAD_IRANI;
				else if (argValue.equalsIgnoreCase("InformationGain"))
					splitterType = FeatureSplitterType.INFORMATION_GAIN_PERCENT;
				else if (argValue.equalsIgnoreCase("RegularIntervals"))
					splitterType = FeatureSplitterType.REGULAR_INTERVALS;
				else
					throw new RuntimeException("Unknown splitter type: " + argValue);
			} else if (argName.equals("normaliseMethod")) {
				if (argValue.equalsIgnoreCase("max"))
					normaliseMethod = NormaliseMethod.NORMALISE_BY_MAX;
				else if (argValue.equalsIgnoreCase("mean"))
					normaliseMethod = NormaliseMethod.NORMALISE_BY_MEAN;
				else
					throw new RuntimeException("Unknown normalisation method: " + argValue);
			}
			else if (argName.equals("missingValueString"))
				missingValueString = argValue;
			else if (argName.equals("identifierPrefix"))
				identifierPrefix = argValue;
			else if (argName.equals("filePrefix"))
				filePrefix = argValue;
			else if (argName.equals("preferredOutcome"))
				preferredOutcome = argValue;
			else if (argName.equals("unknownOutcomeName"))
				unknownOutcomeName = argValue;
			else if (argName.equals("bias"))
				bias = Double.parseDouble(argValue);
			else if (argName.equals("minProbToConsider"))
				minProbToConsider = Double.parseDouble(argValue);
			else if (argName.equals("excludedOutcomes")) {
				String[] outcomeList = argValue.split(",");
				
				excludedOutcomes = new TreeSet<String>();
				for (String outcome : outcomeList)
					excludedOutcomes.add(outcome);
			} else if (argName.equals("includedOutcomes")) {
				String[] outcomeList = argValue.split(",");
				
				includedOutcomes = new TreeSet<String>();
				for (String outcome : outcomeList)
					includedOutcomes.add(outcome);
			} else if (argName.equals("combineFiles")) {
				String[] fileList = argValue.split(",");
				
				combineFiles = new Vector<String>();
				for (String fileNamePortion : fileList)
					combineFiles.add(fileNamePortion);
			} else if (argName.equals("combinedName")) {
				combinedName = argValue;
			} else if (argName.equals("skipUnknownEvents")) {
				skipUnknownEvents = argValue.equals("true");
			} else if (argName.equals("denominalise")) {
				denominalise = argValue.equals("true");
			} else if (argName.equals("featureCount")) {
				featureCount = Integer.parseInt(argValue);
			} else if (argName.equals("featureFile")) {
				featureFilePath = argValue;
			} else if (argName.equals("desiredCountFile")) {
				desiredCountFilePath = argValue;
			} else if (argName.equals("balanceOutcomes")) {
				balanceOutcomes = argValue.equals("true");
			}
			else
				throw new RuntimeException("Unknown argument: " + argName);
		}

		if (command==null)
			throw new RuntimeException("Missing argument: command");
		
		if (informationGainThreshold<0 || informationGainThreshold>=1)
			throw new RuntimeException("informationGainThreshold must be in the range (0,1]: " + informationGainThreshold);		
	}
	
	/**
	 * @param args
	 */
	public void run() throws Exception {
		long startTime = (new Date()).getTime();
		
		if (command.equals("evaluate")) {
			this.doCommandEvaluate();
		} else if (command.equals("train")) {
			this.doCommandTrain();
		} else if (command.equals("analyse")) {
			this.doCommandAnalyse();
		} else if (command.equals("normalize")) {
			this.doCommandNormalise();
		} else if (command.equals("discretize")) {
			this.doCommandDiscretise();
		} else if (command.equals("evaluateFeatures")) {
			this.doCommandEvaluateFeatures();
		} else if (command.equals("bestFeatures")) {
			this.doCommandBestFeatures();
		} else if (command.equals("copy")) {
			this.doCommandCopy();
		} else if (command.equals("writeModelToCSV")) {
			this.doCommandWriteModelToCSV();
		} else if (command.equals("generateCombination")) {
			this.doCommandGenerateCombination();
		} else {
			throw new RuntimeException("Unknown command: " + command);
		}
		long endTime = (new Date()).getTime() - startTime;
		LOG.debug("Total runtime: " + ((double)endTime / 1000) + " seconds");
	}
	
	private void doCommandGenerateCombination() {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (desiredCountFilePath==null)
			throw new RuntimeException("Missing argument: desiredCountFile");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		
		File outDir = new File(outDirPath);
		outDir.mkdirs();
		
		EventCombinationGenerator generator = new EventCombinationGenerator();
		generator.setResultFilePath(resultFilePath);
		File desiredCountFile = new File(desiredCountFilePath);
		generator.readDesiredCounts(desiredCountFile);
		
		File outFile = null;
		String suffix = "";
		for (Entry<String,Integer> entry : generator.getDesiredCountPerOutcome().entrySet()) {
			suffix += "_" + entry.getValue();
		}
		int i = 1;
		String fileNameBase = resultFilePath.substring(resultFilePath.lastIndexOf('/')+1, resultFilePath.lastIndexOf('.'));
		while (outFile==null) {
			String fileName = fileNameBase + suffix + "_" + i + ".csv";
			outFile = new File(outDir, fileName);
			if (outFile.exists())
				outFile = null;
			i++;
		}
		generator.getCombination();
		generator.writeCombination(outFile);
	}

	private void doCommandEvaluate() throws IOException {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (maxentModelFilePath==null && !crossValidation)
			throw new RuntimeException("Missing argument: maxentModel");
		if (!crossValidation) {
			if (testSegment<0)
				throw new RuntimeException("Missing argument: testSegment");
			if (testSegment>9)
				throw new RuntimeException("testSegment must be an integer between 0 and 9");
		}

		LOG.info("Generating event list from CSV files...");
		CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, false);
		
		GenericEvents events = reader.getEvents();
		
		if (generateEventFile) {
			File eventFile = new File(maxentModelFilePath + ".events.txt");	
			this.generateEventFile(eventFile, events);
		}
		
		
		if (!crossValidation) {
			File modelFile = new File(maxentModelFilePath);
			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(modelFile,false));
			zos.putNextEntry(new ZipEntry(maxentModelBaseName + ".bin"));
			MaxentModel maxentModel = this.train(events, zos);
			zos.flush();
			
			Writer writer = new BufferedWriter(new OutputStreamWriter(zos));
			zos.putNextEntry(new ZipEntry(maxentModelBaseName + ".nrm_limits.csv"));
			this.writeNormalisationLimits(writer);
			zos.flush();
			zos.close();
			
			this.evaluate(maxentModel, events, null);
		} else {
			if (outDirPath==null)
				throw new RuntimeException("Missing argument: outDir");

			File outDir = new File(outDirPath);
			outDir.mkdirs();
			String fileBase = this.featureDir.replace('/', '_');
			fileBase = fileBase.replace(':', '_');
			File fscoreFile = new File(outDir, fileBase + "_fscores.csv");
			Writer fscoreFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fscoreFile, false),"UTF8"));
			try {
				Mean accuracyMean = new Mean();
				StandardDeviation accuracyStdDev = new StandardDeviation();
				Map<String,Mean[]> outcomeFscoreMeans = new TreeMap<String, Mean[]>();
				Map<String,StandardDeviation[]> outcomeFscoreStdDevs = new TreeMap<String, StandardDeviation[]>();
				for (int segment = 0; segment<=9; segment++) {
					fscoreFileWriter.write("Run " + segment + ",\n");
					if (balanceOutcomes) {
						for (String outcome : reader.getOutcomes()) {
							int i = 0;
							for (GenericEvent event : events) {
								if (event.getOutcome().equals(outcome)) {
									boolean test = i % 10 == segment;
									event.setTest(test);
									i++;
								}
							}		
						}
					} else {
						int i = 0;
						for (GenericEvent event : events) {
							boolean test = i % 10 == segment;
							event.setTest(test);
							i++;
						}
					}
					
					MaxentModel maxentModel = this.train(events, null);
					FScoreCalculator<String> fscoreCalculator = this.evaluate(maxentModel, events, fscoreFileWriter);
					accuracyMean.increment(fscoreCalculator.getTotalFScore());
					accuracyStdDev.increment(fscoreCalculator.getTotalFScore());
					for (String outcome : fscoreCalculator.getOutcomeSet()) {
						Mean[] means = outcomeFscoreMeans.get(outcome);
						StandardDeviation[] stdDevs = outcomeFscoreStdDevs.get(outcome);
						if (means==null) {
							means = new Mean[3];
							stdDevs = new StandardDeviation[3];
							means[0] = new Mean();
							means[1] = new Mean();
							means[2] = new Mean();
							stdDevs[0] = new StandardDeviation();
							stdDevs[1] = new StandardDeviation();
							stdDevs[2] = new StandardDeviation();
							outcomeFscoreMeans.put(outcome, means);
							outcomeFscoreStdDevs.put(outcome, stdDevs);
						}
						means[0].increment(fscoreCalculator.getPrecision(outcome));
						means[1].increment(fscoreCalculator.getRecall(outcome));
						means[2].increment(fscoreCalculator.getFScore(outcome));
						stdDevs[0].increment(fscoreCalculator.getPrecision(outcome));
						stdDevs[1].increment(fscoreCalculator.getRecall(outcome));
						stdDevs[2].increment(fscoreCalculator.getFScore(outcome));
					} // next outcome
				} // next segment
			
				fscoreFileWriter.write("outcome,precision avg., precision sigma, recall avg., recall sigma, f-score avg., f-score sigma,\n");
				for (String outcome : outcomeFscoreMeans.keySet()) {
					Mean[] means = outcomeFscoreMeans.get(outcome);
					StandardDeviation[] stdDevs = outcomeFscoreStdDevs.get(outcome);
					fscoreFileWriter.write(CSVFormatter.format(outcome) + ","
							+ CSVFormatter.format(means[0].getResult()) + ","
							+ CSVFormatter.format(stdDevs[0].getResult()) + ","
							+ CSVFormatter.format(means[1].getResult()) + ","
							+ CSVFormatter.format(stdDevs[1].getResult()) + ","
							+ CSVFormatter.format(means[2].getResult()) + ","
							+ CSVFormatter.format(stdDevs[2].getResult()) + ","
							+ "\n");
				}
				fscoreFileWriter.write("TOTAL,,,,,"
						+ CSVFormatter.format(accuracyMean.getResult()) + "," + CSVFormatter.format(accuracyStdDev.getResult()) + ",\n" );
				
				LOG.info("Accuracy mean: " + accuracyMean.getResult());
				LOG.info("Accuracy std dev: " + accuracyStdDev.getResult());
			} finally {
				fscoreFileWriter.flush();
				fscoreFileWriter.close();
			}
		}
		
		LOG.info("#### Complete ####");		
	}
	
	private void doCommandTrain() throws IOException {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (maxentModelFilePath==null)
			throw new RuntimeException("Missing argument: maxentModel");

		CSVEventListReader reader = this.getReader(TrainingSetType.ALL_TRAINING, false);
		GenericEvents events = reader.getEvents();

		if (generateEventFile) {
			File eventFile = new File(maxentModelFilePath + ".events.txt");	
			this.generateEventFile(eventFile, events);
		}

		File modelFile = new File(maxentModelFilePath);
		ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(modelFile,false));
		zos.putNextEntry(new ZipEntry(maxentModelBaseName + ".bin"));
		this.train(events, zos);
		zos.flush();
		
		Writer writer = new BufferedWriter(new OutputStreamWriter(zos));
		zos.putNextEntry(new ZipEntry(maxentModelBaseName + ".nrm_limits.csv"));
		this.writeNormalisationLimits(writer);
		zos.flush();
		zos.close();
		
		LOG.info("#### Complete ####");
	}
	
	private void doCommandAnalyse() throws IOException {
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (maxentModelFilePath==null)
			throw new RuntimeException("Missing argument: maxentModel");
		if (outfilePath==null)
			throw new RuntimeException("Missing argument: outfile");
		
		CSVEventListReader reader = this.getReader(TrainingSetType.ALL_TEST, false);

		GenericEvents events = reader.getEvents();

		try {
			LOG.info("Evaluating test events...");
			ZipInputStream zis = new ZipInputStream(new FileInputStream(maxentModelFilePath));
			ZipEntry ze;
		    while ((ze = zis.getNextEntry()) != null) {
		    	if (ze.getName().endsWith(".bin"))
		    		break;
		    }
		    MaxentModel model = new MaxentModelReader(zis).getModel();
			zis.close();
			
			MaxentAnalyser analyser = new MaxentAnalyser();
			analyser.setMaxentModel(model);
			if (preferredOutcome!=null) {
				analyser.setPreferredOutcome(preferredOutcome);
				analyser.setBias(bias);
			}
			
			File outcomeFile = new File(outfilePath);

			if (outfilePath.endsWith(".xml")) {
				MaxentOutcomeXmlWriter xmlWriter = new MaxentOutcomeXmlWriter(outcomeFile);
				xmlWriter.setMinProbToConsider(minProbToConsider);
				xmlWriter.setUnknownOutcomeName(unknownOutcomeName);
				analyser.addObserver(xmlWriter);
			} else {
				MaxentOutcomeCsvWriter csvWriter = new MaxentOutcomeCsvWriter(model, outcomeFile);
				csvWriter.setMinProbToConsider(minProbToConsider);
				csvWriter.setUnknownOutcomeName(unknownOutcomeName);
				analyser.addObserver(csvWriter);
			}
			
			MaxentBestFeatureObserver bestFeatureObserver = null;
			if (!crossValidation && featureCount>0 && resultFilePath!=null) {
				bestFeatureObserver = new MaxentBestFeatureObserver(model, featureCount, reader.getFeatureToFileMap());
				analyser.addObserver(bestFeatureObserver);
			}

			
			MaxentFScoreCalculator maxentFScoreCalculator = null;
			if (resultFilePath!=null) {
				maxentFScoreCalculator = new MaxentFScoreCalculator();
				maxentFScoreCalculator.setMinProbToConsider(minProbToConsider);
				maxentFScoreCalculator.setUnknownOutcomeName(unknownOutcomeName);
				analyser.addObserver(maxentFScoreCalculator);					
			}
			
			analyser.analyse(events);
			
			if (maxentFScoreCalculator!=null) {
				FScoreCalculator<String> fscoreCalculator = maxentFScoreCalculator.getFscoreCalculator();
				
				LOG.info("F-score: " + fscoreCalculator.getTotalFScore());
				
				File fscoreFile = new File(outfilePath + ".fscores.csv");
				fscoreCalculator.writeScoresToCSVFile(fscoreFile);	
			}
			
			if (bestFeatureObserver!=null) {
				File weightPerFileFile = new File(outfilePath + ".weightPerFile.csv");
				weightPerFileFile.delete();
				weightPerFileFile.createNewFile();
				Writer weightPerFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(weightPerFileFile, false),"UTF8"));
				try {
					bestFeatureObserver.writeFileTotalsToFile(weightPerFileWriter);
				} finally {
					weightPerFileWriter.flush();
					weightPerFileWriter.close();
				}					
			
				LOG.debug("Total feature count: " + reader.getFeatures().size());
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
		
		if (generateEventFile) {
			File eventFile = new File(outfilePath + ".events.txt");	
			this.generateEventFile(eventFile, events);
		}
		LOG.info("#### Complete ####");
	}
	
	private void doCommandNormalise() throws IOException {
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		LOG.info("Generating event list from CSV files...");
		new File(outDirPath).mkdirs();
		
		CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
		
		Map<String,Float> normalisationLimits = null;
		boolean havePreviousLimits = false;
		if (this.maxentModelFilePath!=null) {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(maxentModelFilePath));
			ZipEntry ze;
			boolean foundNormLimits = false;
		    while ((ze = zis.getNextEntry()) != null) {
		    	if (ze.getName().endsWith(".nrm_limits.csv")) {
		    		foundNormLimits = true;
		    		break;
		    	}
		    }
		    if (foundNormLimits) {
		    	NormalisationLimitReader normalisationLimitReader = new NormalisationLimitReader(zis);
		    	normalisationLimits = normalisationLimitReader.read();
		    	havePreviousLimits = true;
		    }
			zis.close();
		}

		Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
		
		// normalising & write to directory
		for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
			String filename = fileEvents.getKey();
			LOG.debug("Normalizing file: " + filename);
			GenericEvents events = fileEvents.getValue();

			RealValueFeatureNormaliser normaliser = null;
			if (havePreviousLimits)
				normaliser = new RealValueFeatureNormaliser(normalisationLimits, events);
			else
				normaliser = new RealValueFeatureNormaliser(reader, events);
			normaliser.setNormaliseMethod(normaliseMethod);
			normaliser.normalise();
			if (!havePreviousLimits)
				normalisationLimits = normaliser.getFeatureToMaxMap();
			
			String prefix = null;
			if (reader.getGroupedFiles().contains(filename))
				prefix = "ng_";
			else
				prefix = "n_";
			
			if (normaliseMethod.equals(NormaliseMethod.NORMALISE_BY_MEAN))
				prefix += "mean_";
			
			File file = new File(outDirPath + "/" + prefix + filename);
			CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
			if (filename.endsWith(".zip"))
				eventListWriter.setFilePerEvent(zipEntryPerEvent);
			if (missingValueString!=null)
				eventListWriter.setMissingValueString(missingValueString);
			if (identifierPrefix!=null)
				eventListWriter.setIdentifierPrefix(identifierPrefix);
			eventListWriter.writeFile(events);
			
			if (!havePreviousLimits) {
				File normalisationLimitFile = new File(outDirPath + "/" + prefix + filename + ".nrm_limits.csv");
				NormalisationLimitWriter limitWriter = new NormalisationLimitWriter(normalisationLimitFile);
				limitWriter.writeFile(normalisationLimits);
			}
			
		}		
	}
	
	private void doCommandDiscretise() throws IOException {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		LOG.info("Generating event list from CSV files...");
		new File(outDirPath).mkdirs();
		
		CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
		
		Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
		
		// classify & write to directory
		for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
			String filename = fileEvents.getKey();
			LOG.debug("Discretizing file: " + filename);
			GenericEvents events = fileEvents.getValue();
			Map<String,Set<Double>> classificationLimits = new TreeMap<String, Set<Double>>();
			RealValueFeatureDiscretizer classifier = new RealValueFeatureDiscretizer();
			classifier.setFeatureSplitter(this.getFeatureSplitter());
			for (String feature : reader.getFileToFeatureMap().get(filename)) {
				Set<Double> splitValues = classifier.discretizeFeature(events, feature);
				classificationLimits.put(feature, splitValues);
			}
			File file = new File(outDirPath + "/c_" + filename);
			CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
			if (filename.endsWith(".zip"))
				eventListWriter.setFilePerEvent(zipEntryPerEvent);
			if (missingValueString!=null)
				eventListWriter.setMissingValueString(missingValueString);
			if (identifierPrefix!=null)
				eventListWriter.setIdentifierPrefix(identifierPrefix);
			eventListWriter.writeFile(events);
			// we also need to write the classification limits
			File classLimitFile = new File(outDirPath + "/c_" + filename + ".dsc_limits.csv");
			FeatureDiscreteLimitWriter classLimitWriter = new FeatureDiscreteLimitWriter(classLimitFile);
			classLimitWriter.writeFile(classificationLimits);
		}
	}
	
	private void doCommandEvaluateFeatures() throws IOException {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		LOG.info("Generating event list from CSV files...");
		new File(outDirPath).mkdirs();
		
		CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
		
		Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
		// classify & write to directory
		for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
			String filename = fileEvents.getKey();
			LOG.debug("Classifying file: " + filename);
			GenericEvents events = fileEvents.getValue();
			Map<String,List<Double>> featureEntropies = new TreeMap<String, List<Double>>();
			RealValueFeatureEvaluator evaluator = new RealValueFeatureEvaluator();
			evaluator.setFeatureSplitter(this.getFeatureSplitter());
			Set<String> featuresPerFile = reader.getFileToFeatureMap().get(filename);
			if (featuresPerFile!=null) {
				for (String feature : featuresPerFile) {
					List<Double> levelEntropies = evaluator.evaluateFeature(events, feature);
					featureEntropies.put(feature, levelEntropies);
				}
				// we also need to write the entropies to a file
				File featureEntropyFile = new File(outDirPath + "/c_" + filename + ".entropies.csv");
				FeatureEntropyWriter featureEntropyWriter = new FeatureEntropyWriter(featureEntropyFile);
				featureEntropyWriter.writeFile(featureEntropies);
			}
		}
	}
	
	private void doCommandBestFeatures() throws IOException {
		if (resultFilePath==null)
			throw new RuntimeException("Missing argument: resultFile");
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		if (maxDepth<=0)
			throw new RuntimeException("Missing argument: maxDepth");
		
		new File(outDirPath).mkdirs();
					
		CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, false);
		
		GenericEvents events = reader.getEvents();
		FeatureSplitter featureSplitter = this.getFeatureSplitter();
		BestFeatureFinder bestFeatureFinder = new BestFeatureFinder(featureSplitter);
		
		File bestFeatureFile = new File(outDirPath + "/bestFeatures.csv");
		bestFeatureFile.delete();
		bestFeatureFile.createNewFile();
		
		Map<String,Collection<NameValuePair>> bestFeatureMap = new HashMap<String, Collection<NameValuePair>>();
		
		List<NameValuePair> bestFeaturesAll = bestFeatureFinder.getBestFeatures(events, null, featureCount);
		String allKey = "### All";
		bestFeatureMap.put(allKey, bestFeaturesAll);
		
		Writer bestFeatureWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bestFeatureFile, false),"UTF8"));
		try {
			bestFeatureFinder.writeFirstLine(bestFeatureWriter, featureCount);
			bestFeatureFinder.writeBestFeatures(bestFeatureWriter, allKey, bestFeaturesAll);
		} finally {
			bestFeatureWriter.flush();
			bestFeatureWriter.close();
		}	
		
		for (String outcome : events.getOutcomes()) {
			List<NameValuePair> bestFeatures = bestFeatureFinder.getBestFeatures(events, outcome, featureCount);
			bestFeatureMap.put(outcome, bestFeatures);
			bestFeatureWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bestFeatureFile, true),"UTF8"));
			try {
				bestFeatureFinder.writeBestFeatures(bestFeatureWriter, outcome, bestFeatures);
			} finally {
				bestFeatureWriter.flush();
				bestFeatureWriter.close();
			}	
		}
		
		for (int featureListSize=100; featureListSize<=featureCount; featureListSize+=100) {
			File featureListFile = new File(outDirPath + "/bestFeatureList" + featureListSize + ".txt");
			featureListFile.delete();
			featureListFile.createNewFile();
			bestFeatureWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(featureListFile, true),"UTF8"));
			try {
				bestFeatureFinder.writeFeatureList(bestFeatureWriter, bestFeatureMap, featureListSize);
			} finally {
				bestFeatureWriter.flush();
				bestFeatureWriter.close();
			}	
		}
	}
	
	private void doCommandCopy() throws IOException {
		if (featureDir==null)
			throw new RuntimeException("Missing argument: featureDir");
		if (outDirPath==null)
			throw new RuntimeException("Missing argument: outDir");
		LOG.info("Generating event list from CSV files...");
		new File(outDirPath).mkdirs();
		
		
		if (singleFile!=null) {
			CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, false);
			GenericEvents events = reader.getEvents();
			
			File file = new File(outDirPath + "/" + singleFile);
			CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
			if (singleFile.endsWith(".zip"))
				eventListWriter.setFilePerEvent(zipEntryPerEvent);
			if (missingValueString!=null)
				eventListWriter.setMissingValueString(missingValueString);
			if (identifierPrefix!=null)
				eventListWriter.setIdentifierPrefix(identifierPrefix);
			eventListWriter.setDenominalise(denominalise);
			eventListWriter.setIncludeOutcomes(includeOutcomes);
			eventListWriter.writeFile(events);
		} else {
			CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
			Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
			
			Map<String,Set<String>> fileGroups = new TreeMap<String, Set<String>>();
			if (combineFiles!=null) {
				Set<String> ungroupedFiles = new TreeSet<String>();
				
				// group the files together
				for (String filename : eventToFileMap.keySet()) {
					boolean grouped = false;
					for (String filenamePortion : combineFiles) {
						if (filename.contains(filenamePortion)) {
							String fileGroupName = filename.replace(filenamePortion, combinedName);
							Set<String> fileGroup = fileGroups.get(fileGroupName);
							if (fileGroup==null) {
								fileGroup = new TreeSet<String>();
								fileGroups.put(fileGroupName, fileGroup);
							}
							fileGroup.add(filename);
							grouped = true;
							break;
						}
					}
					if (!grouped)
						ungroupedFiles.add(filename);
				}
				// generate "super" groups of GenericEvents
				Map<String,GenericEvents> eventToFileGroupMap = new TreeMap<String, GenericEvents>();
				
				for (String fileGroupName : fileGroups.keySet()) {
					GenericEvents groupEvents = new GenericEvents();
					eventToFileGroupMap.put(fileGroupName, groupEvents);
					Set<String> fileGroup = fileGroups.get(fileGroupName);
					for (String filename : fileGroup) {
						GenericEvents events = eventToFileMap.get(filename);
						groupEvents.addAll(events.getEvents());
					}
				}
				
				// add any ungrouped files
				for (String filename : ungroupedFiles) {
					eventToFileGroupMap.put(filename, eventToFileMap.get(filename));
				}
				eventToFileMap = eventToFileGroupMap;
			}
			// normalising & write to directory
			for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
				String filename = fileEvents.getKey();
				LOG.debug("Writing file: " + filename);
				GenericEvents events = fileEvents.getValue();
				
				if (filePrefix==null)
					filePrefix = "";
				File file = new File(outDirPath + "/" + filePrefix + filename);
				CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
				if (filename.endsWith(".zip"))
					eventListWriter.setFilePerEvent(zipEntryPerEvent);
				if (missingValueString!=null)
					eventListWriter.setMissingValueString(missingValueString);
				if (identifierPrefix!=null)
					eventListWriter.setIdentifierPrefix(identifierPrefix);
				eventListWriter.setIncludeOutcomes(includeOutcomes);
				eventListWriter.writeFile(events);
			}
		}
	}
	
	private void doCommandWriteModelToCSV() throws IOException {
		if (maxentModelFilePath==null)
			throw new RuntimeException("Missing argument: maxentModel");
		
		LOG.info("Evaluating test events...");
		ZipInputStream zis = new ZipInputStream(new FileInputStream(maxentModelFilePath));
		ZipEntry ze;
	    while ((ze = zis.getNextEntry()) != null) {
	    	if (ze.getName().endsWith(".bin"))
	    		break;
	    }
	    MaxentModel model = new MaxentModelReader(zis).getModel();
		zis.close();
		
		String csvFilePath = maxentModelFilePath + ".model.csv";
		
		MaxEntModelCSVWriter writer = new MaxEntModelCSVWriter();
		writer.setModel(model);
		writer.setCsvFilePath(csvFilePath);
		writer.setTop100(top100);
		writer.writeCSVFile();
	}
	
	private CSVEventListReader getReader(TrainingSetType trainingSetType, boolean splitEventsByFile) throws IOException {
		LOG.info("Generating event list from CSV files...");
		CSVEventListReader reader =  new CSVEventListReader();
		reader.setResultFilePath(resultFilePath);
		reader.setFeatureDirPath(featureDir);
		reader.setTestSegment(testSegment);
		reader.setGroupedFeatureDirPath(groupedFeatureDir);
		reader.setTrainingSetType(trainingSetType);
		reader.setIncludedOutcomes(includedOutcomes);
		reader.setExcludedOutcomes(excludedOutcomes);
		reader.setSkipUnknownEvents(skipUnknownEvents);
		reader.setSplitEventsByFile(splitEventsByFile);
		
		if (featureFilePath!=null) {
			File featureFile = new File(featureFilePath);
			List<String> features = new Vector<String>();
			Scanner scanner = new Scanner(featureFile);
			try {
				while (scanner.hasNextLine()) {
					features.add(scanner.nextLine().trim().replace(' ', '_'));
				}
			} finally {
				scanner.close();
			}
			reader.setIncludedFeatures(features);
		}
		reader.read();
		return reader;
	}
	
	private MaxentModel train(GenericEvents events, OutputStream outputStream) {
		LOG.info("Training model...");
		MaxentTrainer trainer = new MaxentTrainer();
		trainer.setOutputStream(outputStream);
		trainer.setIterations(iterations);
		trainer.setCutoff(cutoff);
		trainer.setSigma(sigma);
		trainer.setSmoothing(smoothing);
		MaxentModel model = trainer.train(events);
		return model;
	}
	
	private void writeNormalisationLimits(Writer writer) {
		File featureDirFile = new File(featureDir);
		NormalisationLimitReader normalisationLimitReader = new NormalisationLimitReader(featureDirFile);
		
		Map<String,Float> normalisationLimits = normalisationLimitReader.read();
		if (normalisationLimits.size()>0) {
			NormalisationLimitWriter normalisationLimitWriter = new NormalisationLimitWriter(writer);
			normalisationLimitWriter.writeFile(normalisationLimits);
		}
	}
	
	private FScoreCalculator<String> evaluate(MaxentModel model, GenericEvents events, Writer fscoreFileWriter) throws IOException {
		LOG.info("Evaluating test events...");

		MaxentAnalyser analyser = new MaxentAnalyser();
		analyser.setMaxentModel(model);
		if (preferredOutcome!=null) {
			analyser.setPreferredOutcome(preferredOutcome);
			analyser.setBias(bias);
		}
		if (!crossValidation) {
			File outcomeFile = new File(maxentModelFilePath + ".outcomes.csv");
			MaxentOutcomeCsvWriter csvWriter = new MaxentOutcomeCsvWriter(model, outcomeFile);
			csvWriter.setMinProbToConsider(minProbToConsider);
			csvWriter.setUnknownOutcomeName(unknownOutcomeName);
			analyser.addObserver(csvWriter);
		}
		
		MaxentFScoreCalculator maxentFScoreCalculator = new MaxentFScoreCalculator();
		maxentFScoreCalculator.setMinProbToConsider(minProbToConsider);
		maxentFScoreCalculator.setUnknownOutcomeName(unknownOutcomeName);
		analyser.addObserver(maxentFScoreCalculator);
		
		if (!crossValidation && generateDetailFile) {
			File detailFile = new File(maxentModelFilePath + ".details.txt");
			analyser.addObserver(new MaxentDetailedAnalysisWriter(model, detailFile));
		}
				
		analyser.analyse(events);
		
		FScoreCalculator<String> fscoreCalculator = maxentFScoreCalculator.getFscoreCalculator();
		
		LOG.info("F-score: " + fscoreCalculator.getTotalFScore());
		if (!crossValidation) {
			File fscoreFile = new File(maxentModelFilePath + ".fscores.csv");
			fscoreCalculator.writeScoresToCSVFile(fscoreFile);
		} else if (fscoreFileWriter!=null) {
			try {
				fscoreCalculator.writeScoresToCSV(fscoreFileWriter);
			} finally {
				fscoreFileWriter.flush();
			}
		}
		
		return fscoreCalculator;
	}
	
	private void generateEventFile(File eventFile, GenericEvents events) throws IOException {
		eventFile.delete();
		eventFile.createNewFile();
		Writer eventFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(eventFile, false),"UTF8"));
		try {
			for (GenericEvent event : events) {
				eventFileWriter.append(event.getIdentifier() + "\t");
				for (int i = 0; i < event.getFeatures().size(); i++) {
					eventFileWriter.append(event.getFeatures().get(i) + "=" + CSVFormatter.format(event.getWeights().get(i)) + "\t");
				}
				eventFileWriter.append(event.getOutcome());
				eventFileWriter.append("\n");
			}
		} finally {
			eventFileWriter.close();
		}
	}
	
	private FeatureSplitter getFeatureSplitter() {
		FeatureSplitter featureSplitter = null;
		if (splitterType.equals(FeatureSplitterType.FAYYAD_IRANI)) {
			FayyadIraniSplitter fayadIraniSplitter = new FayyadIraniSplitter();
			fayadIraniSplitter.setMaxDepth(maxDepth);
			fayadIraniSplitter.setMinErrorRate(minErrorRate);
			fayadIraniSplitter.setMinNodeSize(minNodeSize);
			featureSplitter = fayadIraniSplitter;
		} else if (splitterType.equals(FeatureSplitterType.INFORMATION_GAIN_PERCENT)) {
			InformationGainSplitter informationGainSplitter = new InformationGainSplitter();
			informationGainSplitter.setInformationGainThreshold(informationGainThreshold);
			informationGainSplitter.setMaxDepth(maxDepth);
			informationGainSplitter.setMinErrorRate(minErrorRate);
			informationGainSplitter.setMinNodeSize(minNodeSize);
			featureSplitter = informationGainSplitter;
		} else {
			RegularIntervalSplitter regularIntervalSplitter = new RegularIntervalSplitter();
			regularIntervalSplitter.setMaxDepth(maxDepth);
			featureSplitter = regularIntervalSplitter;
		}
		return featureSplitter;
	}
}
