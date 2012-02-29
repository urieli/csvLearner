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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;

/**
 * A collection of GenericEvent
 * @author Assaf Urieli
 *
 */
public class GenericEvents implements Iterable<GenericEvent> {
	private Collection<GenericEvent> events;
	private Set<String> features;
	private Set<String> outcomes;
	private int size = 0;
	public GenericEvents() {
		this.events = new ArrayList<GenericEvent>();
		this.size = 0;
	}
	public GenericEvents(Collection<GenericEvent> events) {
		this.events = events;
		this.size = events.size();
	}
	@Override
	public Iterator<GenericEvent> iterator() {
		return events.iterator();
	}
	
	/**
	 * Returns an ordered set of features.
	 * @return
	 */
	public Set<String> getFeatures() {
		if (features==null) {
			features = new TreeSet<String>();
			for (GenericEvent event : events)
				for (String feature : event.getFeatures())
					features.add(feature);
		}
		return features;
	}
	
	/**
	 * Returns an ordered set of outcomes.
	 * @return
	 */
	public Set<String> getOutcomes() {
		if (outcomes==null) {
			outcomes = new TreeSet<String>();
			for (GenericEvent event : events)
				outcomes.add(event.getOutcome());
		}
		return outcomes;
	}
	
	public int size() {
		return this.size;
	}
	
	public void addAll(Collection<GenericEvent> eventsToAdd) {
		this.size += eventsToAdd.size();
		this.events.addAll(eventsToAdd);
		this.outcomes = null;
		this.features = null;
	}
	public Collection<GenericEvent> getEvents() {
		return events;
	}
	
	
}
