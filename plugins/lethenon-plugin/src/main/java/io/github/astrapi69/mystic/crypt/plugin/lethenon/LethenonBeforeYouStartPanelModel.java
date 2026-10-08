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

import java.util.List;

/**
 * The state of {@link LethenonBeforeYouStartPanel}, in one object rather than in its widgets
 * (architecture.md: every panel holds its state in a model). The view only shows, so its state is
 * what it shows: the rows of the short checklist and the documents it links to.
 */
public class LethenonBeforeYouStartPanelModel
{

	private final List<LaunchCheck> checks;

	private final List<LaunchDocument> documents;

	/**
	 * Instantiates the model with the given rows and documents
	 * @param checks
	 *            the rows of the short checklist
	 * @param documents
	 *            the documents the view links to
	 */
	public LethenonBeforeYouStartPanelModel(final List<LaunchCheck> checks,
		final List<LaunchDocument> documents)
	{
		this.checks = List.copyOf(checks);
		this.documents = List.copyOf(documents);
	}

	/**
	 * The rows the table shows
	 * @return the rows, never null
	 */
	public List<LaunchCheck> getChecks()
	{
		return checks;
	}

	/**
	 * The documents the view has a button for
	 * @return the documents, never null
	 */
	public List<LaunchDocument> getDocuments()
	{
		return documents;
	}
}
