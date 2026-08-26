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
package io.github.astrapi69.mystic.crypt.wizard;

/**
 * The class {@link ExtensionsPanelModel} holds what the entry form of the {@link ExtensionsPanel}
 * currently contains: the kind of extension that was picked by name, the object id of the
 * extension, whether it is critical and the value that was typed for it. This is the state of the
 * form, not the state of the certificate wizard, which the panel keeps in its own wizard model
 */
public class ExtensionsPanelModel
{

	/** The name of the picked extension kind, as it is shown in the chooser */
	private String extensionKind;

	/** The object id of the extension */
	private String extensionId = "";

	/** The flag that tells if the extension is critical */
	private boolean critical;

	/** The value of the extension, in the readable form the user types it */
	private String extensionValue = "";

	/**
	 * Gets the name of the picked extension kind
	 *
	 * @return the name of the picked extension kind
	 */
	public String getExtensionKind()
	{
		return extensionKind;
	}

	/**
	 * Sets the name of the picked extension kind
	 *
	 * @param extensionKind
	 *            the name of the picked extension kind
	 */
	public void setExtensionKind(final String extensionKind)
	{
		this.extensionKind = extensionKind;
	}

	/**
	 * Gets the object id of the extension
	 *
	 * @return the object id of the extension
	 */
	public String getExtensionId()
	{
		return extensionId;
	}

	/**
	 * Sets the object id of the extension
	 *
	 * @param extensionId
	 *            the object id of the extension
	 */
	public void setExtensionId(final String extensionId)
	{
		this.extensionId = extensionId;
	}

	/**
	 * Checks if the extension is critical
	 *
	 * @return true if the extension is critical, otherwise false
	 */
	public boolean isCritical()
	{
		return critical;
	}

	/**
	 * Sets the flag that tells if the extension is critical
	 *
	 * @param critical
	 *            the flag that tells if the extension is critical
	 */
	public void setCritical(final boolean critical)
	{
		this.critical = critical;
	}

	/**
	 * Gets the value of the extension
	 *
	 * @return the value of the extension
	 */
	public String getExtensionValue()
	{
		return extensionValue;
	}

	/**
	 * Sets the value of the extension
	 *
	 * @param extensionValue
	 *            the value of the extension
	 */
	public void setExtensionValue(final String extensionValue)
	{
		this.extensionValue = extensionValue;
	}
}
