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
package com.joliciel.csvLearner.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.CSVFormatter;

/**
 * Reads a single file or all files from a given directory into a map of normalisation limits.
 * @author Assaf Urieli
 *
 */
public class NormalisationLimitReader {
	private static final Log LOG = LogFactory.getLog(NormalisationLimitReader.class);
	private File file;
	private InputStream inputStream;

	public NormalisationLimitReader(File file) {
		this.file = file;
	}
	public NormalisationLimitReader(InputStream inputStream) {
		this.inputStream = inputStream;
	}
	public Map<String,Float> read() {
		Map<String, Float> featureToMaxMap = new TreeMap<String, Float>();
		try {
			if (inputStream!=null) {
				this.readCSVFile(inputStream, featureToMaxMap);
			} else if (file.isDirectory()) {
				Stack<File> directoryStack = new Stack<File>();
				directoryStack.add(file);
				while (!directoryStack.isEmpty()) {
					File directory = directoryStack.pop();
					LOG.debug("Scanning directory: " + directory.getName());
					File[] files = directory.listFiles();
					if (files==null) {
						continue;
					}
					for (File oneFile : files) {
						if (oneFile.isDirectory()) {
							directoryStack.push(oneFile);
						} else if (oneFile.getName().endsWith(".nrm_limits.csv")) {
							LOG.debug("Scanning file : " + oneFile.getName());
							this.readCSVFile(new FileInputStream(oneFile), featureToMaxMap);
						} else {
							LOG.debug("Ignoring : " + oneFile.getName());
						}
					}
	
				}
			} else {
				LOG.debug("Scanning file : " + file.getName());
				this.readCSVFile(new FileInputStream(file), featureToMaxMap);
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		return featureToMaxMap;
	}
	
	private void readCSVFile(InputStream csvInputStream, Map<String, Float> featureToMaxMap) {
		Scanner scanner = new Scanner(csvInputStream,"UTF-8");
		try {
			boolean firstLine = true;
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				if (!firstLine) {
					List<String> cells = CSVFormatter.getCSVCells(line);
					String featureName = cells.get(0);
					float maxValue = Float.parseFloat(cells.get(1));
					featureToMaxMap.put(featureName, maxValue);
				}
				firstLine = false;
			}
		} finally {
			scanner.close();
		}
	}
}
