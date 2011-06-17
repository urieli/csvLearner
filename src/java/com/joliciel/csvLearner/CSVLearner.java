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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import com.joliciel.csvLearner.RealValueFeatureNormaliser.NormaliseMethod;
import com.joliciel.csvLearner.maxent.MaxentAnalyser;
import com.joliciel.csvLearner.maxent.MaxentDetailedAnalysisWriter;
import com.joliciel.csvLearner.maxent.MaxentFScoreCalculator;
import com.joliciel.csvLearner.maxent.MaxentModelReader;
import com.joliciel.csvLearner.maxent.MaxentOutcomeCsvWriter;
import com.joliciel.csvLearner.maxent.MaxentOutcomeXmlWriter;
import com.joliciel.csvLearner.maxent.MaxentTrainer;
import com.joliciel.csvLearner.utils.CSVFormatter;
import com.joliciel.csvLearner.utils.FScoreCalculator;
import com.joliciel.csvLearner.utils.LogUtils;
import com.joliciel.csvLearner.utils.MaxEntModelCSVWriter;
import com.joliciel.csvLearner.utils.FeatureSplitter.StopConditionTest;

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
	String outDir = null;
	StopConditionTest discretisationTest = StopConditionTest.INFORMATION_GAIN_PERCENT;
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
	Collection<String> combineFiles = null;
	double minProbToConsider = 0.0;
	String unknownOutcomeName = "";
	
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
			else if (argName.equals("featureDir")) 
				featureDir = argValue;
			else if (argName.equals("outDir")) 
				outDir = argValue;
			else if (argName.equals("groupedFeatureDir")) 
				groupedFeatureDir = argValue;
			else if (argName.equals("maxentModel")) {
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
			else if (argName.equals("test")) {
				if (argValue.equalsIgnoreCase("FayyadIrani"))
					discretisationTest = StopConditionTest.FAYYAD_IRANI;
				else
					throw new RuntimeException("Unknown discretisation test: " + argValue);
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
			}
			else if (argName.equals("combineFiles")) {
				String[] fileList = argValue.split(",");
				
				combineFiles = new Vector<String>();
				for (String fileNamePortion : fileList)
					combineFiles.add(fileNamePortion);
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
			if (resultFilePath==null)
				throw new RuntimeException("Missing argument: resultFile");
			if (featureDir==null)
				throw new RuntimeException("Missing argument: featureDir");
			if (maxentModelFilePath==null)
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
				
				this.evaluate(maxentModel, events);
			} else {
				Mean accuracyMean = new Mean();
				StandardDeviation accuracyStdDev = new StandardDeviation();
				for (int segment = 0; segment<=9; segment++) {
					int i = 0;
					for (GenericEvent event : events) {
						event.setTest(i % 10 == segment);
						i++;
					}
					MaxentModel maxentModel = this.train(events, null);
					double accuracy = this.evaluate(maxentModel, events);
					accuracyMean.increment(accuracy);
					accuracyStdDev.increment(accuracy);
				}
				LOG.info("Accuracy mean: " + accuracyMean.getResult());
				LOG.info("Accuracy std dev: " + accuracyStdDev.getResult());
			}
			
			LOG.info("#### Complete ####");
		} else if (command.equals("train")) {
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
		} else if (command.equals("analyse")) {
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
			} catch (IOException ioe) {
				LogUtils.logError(LOG, ioe);
				throw new RuntimeException(ioe);
			}
			
			if (generateEventFile) {
				File eventFile = new File(outfilePath + ".events.txt");	
				this.generateEventFile(eventFile, events);
			}
			LOG.info("#### Complete ####");
		} else if (command.equals("normalize")) {
			if (featureDir==null)
				throw new RuntimeException("Missing argument: featureDir");
			if (outDir==null)
				throw new RuntimeException("Missing argument: outDir");
			LOG.info("Generating event list from CSV files...");
			new File(outDir).mkdir();
			
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
				
				File file = new File(outDir + "/" + prefix + filename);
				CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
				if (filename.endsWith(".zip"))
					eventListWriter.setFilePerEvent(zipEntryPerEvent);
				if (missingValueString!=null)
					eventListWriter.setMissingValueString(missingValueString);
				if (identifierPrefix!=null)
					eventListWriter.setIdentifierPrefix(identifierPrefix);
				eventListWriter.writeFile(events);
				
				if (!havePreviousLimits) {
					File normalisationLimitFile = new File(outDir + "/" + prefix + filename + ".nrm_limits.csv");
					NormalisationLimitWriter limitWriter = new NormalisationLimitWriter(normalisationLimitFile);
					limitWriter.writeFile(normalisationLimits);
				}
				
			}
		} else if (command.equals("discretize")) {
			if (resultFilePath==null)
				throw new RuntimeException("Missing argument: resultFile");
			if (featureDir==null)
				throw new RuntimeException("Missing argument: featureDir");
			if (outDir==null)
				throw new RuntimeException("Missing argument: outDir");
			LOG.info("Generating event list from CSV files...");
			new File(outDir).mkdir();
			
			CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
			
			Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
			
			// classify & write to directory
			for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
				String filename = fileEvents.getKey();
				LOG.debug("Discretizing file: " + filename);
				GenericEvents events = fileEvents.getValue();
				Map<String,Set<Double>> classificationLimits = new TreeMap<String, Set<Double>>();
				RealValueFeatureDiscretizer classifier = new RealValueFeatureDiscretizer();
				classifier.setInformationGainThreshold(informationGainThreshold);
				classifier.setMaxDepth(maxDepth);
				classifier.setMinErrorRate(minErrorRate);
				classifier.setMinNodeSize(minNodeSize);
				classifier.setStopConditionTest(discretisationTest);
				for (String feature : reader.getFeaturesPerFile().get(filename)) {
					Set<Double> splitValues = classifier.discretizeFeature(events, feature);
					classificationLimits.put(feature, splitValues);
				}
				File file = new File(outDir + "/c_" + filename);
				CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
				if (filename.endsWith(".zip"))
					eventListWriter.setFilePerEvent(zipEntryPerEvent);
				if (missingValueString!=null)
					eventListWriter.setMissingValueString(missingValueString);
				if (identifierPrefix!=null)
					eventListWriter.setIdentifierPrefix(identifierPrefix);
				eventListWriter.writeFile(events);
				// we also need to write the classification limits
				File classLimitFile = new File(outDir + "/c_" + filename + ".dsc_limits.csv");
				FeatureDiscreteLimitWriter classLimitWriter = new FeatureDiscreteLimitWriter(classLimitFile);
				classLimitWriter.writeFile(classificationLimits);
			}
		} else if (command.equals("evaluateFeatures")) {
			if (resultFilePath==null)
				throw new RuntimeException("Missing argument: resultFile");
			if (featureDir==null)
				throw new RuntimeException("Missing argument: featureDir");
			if (outDir==null)
				throw new RuntimeException("Missing argument: outDir");
			LOG.info("Generating event list from CSV files...");
			new File(outDir).mkdir();
			
			CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
			
			Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
			// classify & write to directory
			for (Entry<String,GenericEvents> fileEvents : eventToFileMap.entrySet()) {
				String filename = fileEvents.getKey();
				LOG.debug("Classifying file: " + filename);
				GenericEvents events = fileEvents.getValue();
				Map<String,List<Double>> featureEntropies = new TreeMap<String, List<Double>>();
				RealValueFeatureEvaluator evaluator = new RealValueFeatureEvaluator();
				for (String feature : reader.getFeaturesPerFile().get(filename)) {
					List<Double> levelEntropies = evaluator.evaluateFeature(events, feature, informationGainThreshold, minNodeSize, maxDepth, minErrorRate);
					featureEntropies.put(feature, levelEntropies);
				}

				// we also need to write the entropies to a file
				File featureEntropyFile = new File(outDir + "/c_" + filename + ".entropies.csv");
				FeatureEntropyWriter featureEntropyWriter = new FeatureEntropyWriter(featureEntropyFile);
				featureEntropyWriter.writeFile(featureEntropies);
			}
			
		} else if (command.equals("copy")) {
			if (featureDir==null)
				throw new RuntimeException("Missing argument: featureDir");
			if (outDir==null)
				throw new RuntimeException("Missing argument: outDir");
			LOG.info("Generating event list from CSV files...");
			new File(outDir).mkdir();
			
			CSVEventListReader reader = this.getReader(TrainingSetType.TEST_SEGMENT, true);
			
			if (singleFile!=null) {
				GenericEvents events = reader.getEvents();
				
				File file = new File(outDir + "/" + singleFile);
				CSVEventListWriter eventListWriter = new CSVEventListWriter(file);
				if (singleFile.endsWith(".zip"))
					eventListWriter.setFilePerEvent(zipEntryPerEvent);
				if (missingValueString!=null)
					eventListWriter.setMissingValueString(missingValueString);
				if (identifierPrefix!=null)
					eventListWriter.setIdentifierPrefix(identifierPrefix);
				eventListWriter.setIncludeOutcomes(includeOutcomes);
				eventListWriter.writeFile(events);
			} else {
				Map<String,GenericEvents> eventToFileMap = reader.getEventsPerFile();
				
				Map<String,Set<String>> fileGroups = new TreeMap<String, Set<String>>();
				if (combineFiles!=null) {
					Set<String> ungroupedFiles = new TreeSet<String>();
					
					// group the files together
					for (String filename : eventToFileMap.keySet()) {
						boolean grouped = false;
						for (String filenamePortion : combineFiles) {
							if (filename.contains(filenamePortion)) {
								String fileGroupName = filename.replace(filenamePortion, "");
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
					File file = new File(outDir + "/" + filePrefix + filename);
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
		} else if (command.equals("writeModelToCSV")) {
			if (maxentModelFilePath==null)
				throw new RuntimeException("Missing argument: maxentModel");
			if (outfilePath==null)
				throw new RuntimeException("Missing argument: outfile");
			
			MaxEntModelCSVWriter writer = new MaxEntModelCSVWriter();
			writer.setMaxentModelFilePath(maxentModelFilePath);
			writer.setCsvFilePath(outfilePath);
			writer.setTop100(top100);
			writer.writeCSVFile();
		} else {
			throw new RuntimeException("Unknown command: " + command);
		}
		long endTime = (new Date()).getTime() - startTime;
		LOG.debug("Total runtime: " + ((double)endTime / 1000) + " seconds");
	}
	
	private CSVEventListReader getReader(TrainingSetType trainingSetType, boolean splitEventsByFile) {
		LOG.info("Generating event list from CSV files...");
		CSVEventListReader reader =  new CSVEventListReader();
		reader.setResultFilePath(resultFilePath);
		reader.setFeatureDirPath(featureDir);
		reader.setTestSegment(testSegment);
		reader.setGroupedFeatureDirPath(groupedFeatureDir);
		reader.setTrainingSetType(trainingSetType);
		reader.setExcludedOutcomes(excludedOutcomes);
		if (!splitEventsByFile)
			reader.setSplitEventsByFile(false);
		else if (singleFile==null)
			reader.setSplitEventsByFile(true);
		else
			reader.setSplitEventsByFile(false);
		
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
	
	private double evaluate(MaxentModel model, GenericEvents events) {
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
		}
		return fscoreCalculator.getTotalFScore();

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
}
