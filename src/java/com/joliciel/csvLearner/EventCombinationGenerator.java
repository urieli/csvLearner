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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.utils.CSVFormatter;
import com.joliciel.csvLearner.utils.LogUtils;

/**
 * Generates a random combination of events.
 * @author Assaf Urieli
 *
 */
public class EventCombinationGenerator {
	private static final Log LOG = LogFactory.getLog(EventCombinationGenerator.class);
	private Map<String,Integer> desiredCountPerOutcome;
	private String resultFilePath = null;
	
	private Map<String,List<String>> eventsPerOutcome = new TreeMap<String, List<String>>();
	private Map<String,String> combination;
	
	public EventCombinationGenerator() {
		
	}
	
	public EventCombinationGenerator(
			Map<String, Integer> desiredCountPerOutcome, String resultFilePath) {
		super();
		this.desiredCountPerOutcome = desiredCountPerOutcome;
		this.resultFilePath = resultFilePath;
	}
	
	/**
	 * The combination returned will:
	 * a) respect the desired counts in this.getDesiredCountPerOutcome().
	 * b) be ordered one outcome at a time.
	 * @return
	 */
	public Map<String, String> getCombination() {
		if (this.combination==null) {
			this.combination = new LinkedHashMap<String, String>();
			this.scanResultsFile();
			Random random = new Random(new Date().getTime());
			for (Entry<String,Integer> desiredCount : desiredCountPerOutcome.entrySet()) {
				String outcome = desiredCount.getKey();
				int count = desiredCount.getValue();
				List<String> events = eventsPerOutcome.get(outcome);
				if (events==null||events.size()<count) {
					throw new RuntimeException("Not enough events for outcome " + outcome + " to generate a combination with " + count + ".");
				}
				Set<Integer> usedUp = new TreeSet<Integer>();
				for (int i=0;i<count;i++) {
					int index = random.nextInt(events.size());
					while (usedUp.contains(index))
						index = random.nextInt(events.size());
					usedUp.add(index);
					
					String ref = events.get(index);
					this.combination.put(ref, outcome);
				} // next randomly selected event
			} // next outcome
		}
		return combination;
	}

	void scanResultsFile() {
		Scanner resultScanner;
		try {
			resultScanner = new Scanner(new FileInputStream(
					resultFilePath), "UTF-8");

			try {
				boolean firstLine = true;
				while (resultScanner.hasNextLine()) {
					String line = resultScanner.nextLine();
					if (!firstLine) {
						List<String> cells = CSVFormatter.getCSVCells(line);
						String ref = cells.get(0);
						String outcome = cells.get(1);
						List<String> eventsForThisOutcome = this.eventsPerOutcome.get(outcome);
						if (eventsForThisOutcome==null) {
							eventsForThisOutcome = new ArrayList<String>();
							this.eventsPerOutcome.put(outcome, eventsForThisOutcome);
						}
						eventsForThisOutcome.add(ref);
					}
					firstLine = false;
	
				}
			} finally {
				resultScanner.close();
			}
		} catch (IOException e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
	}
	
	public Map<String, Integer> getDesiredCountPerOutcome() {
		return desiredCountPerOutcome;
	}

	public String getResultFilePath() {
		return resultFilePath;
	}
	
	public void setDesiredCountPerOutcome(
			Map<String, Integer> desiredCountPerOutcome) {
		this.desiredCountPerOutcome = desiredCountPerOutcome;
	}

	public void setResultFilePath(String resultFilePath) {
		this.resultFilePath = resultFilePath;
	}

	public void readDesiredCounts(File file) {
		try {
			this.desiredCountPerOutcome = new LinkedHashMap<String, Integer>();
			Scanner scanner = new Scanner(new FileInputStream(
					file), "UTF-8");
			try {
				while (scanner.hasNextLine()) {
					String line = scanner.nextLine();
					List<String> cells = CSVFormatter.getCSVCells(line);
					String outcome = cells.get(0);
					int count = Integer.parseInt(cells.get(1));
					this.desiredCountPerOutcome.put(outcome, count);
				}
			} finally {
				scanner.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);			
		}
	}
	
	public void writeCombination(File file) {
		try {
			file.delete();
			file.createNewFile();
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
			this.writeCombination(writer);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		
	}
	
	public void writeCombination(Writer writer) {
		try {
			try {
				writer.write("ID,outcome,\n");
				for (Entry<String,String> entry : this.combination.entrySet()) {
					writer.write(CSVFormatter.format(entry.getKey()) + "," + CSVFormatter.format(entry.getValue()) +",\n");
				}
			} finally {
				writer.flush();
				writer.close();
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
}
