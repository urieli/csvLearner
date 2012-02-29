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

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;
import com.joliciel.csvLearner.NameValueDescendingComparator;
import com.joliciel.csvLearner.NameValuePair;

import opennlp.model.MaxentModel;

/**
 * A Maxent analysis class which analyses a collection of events.
 * @author Assaf Urieli
 *
 */
public class MaxentAnalyser {
    private static final Log LOG = LogFactory.getLog(MaxentAnalyser.class);
	private MaxentModel maxentModel;
	private List<MaxentObserver> observers = new ArrayList<MaxentObserver>();
	private String preferredOutcome = null;
	private double bias = 0.0;
	
	/**
	 * Analyse any event where event.isTest(),
	 * and call appropriate events on the MaxentObservers added.
	 * @param events
	 */
	public void analyse(GenericEvents events) {
		try {
			for (GenericEvent event : events) {
				if (event.isTest()) {
					LOG.trace("Test event: " + event.getIdentifier());
					String[] contexts = new String[event.getFeatures().size()];
					int i = 0;
					for (String context : event.getFeatures()) {
						contexts[i++] = context;
					}
					float[] weights = new float[event.getWeights().size()];
					i = 0;
					for (Float weight : event.getWeights()) {
						weights[i++]  = weight;
					}
					
					double[] probs = maxentModel.eval(contexts, weights);
					String[] outcomes = new String[probs.length];
					for (i=0;i<probs.length;i++)
						outcomes[i]=maxentModel.getOutcome(i);
					
					Collection<NameValuePair> heap = new TreeSet<NameValuePair>(new NameValueDescendingComparator());
					for (i=0;i<probs.length;i++) {
						String outcome = outcomes[i];
						double prob = probs[i];
						if (this.preferredOutcome!=null && this.bias>0) {
							if (this.preferredOutcome.equals(outcome)) {
								prob = (prob + bias) / (1 + bias);
							} else {
								prob = (prob) / (1 + bias);
							}
						}
						NameValuePair weightedOutcome = new NameValuePair(outcome, prob);
						heap.add(weightedOutcome);
					}
					
					for (MaxentObserver observer : observers) {
						observer.onAnalyse(event, heap);
					}
					
					if (LOG.isTraceEnabled()) {
						NameValuePair bestOutcome = heap.iterator().next();
						LOG.trace("Event " + event.getIdentifier());
						LOG.trace("Real outcome: " + event.getOutcome());
						LOG.trace("Guessed outcome: " + bestOutcome.getName());
						for (i=0;i<contexts.length;i++) {
							LOG.trace(contexts[i] + ": " + weights[i]);
						}
						
						LOG.trace("Outcome list: ");
						for (NameValuePair weightedOutcome : heap) {
							LOG.trace("Outcome: " + weightedOutcome.getName() + ", Weight: " + weightedOutcome.getValue());
						}
					}
				}
			}
		} finally {
			for (MaxentObserver observer : this.observers) {
				observer.onTerminate();
			}
		}
	}

	/**
	 * Allows us to specify that one outcome is preferred to the others.
	 * @return
	 */
	public String getPreferredOutcome() {
		return preferredOutcome;
	}


	public void setPreferredOutcome(String preferredOutcome) {
		this.preferredOutcome = preferredOutcome;
	}

	/**
	 * Bias to assign to the preferred outcome.
	 * If the bias is 0.05, then the probability for the preferred outcome will be (p+0.05)/1.05
	 * while, the probability for all other outcomes will be p/1.05.
	 * @return
	 */
	public double getBias() {
		return bias;
	}


	public void setBias(double bias) {
		this.bias = bias;
	}


	public MaxentModel getMaxentModel() {
		return maxentModel;
	}

	public void setMaxentModel(MaxentModel maxentModel) {
		this.maxentModel = maxentModel;
	}
	
	public void addObserver(MaxentObserver observer) {
		this.observers.add(observer);
	}
}
