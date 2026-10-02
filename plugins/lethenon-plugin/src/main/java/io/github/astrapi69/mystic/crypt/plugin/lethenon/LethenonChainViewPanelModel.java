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
 * The state of {@link LethenonChainViewPanel}, in one object rather than scattered across its
 * widgets (architecture.md: every panel holds its state in a model).
 */
public class LethenonChainViewPanelModel
{

	private String chainFile = "";

	private List<ChainBlockRow> rows = List.of();

	private String resultText = " ";

	public String getChainFile()
	{
		return chainFile;
	}

	public void setChainFile(String chainFile)
	{
		this.chainFile = chainFile;
	}

	/**
	 * The blocks shown, only ever those of an accepted chain
	 *
	 * @return the rows, never null
	 */
	public List<ChainBlockRow> getRows()
	{
		return rows;
	}

	public void setRows(List<ChainBlockRow> rows)
	{
		this.rows = List.copyOf(rows);
	}

	public String getResultText()
	{
		return resultText;
	}

	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}
}
