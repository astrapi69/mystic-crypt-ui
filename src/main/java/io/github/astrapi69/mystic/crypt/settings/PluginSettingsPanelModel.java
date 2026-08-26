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
package io.github.astrapi69.mystic.crypt.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The state of the {@link PluginSettingsPanel}: which plugin the user selected, the settings of
 * that plugin as they are currently shown in the table and the message shown below the table.
 * <p>
 * The panel keeps this object up to date while the user works, so what "Apply" would store is
 * readable at any moment without asking a component for it.
 */
public class PluginSettingsPanelModel
{

	/** The row of the selected plugin in the list, -1 when nothing is selected */
	private int selectedPluginIndex = -1;

	/** The display name of the selected plugin, the value the list component binds */
	private String selectedPluginName;

	/** The settings of the selected plugin, keyed by setting name, in the shown order */
	private Map<String, String> shownValues = new LinkedHashMap<>();

	/** The message shown below the table */
	private String resultText = " ";

	/**
	 * Gets the row of the selected plugin in the list
	 *
	 * @return the selected row, -1 when nothing is selected
	 */
	public int getSelectedPluginIndex()
	{
		return selectedPluginIndex;
	}

	/**
	 * Sets the row of the selected plugin in the list
	 *
	 * @param selectedPluginIndex
	 *            the selected row, -1 when nothing is selected
	 */
	public void setSelectedPluginIndex(int selectedPluginIndex)
	{
		this.selectedPluginIndex = selectedPluginIndex;
	}

	/**
	 * Gets the display name of the selected plugin
	 *
	 * @return the display name, {@code null} when nothing is selected
	 */
	public String getSelectedPluginName()
	{
		return selectedPluginName;
	}

	/**
	 * Sets the display name of the selected plugin
	 *
	 * @param selectedPluginName
	 *            the display name, {@code null} when nothing is selected
	 */
	public void setSelectedPluginName(String selectedPluginName)
	{
		this.selectedPluginName = selectedPluginName;
	}

	/**
	 * Gets the settings as they are currently shown in the table
	 *
	 * @return the shown settings, keyed by setting name
	 */
	public Map<String, String> getShownValues()
	{
		return shownValues;
	}

	/**
	 * Sets the settings as they are currently shown in the table
	 *
	 * @param shownValues
	 *            the shown settings, keyed by setting name
	 */
	public void setShownValues(Map<String, String> shownValues)
	{
		this.shownValues = shownValues != null ? shownValues : new LinkedHashMap<>();
	}

	/**
	 * Gets the message shown below the table
	 *
	 * @return the message
	 */
	public String getResultText()
	{
		return resultText;
	}

	/**
	 * Sets the message shown below the table
	 *
	 * @param resultText
	 *            the message
	 */
	public void setResultText(String resultText)
	{
		this.resultText = resultText;
	}
}
