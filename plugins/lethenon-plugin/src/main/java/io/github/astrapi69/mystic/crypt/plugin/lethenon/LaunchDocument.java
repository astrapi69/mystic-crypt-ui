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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import java.util.Locale;
import java.util.function.BinaryOperator;

/**
 * The five launch documents of lethenon#141, which the "Before you start" view links to (#540).
 * They live in lethenon's repository under {@code docs/launch/} and are read on its develop
 * branch, where they are re-checked before the main chain starts (lethenon#144); this view only
 * points to them.
 */
public enum LaunchDocument
{

	/** What MiCA, the AMLR and Dubai's regulators say, each statement with its source */
	REGULATORY_OVERVIEW("regulatory-overview.md", "Regulatory overview",
		"btnOpenRegulatoryOverview"),

	/** What needs an authorisation and what does not, and the main chain's prerequisites */
	LAUNCH_CHECKLIST("launch-checklist.md", "Launch checklist", "btnOpenLaunchChecklist"),

	/** What may be said about lethenon in public, each statement with its evidence */
	MESSAGING_GUIDE("messaging-guide.md", "Messaging guide", "btnOpenMessagingGuide"),

	/** The technical description, deliberately not called a white paper */
	SPECIFICATION("specification.md", "Specification", "btnOpenSpecification"),

	/** What a start needs in servers, operation, data protection and money */
	INFRASTRUCTURE("infrastructure.md", "Infrastructure", "btnOpenInfrastructure");

	/** Where the documents are read: lethenon's develop branch on GitHub */
	public static final String LOCATION = "https://github.com/astrapi69/lethenon/blob/develop/docs/launch/";

	private final String fileName;

	private final String defaultTitle;

	private final String componentName;

	LaunchDocument(final String fileName, final String defaultTitle, final String componentName)
	{
		this.fileName = fileName;
		this.defaultTitle = defaultTitle;
		this.componentName = componentName;
	}

	/**
	 * Gets the address the document is read at
	 * @return the document's address on lethenon's develop branch
	 */
	public String url()
	{
		return LOCATION + fileName;
	}

	/**
	 * Gets the document's title in the language of the given text
	 * @param text
	 *            looks up a key, falling back to the default it is given
	 * @return the title
	 */
	public String title(final BinaryOperator<String> text)
	{
		String key = name().toLowerCase(Locale.ROOT).replace('_', '.');
		return text.apply(LaunchChecklist.KEY_PREFIX + "document." + key, defaultTitle);
	}

	/**
	 * Gets the name of the button that opens the document, for an end-to-end test to find it by
	 * @return the component name
	 */
	public String componentName()
	{
		return componentName;
	}
}
