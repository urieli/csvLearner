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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.CSVFormatter;
import com.joliciel.csvLearner.utils.LogUtils;

/**
 * Given a result file and a set of feature files, constructs a set of events to
 * be used for training and evaluation.
 * 
 * @author Assaf Urieli
 * 
 */
public class CSVEventListReader {
	private static final Log LOG = LogFactory.getLog(CSVEventListReader.class);
	private String resultFilePath = null;
	private String featureDirPath = null;
	private String groupedFeatureDirPath = null;
	private int testSegment = -1;
	private TrainingSetType trainingSetType = TrainingSetType.TEST_SEGMENT;
	
	private Map<String, FeatureStats> featureStatsMap = new TreeMap<String, CSVEventListReader.FeatureStats>();
	private Map<String, String> groupedFeatures = new TreeMap<String,String>();
	private Map<String, Float> fileMaxValues = new TreeMap<String,Float>();
	private Map<String, Float> fileMeanValues = new TreeMap<String,Float>();

	private Map<String, GenericEvent> eventMap = null;
	private Map<String,Map<String,GenericEvent>> eventFileMap = null;
	private Set<String> features = new TreeSet<String>();
	private Map<String,Set<String>> featuresPerFile = new TreeMap<String, Set<String>>();
	private Set<String> groupedFiles = new TreeSet<String>();
	private Collection<String> excludedOutcomes = null;
	private Set<String> eventsToExclude = new TreeSet<String>();
	
	private boolean splitEventsByFile = false;
	
	private GenericEvents events = null;
	private Map<String,GenericEvents> eventsPerFile = null;


	
	public enum TrainingSetType {
		ALL_TRAINING, ALL_TEST, TEST_SEGMENT
	}

	public void read() {
		if (splitEventsByFile)
			eventFileMap = new TreeMap<String, Map<String,GenericEvent>>();
		eventMap = new TreeMap<String, GenericEvent>();
		features = new TreeSet<String>();
		try {

			this.scanResultsFile();
			
			File featureDir = new File(featureDirPath);
			this.scanFeatureDir(featureDir, false);
			
			if (groupedFeatureDirPath!=null) {
				File groupedFeatureDir = new File(groupedFeatureDirPath);
				this.scanFeatureDir(groupedFeatureDir, true);
			}
		} catch (IOException ioe) {
			LogUtils.logError(LOG, ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	void scanResultsFile() throws IOException {
		if (this.resultFilePath!=null) {
			// have results
			Scanner resultScanner = new Scanner(new FileInputStream(
					resultFilePath), "UTF-8");

			try {
				int i = 0;
				boolean firstLine = true;
				while (resultScanner.hasNextLine()) {
					String line = resultScanner.nextLine();
					if (!firstLine) {
						List<String> cells = CSVFormatter.getCSVCells(line);
						String ref = cells.get(0);
						int slashIndex = ref.indexOf('/');
						if (slashIndex >= 0)
							ref = ref.substring(slashIndex + 1);
						int dotIndex = ref.indexOf('.');
						if (dotIndex >= 0)
							ref = ref.substring(0, dotIndex);
						String outcome = cells.get(1);
						if (excludedOutcomes!=null && excludedOutcomes.contains(outcome)) {
							this.eventsToExclude.add(ref);
						} else {
							GenericEvent event = new GenericEvent(ref);
							event.setOutcome(outcome);
							if (trainingSetType.equals(TrainingSetType.ALL_TRAINING))
								event.setTest(false);
							else if (trainingSetType.equals(TrainingSetType.ALL_TEST))
								event.setTest(true);
							else if (trainingSetType.equals(TrainingSetType.TEST_SEGMENT))
								event.setTest(i % 10 == testSegment);
							else 
								throw new RuntimeException("Unknown TrainingSetType: " + trainingSetType);
							eventMap.put(ref, event);
							i++;
						}
					}
					firstLine = false;

				}
			} finally {
				resultScanner.close();
			}
		} // have results		
	}
	
	/**
	 * Scan a feature directory and all of its sub-directories, and add the
	 * contents of the feature files to the event map.
	 * 
	 * @param featureDir
	 * @throws IOException
	 */
	void scanFeatureDir(File featureDir, boolean grouped) throws IOException {
		LOG.debug("Scanning feature directory " + featureDir.getPath());
		File[] files = featureDir.listFiles();
		if (files==null) {
			LOG.debug("Not a directory!");
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				// recursively scan this feature sub-directory
				this.scanFeatureDir(file, grouped);
			} else {
				String fileName = file.getName();
				LOG.debug("Scanning file " + fileName);
				Map<String,GenericEvent> currentEventMap = eventMap;
				if (eventFileMap!=null) {
					currentEventMap = new TreeMap<String, GenericEvent>();
					// copy the results to the event map
					for (GenericEvent event : eventMap.values()) {
						GenericEvent eventClone = new GenericEvent(event.getIdentifier());
						eventClone.setTest(event.isTest());
						eventClone.setOutcome(event.getOutcome());
						currentEventMap.put(event.getIdentifier(), eventClone);
					}
					eventFileMap.put(fileName, currentEventMap);
				}
				InputStream inputStream = null;
				try {
					if (fileName.endsWith(".dsc_limits.csv")||fileName.endsWith(".nrm_limits.csv")){
						LOG.debug("Ignoring limits file: " + fileName);
					} else if (fileName.endsWith(".csv")) {
						inputStream = new FileInputStream(file);
						this.scanCSVFile(inputStream, true, grouped, fileName, currentEventMap);
					} else if (fileName.endsWith(".zip")) {
						inputStream = new FileInputStream(file);
						ZipInputStream zis = new ZipInputStream(inputStream);
						ZipEntry zipEntry;
						while ((zipEntry = zis.getNextEntry()) != null) {
							LOG.debug("Scanning zip entry "
									+ zipEntry.getName());

							this.scanCSVFile(zis, false, grouped, fileName, currentEventMap);
							zis.closeEntry();
						}

						zis.close();
					} else {
						throw new RuntimeException(
								"Bad file extension in feature directory: "
										+ file.getName());
					}
				} finally {
					if (inputStream != null)
						inputStream.close();
				}
			} // file or directory?
		} // next file
	}

	private void scanCSVFile(InputStream inputStream, boolean closeStreamer, boolean grouped, String fileName, Map<String,GenericEvent> currentEventMap) {
		// add contents of the current file to the event map.
		
		if (grouped)
			this.groupedFiles.add(fileName);
		
		boolean firstLine = true;
		List<String> featureNames = null;
		Scanner scanner = new Scanner(inputStream, "UTF-8");
		Set<String> featureSet = featuresPerFile.get(fileName);
		if (featureSet==null) {
			featureSet = new TreeSet<String>();
			featuresPerFile.put(fileName, featureSet);
		}
		try {
			int row = 1;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				List<String> cells = CSVFormatter.getCSVCells(line);
				if (firstLine) {
					featureNames = cells;
					boolean firstColumn = true;
					if (grouped) {
						for (String featureName : featureNames) {
							if (!firstColumn)
								groupedFeatures.put(featureName, fileName);
							if (firstColumn) firstColumn = false;
						}
					}
					firstColumn = true;
					for (String featureName : featureNames) {
						if (!firstColumn) {
							features.add(featureName);
							featureSet.add(featureName);
						}
						if (firstColumn) firstColumn = false;
					}
					
					firstLine = false;
				} else {
					boolean firstCell = true;
					GenericEvent event = null;
					int i = 0;
					for (String cell : cells) {
						if (firstCell) {
							String ref = cell;
							int slashIndex = ref.indexOf('/');
							if (slashIndex >= 0)
								ref = ref.substring(slashIndex + 1);
							int dotIndex = ref.indexOf('.');
							if (dotIndex >= 0)
								ref = ref.substring(0, dotIndex);
							
							if (this.eventsToExclude.size()>0 && this.eventsToExclude.contains(ref)) {
								// skip this whole line
								break;
							}
							event = currentEventMap.get(ref);
							if (event == null) {
								if (resultFilePath!=null) {
									throw new RuntimeException(
										"ID not found in result file: " + cell);
								} else {
									event = new GenericEvent(ref);
									event.setTest(true);
									currentEventMap.put(ref, event);
								}
							}
							firstCell = false;
						} else {
							// weight cell
							if (i>featureNames.size()-1)
								throw new RuntimeException("File: " + fileName + ". Too many cells on row: " + row);
							String featureName = featureNames.get(i);
							float weight = 0;
							try {
								weight = Float.parseFloat(cell);
							} catch (NumberFormatException nfe) {
								// skip empty cells
								if (cell.length() > 0) {
									featureName += CSVLearner.NOMINAL_MARKER + cell;
									weight = 1;
								}
							}
							FeatureStats featureStats = this.featureStatsMap.get(featureName);
							if (featureStats==null)
							{
								featureStats = new FeatureStats();
								this.featureStatsMap.put(featureName, featureStats);
							}
							
							// skip cells with an explicit weight of zero
							if (weight > 0) {
								event.addFeature(featureName, weight);

								if (weight > featureStats.max)
									featureStats.max = weight;
								featureStats.count = featureStats.count + 1;
								featureStats.total = featureStats.total + weight;
							}
						} // type of cell
						i++;
					} // next cell
				} // first line?
				row++;
			} // next line
		} finally {
			if (closeStreamer)
				scanner.close();
		}
	}



	/**
	 * A CSV file giving correct result for each event. The top row is ignored.
	 * Each other row contains the unique event id on the left column, and the
	 * result for this event in the next column.
	 * 
	 * @return
	 */
	public String getResultFilePath() {
		return resultFilePath;
	}

	public void setResultFilePath(String resultFilePath) {
		this.resultFilePath = resultFilePath;
	}

	/**
	 * Directory containing all feature files in CSV format. May include
	 * sub-directories. Feature files need to list the feature names in the top
	 * row and unique event id in the left column. The remaining cells give
	 * feature values. If a feature value can be interpreted as a number, it
	 * will be assumed to be a weight.
	 * 
	 * @return
	 */
	public String getFeatureDirPath() {
		return featureDirPath;
	}

	public void setFeatureDirPath(String featureDirPath) {
		this.featureDirPath = featureDirPath;
	}

	/**
	 * For each block of 10 rows in the results file, the index of the row which
	 * should be considered as test.
	 * 
	 * @return
	 */
	public int getTestSegment() {
		return testSegment;
	}

	public void setTestSegment(int testSegment) {
		this.testSegment = testSegment;
	}

	/**
	 * An alternative directory containing .csv and .zip files.
	 * If  scaleNormalised is true, files in this directory will be normalised as a whole
	 * rather than being normalised feature by feature.
	 * Note that all CSVs in a single zip file will be normalised as a single group.
	 * @return
	 */
	public String getGroupedFeatureDirPath() {
		return groupedFeatureDirPath;
	}

	public void setGroupedFeatureDirPath(String groupedFeatureDirPath) {
		this.groupedFeatureDirPath = groupedFeatureDirPath;
	}

	public Set<String> getFeatures() {
		return features;
	}
	
	/**
	 * A map of feature name to file name for any features that were grouped together
	 * where normalising.
	 * @return
	 */
	public Map<String, String> getGroupedFeatures() {
		return groupedFeatures;
	}

	/**
	 * A map of file name to a set of feature names for all files.
	 * @return
	 */
	public Map<String, Set<String>> getFeaturesPerFile() {
		return featuresPerFile;
	}
	
	public float getMax(String featureName) {
		String fileName = this.groupedFeatures.get(featureName);
		float maxValue = 0;
		if (fileName!=null) {
			// in this case, this feature was grouped with other features into a file
			// that needs to be normalised as a whole
			Float maxValueObj = this.fileMaxValues.get(fileName);
			if (maxValueObj==null) {
				maxValue = 0;
				for (String feature : this.featuresPerFile.get(fileName)) {
					float featureMax = this.featureStatsMap.get(feature).max;
					if (featureMax>maxValue)
						maxValue = featureMax;
				}
				this.fileMaxValues.put(fileName, maxValue);
			} else {
				maxValue = maxValueObj.floatValue();
			}
		} else {
			// this feature is normalised on its own
			maxValue = this.featureStatsMap.get(featureName).max;
		}
		return maxValue;
	}
	
	public float getMean(String featureName) {
		String fileName = this.groupedFeatures.get(featureName);
		float meanValue = 0;
		if (fileName!=null) {
			// in this case, this feature was grouped with other features into a file
			// that needs to be normalised as a whole
			Float meanValueObj = this.fileMeanValues.get(fileName);
			if (meanValueObj==null) {
				float totalValue = 0;
				int totalCount = 0;
				for (String feature : this.featuresPerFile.get(fileName)) {
					totalValue += this.featureStatsMap.get(feature).total;
					totalCount += this.featureStatsMap.get(feature).count;
				}
				meanValue = totalValue / (float) totalCount;
				this.fileMeanValues.put(fileName, meanValue);
			} else {
				meanValue = meanValueObj.floatValue();
			}
		} else {
			// this feature is normalised on its own
			float totalValue = this.featureStatsMap.get(featureName).total;
			int totalCount = this.featureStatsMap.get(featureName).count;
			meanValue = totalValue / (float) totalCount;
		}
		return meanValue;		
	}

	/**
	 * The events found by this reader.
	 * @return
	 */
	public GenericEvents getEvents() {
		if (this.events == null) {
			this.events = new GenericEvents(eventMap.values());
		}
		return this.events;
	}
	
	
	public Map<String, GenericEvents> getEventsPerFile() {
		if (eventsPerFile == null) {
			eventsPerFile = new TreeMap<String, GenericEvents>();
			for (Entry<String, Map<String,GenericEvent>> entry : eventFileMap.entrySet()) {
				eventsPerFile.put(entry.getKey(), new GenericEvents(entry.getValue().values()));
			}
		}
		return eventsPerFile;
	}

	/**
	 * Which files are were in the grouped normalisation directory?
	 * @return
	 */
	public Set<String> getGroupedFiles() {
		return groupedFiles;
	}

	private static final class FeatureStats {
		public float max;
		public float total;
		public int count;
	}

	public TrainingSetType getTrainingSetType() {
		return trainingSetType;
	}

	public void setTrainingSetType(TrainingSetType trainingSetType) {
		this.trainingSetType = trainingSetType;
	}


	/**
	 * Any event with an outcome in excludedOutcomes will be completely ignored.
	 * @return
	 */	public Collection<String> getExcludedOutcomes() {
		return excludedOutcomes;
	}

	public void setExcludedOutcomes(Collection<String> excludedOutcomes) {
		this.excludedOutcomes = excludedOutcomes;
	}

	/**
	 * If true, events will be split out for each file.
	 * @return
	 */
	public boolean isSplitEventsByFile() {
		return splitEventsByFile;
	}

	public void setSplitEventsByFile(boolean splitEventsByFile) {
		this.splitEventsByFile = splitEventsByFile;
	}


	
	
}
