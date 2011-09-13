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

/**
 * A name/value pair that orders automatically in ascending order.
 * @author Assaf Urieli
 *
 */
public final class NameValuePair implements Comparable<NameValuePair> {
	private String name;
	private double value;
	public NameValuePair(String name, double value) {
		this.name = name;
		this.value = value;
	}
	
	
	public void setName(String name) {
		this.name = name;
	}


	public void setValue(double value) {
		this.value = value;
	}


	public String getName() {
		return name;
	}


	public double getValue() {
		return value;
	}


	@Override
	public int compareTo(NameValuePair o) {
		if (this.getValue()<o.getValue()) {
			return -1;
		} else if (this.getValue()>o.getValue()) {
			return 1;
		} else {
			int nameCompare = this.getName().compareTo(o.getName());
			if (nameCompare!=0) return nameCompare;
			return this.hashCode()-o.hashCode();
		}
	}
	
	
}
