///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2011 Assaf Urieli
//
//This file is part of csvLearner.
//
//csvLearner is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//csvLearner is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with csvLearner.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.csvLearner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.CSVFormatter;

/**
 * Given a filename and a collection of generic events,
 * writes them to CSV files readable by this application for training or analysis.
 * @author Assaf Urieli
 *
 */
public class CSVEventListWriter {
	private static final Log LOG = LogFactory.getLog(CSVEventListWriter.class);
	boolean isZip = false;
	boolean filePerEvent = false;
	boolean includeOutcomes = false;
	String missingValueString="";
	String identifierPrefix = "";
	boolean denominalise = false;
	
	File file = null;
	
	public CSVEventListWriter(File file) {
		this.file = file;
	}
	
	public void writeFile(GenericEvents events) {
		try {
			LOG.debug("writeFile: " + file.getName());
			file.delete();
			file.createNewFile();
			if (file.getName().endsWith(".zip"))
				isZip = true;
			Writer writer = null;
			ZipOutputStream zos = null;
			try {
				if (isZip) {
					zos = new ZipOutputStream(new FileOutputStream(file,false));
					writer = new BufferedWriter(new OutputStreamWriter(zos));
				} else {
					writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
				}
				Set<String> features = new TreeSet<String>();
				if (!filePerEvent) {
					if (isZip) {
						zos.putNextEntry(new ZipEntry(file.getName().substring(0,file.getName().lastIndexOf('.'))+".csv"));
					}

					for (GenericEvent event: events) {
						if (LOG.isTraceEnabled())
							LOG.trace("Writing event: " + event.getIdentifier());
						for (String feature : event.getFeatures()) {
							int classIndex = feature.indexOf(CSVLearner.NOMINAL_MARKER);
							if (classIndex<0||denominalise)
								features.add(feature);
							else
								features.add(feature.substring(0, classIndex));
						}
					}

					writer.append("ID,");
	
					if (includeOutcomes)
						writer.append("outcome,");
					for (String feature : features) {
						writer.append(CSVFormatter.format(feature)+",");
					}
					
					writer.append("\n");
					writer.flush();
				}
				
				for (GenericEvent event : events) {
					if (filePerEvent) {
						features = new TreeSet<String>();
						for (String feature : event.getFeatures()) {
							int classIndex = feature.indexOf(CSVLearner.NOMINAL_MARKER);
							if (classIndex<0||denominalise)
								features.add(feature);
							else
								features.add(feature.substring(0, classIndex));
						}
						
						if (isZip)
							zos.putNextEntry(new ZipEntry(event.getIdentifier()+".csv"));
						writer.append("ID,");
						if (includeOutcomes)
							writer.append("outcome,");
						
						for (String feature : features) {
							writer.append(CSVFormatter.format(feature)+",");
						}
						
						writer.append("\n");
						writer.flush();
					}
					writer.append(CSVFormatter.format(identifierPrefix + event.getIdentifier())+",");
					if (includeOutcomes)
						writer.append(CSVFormatter.format(event.getOutcome())+ ",");
					
					for (String feature : features) {
						Integer featureIndexObj = event.getFeatureIndex(feature);
						int featureIndex = featureIndexObj==null ? -1 : featureIndexObj.intValue();

						if (featureIndex<0) {
							writer.append(missingValueString + ",");
						} else {
							String eventFeature = event.getFeatures().get(featureIndex);
							if (!eventFeature.equals(feature)) {
								int classIndex = eventFeature.indexOf(CSVLearner.NOMINAL_MARKER);
								String clazz = eventFeature.substring(classIndex+CSVLearner.NOMINAL_MARKER.length());
								writer.append(CSVFormatter.format(clazz)+",");
							} else {
								double value = event.getWeights().get(featureIndex);
								writer.append(CSVFormatter.format(value)+",");
							}
						}
					}
					writer.append("\n");
					writer.flush();
					if (filePerEvent && isZip)
						zos.closeEntry();
				}
				if (!filePerEvent && isZip)
					zos.closeEntry();
			} finally {
				if (zos!=null) {
					zos.flush();
					zos.close();
				}
				if (writer!=null) {
					writer.flush();
					writer.close();
				}
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	/**
	 * If true, when writing to a file with an extension of ".zip", will create a separate
	 * csv file zip-entry for each event, instead of one single csv file zip-entry for all the events.
	 * @return
	 */
	public boolean isFilePerEvent() {
		return filePerEvent;
	}

	public void setFilePerEvent(boolean filePerEvent) {
		this.filePerEvent = filePerEvent;
	}

	/**
	 * The string to use when a particular feature is missing.
	 * The default is the empty string, though you might want to use "0" if numeric values are required.
	 * @return
	 */
	public String getMissingValueString() {
		return missingValueString;
	}

	public void setMissingValueString(String missingValueCharacter) {
		this.missingValueString = missingValueCharacter;
	}

	/**
	 * Should we write the outcomes to the file.
	 * @return
	 */
	public boolean isIncludeOutcomes() {
		return includeOutcomes;
	}

	public void setIncludeOutcomes(boolean includeOutcomes) {
		this.includeOutcomes = includeOutcomes;
	}

	/**
	 * A prefix to add to the identifiers (in the case when we're combining different datasets
	 * which share identifiers for distinct events).
	 * @return
	 */
	public String getIdentifierPrefix() {
		return identifierPrefix;
	}

	public void setIdentifierPrefix(String identifierPrefix) {
		this.identifierPrefix = identifierPrefix;
	}

	/**
	 * Whether or not nominal features should be converted to numeric features
	 * when writing.
	 * @return
	 */
	public boolean isDenominalise() {
		return denominalise;
	}

	public void setDenominalise(boolean denominalise) {
		this.denominalise = denominalise;
	}
	
	
}
