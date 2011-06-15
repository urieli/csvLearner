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

/**
 * Observes the Maxent analysis process, and can respond to various events.
 * @author Assaf Urieli
 *
 */
public interface MaxentObserver {
	/**
	 * Called after maxent analysis.
	 * @param event the event analysed
	 * @param outcomes  an ordered collection of weighted outcomes (from highest weight to lowest)
	 */
	public void onAnalyse(GenericEvent event, Collection<NameValuePair> outcomes);
	
	/**
	 * Called when analysis is complete.
	 */
	public void onTerminate();
}
