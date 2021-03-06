/*
 * Copyright 2016 IKS Gesellschaft fuer Informations- und Kommunikationssysteme mbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.iksgmbh.sql.pojomemodb.validator.type;

import com.iksgmbh.sql.pojomemodb.validator.TypeValidator;
import com.iksgmbh.sql.pojomemodb.SQLKeyWords;
import com.iksgmbh.sql.pojomemodb.utils.StringParseUtil;

import java.sql.SQLDataException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.iksgmbh.sql.pojomemodb.SQLKeyWords.*;


public class DateTypeValidator extends TypeValidator
{
	private static final ValidatorType VALIDATION_TYPE = ValidatorType.DATE;
	private static final String DATE_IN_MILLIS = "DATE_IN_MILLIS:";
	private static final SimpleDateFormat MYSQL_D_SIMPLEDATEFORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat MYSQL_TS_SIMPLEDATEFORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public void validateValueForType(Object value) throws SQLDataException
    {
        if (value == null) return; // nullable is not checked here
        if (SQLKeyWords.NULL.equalsIgnoreCase(value.toString())) return;

        try {
            if (value instanceof String) {
                convertIntoColumnType( (String) value );
                return;
            }

            if (value instanceof Date) {
                convertIntoColumnType( DATE_IN_MILLIS + ((Date)value).getTime() );
                return;
            }

            throw new SQLDataException("Value '" + value + "' is not valid");

        } catch (SQLDataException e) {
            throw new SQLDataException("Value '" + value + "' is not valid");
        }
    }
	@Override 
	public ValidatorType getType() { 
		return VALIDATION_TYPE; 
	}

	@Override
	public Object convertIntoColumnType(String valueAsString) throws SQLDataException
    {
		if (SYSDATE.equals(valueAsString)
            || CURRENT_TIMESTAMP.equals(valueAsString)
            || GET_DATE.equals(valueAsString)) {
			return new Date();
		}
		
		if (valueAsString.startsWith("{") && valueAsString.endsWith("}")) {
			return convertMySqlDateFormats(valueAsString);
		}
		
		if (valueAsString.startsWith(TO_DATE))			{
			return toDate(valueAsString.substring(TO_DATE.length()));
		}
		
		if (valueAsString.startsWith(DATE_IN_MILLIS)) {
			valueAsString = valueAsString.substring(DATE_IN_MILLIS.length());
			return new Date(Long.valueOf(valueAsString));
		}		
		
		throw new SQLDataException("Insert values '" + valueAsString + "' is no date.");
	}
	
	private Object convertMySqlDateFormats(String valueAsString) throws SQLDataException 
	{

		try {
			valueAsString = valueAsString.substring(1, valueAsString.length()-1).trim();
			
			if (valueAsString.startsWith("d")) {
				valueAsString = valueAsString.substring(1).trim();
				if (valueAsString.startsWith("'") && valueAsString.endsWith("'")) {
					valueAsString = valueAsString.substring(1, valueAsString.length()-1).trim();
					return MYSQL_D_SIMPLEDATEFORMAT.parse(valueAsString);
				}
			}
					
			if (valueAsString.startsWith("ts")) {
				valueAsString = valueAsString.substring(2).trim();
				if (valueAsString.startsWith("'") && valueAsString.endsWith("'")) {
					valueAsString = valueAsString.substring(1, valueAsString.length()-1).trim();
					return MYSQL_TS_SIMPLEDATEFORMAT.parse(valueAsString);
				}
			}
		} catch (Exception pe) {
			// do nothing here, handle exception below
		}
		
		throw new SQLDataException("Insert values '" + valueAsString + "' cannot be parsed into a date.");
	}
	private Date toDate(String dateString) throws SQLDataException 
	{
		try {
			dateString = StringParseUtil.removeSurroundingPrefixAndPostFix(dateString, "(", ")");
			
			final String[] splitResult = dateString.split(",");
			
			if (splitResult.length != 2) {
				throw new SQLDataException("Cannot parse to DateTime: " + dateString);
			}
			
			final String dateValue = StringParseUtil.removeSurroundingPrefixAndPostFix(splitResult[0], "'", "'");
			final String pattern = StringParseUtil.removeSurroundingPrefixAndPostFix(splitResult[1], "'", "'");
			
			return toDate(dateValue, translateFromOracleToJavaLiterals(pattern));
		} catch (Exception e) {
			throw new SQLDataException(e);
		}
	}
	
	private String translateFromOracleToJavaLiterals(String pattern) 
	{
		return pattern.replace('R', 'y')
				      .replace('D', 'd'); 
	}

	private Date toDate(final String value, 
			            final String pattern) throws SQLDataException
	{
		final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		
		try {
			return simpleDateFormat.parse(value);
		} catch (ParseException e) {
			throw new SQLDataException("Cannot convert DateTime: dateAsString=" + value + ", pattern=" + pattern);
		}
	}

	@Override
	public Boolean isValue1SmallerThanValue2(Object value1, Object value2) throws SQLDataException
	{
		if (value1 == null || value2 == null)
			return isValue1SmallerThanValue2ForNullvalues(value1, value2);

		final Date d1 = (Date) value1;
		final Date d2 = (Date) value2;

		int result = d1.compareTo(d2);

		if (result == 0) return null;

		return result == -1;
	}

}