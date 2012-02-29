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
package com.joliciel.csvLearner.svm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.export.RapidMinerExampleSetLoader;
import com.joliciel.csvLearner.utils.LogUtils;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.functions.kernel.LibSVMModel;

public class SVMPredicter {
	private static final Log LOG = LogFactory.getLog(SVMPredicter.class);
	LibSVMModel model = null;
	
	public SVMPredicter(LibSVMModel model) {
		this.model = model;
	}
	
	public void predict(GenericEvents events) {
		RapidMinerExampleSetLoader exampleSetLoader = new RapidMinerExampleSetLoader();
		ExampleSet testSet = exampleSetLoader.createExampleSet(events, true, events.getOutcomes());
		Attribute predicatedLabel = testSet.getAttributes().getPredictedLabel();

		this.predict(testSet);
		int i = 0;
		for (GenericEvent event : events) {
			if (event.isTest()) {
				Example example = testSet.getExample(i);
				String outcome = predicatedLabel.getMapping().mapIndex((int)example.getPredictedLabel());
				LOG.debug("Predicted outcome: " + outcome);
				LOG.debug("Event outcome: " + event.getOutcome());
				i++;
			}
		}
	}
	
	public void predict(ExampleSet testSet) {
		Attribute predicatedLabel = testSet.getAttributes().getPredictedLabel();
		try {
			model.performPrediction(testSet, predicatedLabel);
		} catch (UserError e) {
			LogUtils.logError(LOG, e);
			throw new RuntimeException(e);
		}
		
	}
}
