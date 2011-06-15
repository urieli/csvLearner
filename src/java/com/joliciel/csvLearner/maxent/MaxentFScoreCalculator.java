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
package com.joliciel.csvLearner.maxent;

import java.util.Collection;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.NameValuePair;
import com.joliciel.csvLearner.utils.FScoreCalculator;

/**
 * On the assumption that all events already have an outcome assigned,
 * calculates the f-score per outcome based on the most probable outcome in the list of guesses.
 * @author Assaf Urieli
 *
 */
public class MaxentFScoreCalculator implements MaxentObserver {
	FScoreCalculator<String> fscoreCalculator = new FScoreCalculator<String>();
	private double minProbToConsider = 0.0;
	private String unknownOutcomeName = "";
	
	@Override
	public void onAnalyse(GenericEvent event,
			Collection<NameValuePair> outcomes) {
		NameValuePair bestOutcome = outcomes.iterator().next();
		String outcome = bestOutcome.getName();
		if (bestOutcome.getValue() < minProbToConsider)
			outcome = unknownOutcomeName;
		fscoreCalculator.increment(event.getOutcome(), outcome);
	}
	
	@Override
	public void onTerminate() {
		// nothing to do here
	}

	public FScoreCalculator<String> getFscoreCalculator() {
		return fscoreCalculator;
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
