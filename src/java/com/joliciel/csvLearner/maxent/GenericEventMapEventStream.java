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

import java.io.IOException;
import java.util.Iterator;

import com.joliciel.csvLearner.GenericEvent;
import com.joliciel.csvLearner.GenericEvents;

import opennlp.model.Event;
import opennlp.model.EventStream;

/**
 * Implementation of event stream that reads from a Map<String,GenericEvent> and 
 * returns any events that aren't marked as test.
 * @author Assaf Urieli
 *
 */
class GenericEventMapEventStream implements EventStream {
	private GenericEvents events = null;
	private Iterator<GenericEvent> eventIterator = null;
	private GenericEvent currentEvent = null;
	
	public GenericEventMapEventStream(GenericEvents events) {
		this.events = events;
		eventIterator = this.events.iterator();
	}
	
	@Override
	public boolean hasNext() throws IOException {
		currentEvent = null;
		while (currentEvent==null && eventIterator.hasNext()) {
			currentEvent = eventIterator.next();
			if (currentEvent.isTest()) {
				currentEvent = null;
			}
		}
		return (currentEvent!=null);
	}

	@Override
	public Event next() throws IOException {
		String[] contexts = new String[currentEvent.getFeatures().size()];
		int i = 0;
		for (String context : currentEvent.getFeatures()) {
			contexts[i++] = context;
		}
		float[] weights = new float[currentEvent.getWeights().size()];
		i = 0;
		for (Float weight : currentEvent.getWeights()) {
			weights[i++]  = weight;
		}
		
		Event event = new Event(currentEvent.getOutcome(), contexts, weights);
		return event;
	}

}
