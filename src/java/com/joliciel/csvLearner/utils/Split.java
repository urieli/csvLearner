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
package com.joliciel.csvLearner.utils;

/**
 * Represents one split in a list of NameValuePairs.
 * @author Assaf Urieli
 *
 */
public class Split implements Comparable<Split> {
	private int start;
	private int end;
	private double entropy;
	
	public Split(int start, int end) {
		this.start = start;
		this.end = end;
	}
	public double getEntropy() {
		return entropy;
	}
	public void setEntropy(double entropy) {
		this.entropy = entropy;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	
	public int getSize() {
		return (end-start)+1;
	}
	
	@Override
	public int compareTo(Split o) {
		if (this.getStart()!=o.getStart())
			return this.getStart() - o.getStart();
		return this.getEnd() - o.getEnd();
	}
	
}
