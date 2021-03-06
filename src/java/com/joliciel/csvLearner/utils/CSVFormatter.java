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
package com.joliciel.csvLearner.utils;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSVFormatter {
    private static DecimalFormat decFormat;
    private static DecimalFormat intFormat;
    
	private static Pattern csvSeparators = Pattern.compile("[,\"]");
	private enum TokenType {
		COMMA, QUOTE, OTHER
	};
    
    static {
	    decFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	    decFormat.applyPattern("##0.00000000");
	    intFormat = (DecimalFormat) DecimalFormat.getNumberInstance(Locale.US);
	    intFormat.applyPattern("##0");
	    
    }
    public static String format(double number) {
		return decFormat.format(number);
	}
    public static String format(float number) {
		return decFormat.format(number);
	}
    public static String format(int number) {
		return intFormat.format(number);
	}
    
    public static String format(String string) {
    	int quotePos = string.indexOf('"');
    	int commaPos = string.indexOf(',');
		if (quotePos>=0) {
			string = string.replace("\"", "\"\"");
		}
		if (quotePos>=0||commaPos>=0)
			return "\"" + string + "\"";
		else
			return string;
   	
    }
    
	/**
	 * Extract a list of cell contents from a given CSV line.
	 * 
	 * @param csvLine
	 * @return
	 */
	public static List<String> getCSVCells(String csvLine) {
		List<String> cells = new ArrayList<String>();
		Matcher matcher = csvSeparators.matcher(csvLine);
		int currentPos = 0;
		List<String> tokens = new ArrayList<String>();
		while (matcher.find()) {
			if (matcher.start() > currentPos) {
				tokens.add(csvLine.substring(currentPos, matcher.start()));
			}
			tokens.add(csvLine.substring(matcher.start(), matcher.end()));
			currentPos = matcher.end();
		}
		tokens.add(csvLine.substring(currentPos));
		StringBuilder currentCell = new StringBuilder();
		boolean inQuote = false;
		TokenType lastToken = TokenType.OTHER;
		for (String token : tokens) {
			if (token.equals("\"")) {
				inQuote = !inQuote;
				if (lastToken.equals(TokenType.QUOTE)) {
					currentCell.append(token);
					lastToken = TokenType.OTHER;
				} else {
					lastToken = TokenType.QUOTE;
				}
			} else if (token.equals(",")) {
				if (inQuote) {
					currentCell.append(token);
					lastToken = TokenType.OTHER;
				} else {
					cells.add(currentCell.toString().trim());
					currentCell = new StringBuilder();
					lastToken = TokenType.COMMA;
				}
			} else {
				currentCell.append(token);
				lastToken = TokenType.OTHER;
			}
		}
		if (currentCell.length() > 0)
			cells.add(currentCell.toString().trim());
		return cells;
	}
}
