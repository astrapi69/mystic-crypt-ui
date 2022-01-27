/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.certificate;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CertificateAttributes
{
	String commonName;
	String organisation;
	String organisationUnit;
	String countryCode;
	String state;
	String location;

	public String toRepresentableString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		if (setCertificateValue(stringBuilder, "C", countryCode))
		{
			stringBuilder.append(", ");
		}
		if (setCertificateValue(stringBuilder, "ST", state))
		{
			stringBuilder.append(", ");
		}
		if (setCertificateValue(stringBuilder, "L", location))
		{
			stringBuilder.append(", ");
		}
		if (setCertificateValue(stringBuilder, "O", organisation))
		{
			stringBuilder.append(", ");
		}
		if (setCertificateValue(stringBuilder, "OU", organisationUnit))
		{
			stringBuilder.append(", ");
		}
		if (setCertificateValue(stringBuilder, "CN", commonName))
		{
			stringBuilder.append(", ");
		}
		String result = stringBuilder.toString();
		if (result.endsWith(", "))
		{
			result = result.substring(0, result.lastIndexOf(", "));
		}
		return result;
	}

	private boolean setCertificateValue(StringBuilder stringBuilder, String key,
		String certificateValue)
	{
		if (certificateValue != null && !certificateValue.isEmpty())
		{
			stringBuilder.append(key).append("=").append(certificateValue);
			return true;
		}
		return false;
	}
}
