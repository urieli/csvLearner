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
package com.joliciel.csvLearner.maxent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.NameValuePair;

/**
 * Writes an XML file including a list of events and an outcome for each event.
 * @author Assaf Urieli
 *
 */
public class MaxentOutcomeXmlWriter implements MaxentObserver {
	private Writer writer;
	private double minProbToConsider = 0.0;
	private String unknownOutcomeName = "";

	public MaxentOutcomeXmlWriter(File file) {

		try {
			file.delete();
			file.createNewFile();
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false),"UTF8"));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		this.initialise();
	}
	public MaxentOutcomeXmlWriter(Writer outcomeFileWriter) {
		this.writer = outcomeFileWriter;
		this.initialise();
	}
	
	private void initialise() {
		try {
			writer.append("<results>\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onAnalyse(GenericEvent event,
			Collection<NameValuePair> outcomes) {
		NameValuePair bestOutcome =  outcomes.iterator().next();
		String outcome = bestOutcome.getName();
		if (bestOutcome.getValue() < minProbToConsider)
			outcome = unknownOutcomeName;
		
		try {
			writer.append("<event id=\"" + event.getIdentifier() + "\">\n");
			writer.append("  <outcome name=\"" + outcome + "\"/>\n");
			writer.append("</event>\n");
			writer.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}
	
	@Override
	public void onTerminate() {
		try {
			this.writer.append("</results>\n");
			this.writer.flush();
			this.writer.close();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}			
	}
	public double getMinProbToConsider() {
		return minProbToConsider;
	}
	public void setMinProbToConsider(double minProbToConsider) {
		this.minProbToConsider = minProbToConsider;
	}
	public String getUnknownOutcomeName() {
		return unknownOutcomeName;
	}
	public void setUnknownOutcomeName(String unknownOutcomeName) {
		this.unknownOutcomeName = unknownOutcomeName;
	}

}
