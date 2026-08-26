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
package io.github.astrapi69.mystic.crypt.plugin.conversion;

/**
 * The state the {@link ConversionPanel} holds: the file to convert, the file to write to, what the
 * chosen file was found to hold, the message shown after a conversion, and the kind the chosen file
 * was detected as - the kind decides which conversions the panel offers.
 */
public class ConversionPanelModel
{

	/**
	 * What a label carries while there is nothing to say - a blank keeps the row at its height
	 * where an empty string would collapse it
	 */
	public static final String NOTHING_TO_SAY = " ";

	private String sourceFilePath = "";

	private String targetFilePath = "";

	private String whatItHolds = NOTHING_TO_SAY;

	private String resultMessage = NOTHING_TO_SAY;

	private ConversionSupport.FileKind fileKind;

	/**
	 * Gets the path of the file to convert
	 *
	 * @return the path of the file to convert
	 */
	public String getSourceFilePath()
	{
		return sourceFilePath;
	}

	/**
	 * Sets the path of the file to convert
	 *
	 * @param sourceFilePath
	 *            the path of the file to convert
	 */
	public void setSourceFilePath(final String sourceFilePath)
	{
		this.sourceFilePath = sourceFilePath;
	}

	/**
	 * Gets the path of the file the conversion is written to
	 *
	 * @return the path of the file the conversion is written to
	 */
	public String getTargetFilePath()
	{
		return targetFilePath;
	}

	/**
	 * Sets the path of the file the conversion is written to
	 *
	 * @param targetFilePath
	 *            the path of the file the conversion is written to
	 */
	public void setTargetFilePath(final String targetFilePath)
	{
		this.targetFilePath = targetFilePath;
	}

	/**
	 * Gets the description of what the chosen file was found to hold
	 *
	 * @return the description of what the chosen file was found to hold
	 */
	public String getWhatItHolds()
	{
		return whatItHolds;
	}

	/**
	 * Sets the description of what the chosen file was found to hold
	 *
	 * @param whatItHolds
	 *            the description of what the chosen file was found to hold
	 */
	public void setWhatItHolds(final String whatItHolds)
	{
		this.whatItHolds = whatItHolds;
	}

	/**
	 * Gets the message shown after a conversion or a failed read
	 *
	 * @return the message shown after a conversion or a failed read
	 */
	public String getResultMessage()
	{
		return resultMessage;
	}

	/**
	 * Sets the message shown after a conversion or a failed read
	 *
	 * @param resultMessage
	 *            the message shown after a conversion or a failed read
	 */
	public void setResultMessage(final String resultMessage)
	{
		this.resultMessage = resultMessage;
	}

	/**
	 * Gets the kind the chosen file was detected as, or null while no file could be read
	 *
	 * @return the kind the chosen file was detected as, or null while no file could be read
	 */
	public ConversionSupport.FileKind getFileKind()
	{
		return fileKind;
	}

	/**
	 * Sets the kind the chosen file was detected as, null while no file could be read
	 *
	 * @param fileKind
	 *            the kind the chosen file was detected as, null while no file could be read
	 */
	public void setFileKind(final ConversionSupport.FileKind fileKind)
	{
		this.fileKind = fileKind;
	}
}
