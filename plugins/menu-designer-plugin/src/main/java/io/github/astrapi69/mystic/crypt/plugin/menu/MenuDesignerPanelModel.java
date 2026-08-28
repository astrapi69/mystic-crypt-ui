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
package io.github.astrapi69.mystic.crypt.plugin.menu;

/**
 * Everything the {@link MenuDesignerPanel} holds: the menu xml that is being edited and the message
 * the last operation left.
 * <p>
 * The editor is bound to this object, so the xml the user typed, pasted or exported is readable
 * here at any moment - "Validate", "Apply" and "Save" take it from the model instead of asking the
 * editor widget for its content.
 */
public class MenuDesignerPanelModel
{

	/** The menu xml that is being edited, empty as long as nothing was exported or typed */
	private String menuXml = "";

	/** The message the last operation left, empty before the first one */
	private String resultMessage = "";

	/**
	 * Gets the menu xml that is being edited
	 *
	 * @return the menu xml that is being edited
	 */
	public String getMenuXml()
	{
		return menuXml;
	}

	/**
	 * Sets the menu xml that is being edited
	 *
	 * @param menuXml
	 *            the menu xml that is being edited
	 */
	public void setMenuXml(final String menuXml)
	{
		this.menuXml = menuXml;
	}

	/**
	 * Gets the message the last operation left
	 *
	 * @return the message of the last operation
	 */
	public String getResultMessage()
	{
		return resultMessage;
	}

	/**
	 * Sets the message the last operation left
	 *
	 * @param resultMessage
	 *            the message of the last operation
	 */
	public void setResultMessage(final String resultMessage)
	{
		this.resultMessage = resultMessage;
	}
}
