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

import java.util.Comparator;

/**
 * Orders Name-Value pairs from highest value to lowest value.
 * @author Assaf
 *
 */
public final class NameValueDescendingComparator implements Comparator<NameValuePair> {

	@Override
	public int compare(NameValuePair o1, NameValuePair o2) {
		if (o1.getValue()<o2.getValue()) {
			return 1;
		} else if (o1.getValue()>o2.getValue()) {
			return -1;
		} else {
			int nameCompare = o1.getName().compareTo(o2.getName());
			if (nameCompare!=0) return nameCompare;
			return o1.hashCode()-o2.hashCode();
		}
	}

}
