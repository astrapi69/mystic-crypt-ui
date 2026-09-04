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
package io.github.astrapi69.mystic.crypt.plugin.conversion.wizard;

import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * The domain model the conversion wizard carries through its three steps: the file to convert, the
 * file to write the result to, what the source file was detected to hold, the conversion the user
 * chose and the message the last attempt to convert ended with.
 * <p>
 * This is the wizard's single source of truth - every step panel reads and writes it rather than
 * keeping its own copy, so the wizard's state is readable at any moment from outside the panels too
 * (tests included). It replaces the old {@code ConversionPanelModel}, with one addition: the chosen
 * {@link ConversionOperation}, which the old single-tool panel did not need since each of its buttons
 * already was the operation.
 */
public class ConversionWizardModel
{

	/**
	 * What a label carries while there is nothing to say - a blank keeps the row at its height where
	 * an empty string would collapse it
	 */
	public static final String NOTHING_TO_SAY = " ";

	private String sourceFilePath = "";

	private String targetFilePath = "";

	private ConversionSupport.FileKind fileKind;

	private ConversionOperation operation;

	private String whatItHolds = NOTHING_TO_SAY;

	private String resultMessage = NOTHING_TO_SAY;

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
	 * Gets the kind the source file was detected as, or null while no file could be read
	 *
	 * @return the kind the source file was detected as, or null while no file could be read
	 */
	public ConversionSupport.FileKind getFileKind()
	{
		return fileKind;
	}

	/**
	 * Sets the kind the source file was detected as, null while no file could be read
	 *
	 * @param fileKind
	 *            the kind the source file was detected as, null while no file could be read
	 */
	public void setFileKind(final ConversionSupport.FileKind fileKind)
	{
		this.fileKind = fileKind;
	}

	/**
	 * Gets the conversion the user chose on the Target step, or null while none is chosen yet
	 *
	 * @return the chosen conversion, or null
	 */
	public ConversionOperation getOperation()
	{
		return operation;
	}

	/**
	 * Sets the conversion the user chose on the Target step
	 *
	 * @param operation
	 *            the chosen conversion, or null
	 */
	public void setOperation(final ConversionOperation operation)
	{
		this.operation = operation;
	}

	/**
	 * Gets the description of what the source file was found to hold
	 *
	 * @return the description of what the source file was found to hold
	 */
	public String getWhatItHolds()
	{
		return whatItHolds;
	}

	/**
	 * Sets the description of what the source file was found to hold
	 *
	 * @param whatItHolds
	 *            the description of what the source file was found to hold
	 */
	public void setWhatItHolds(final String whatItHolds)
	{
		this.whatItHolds = whatItHolds;
	}

	/**
	 * Gets the message the last conversion attempt ended with
	 *
	 * @return the message the last conversion attempt ended with
	 */
	public String getResultMessage()
	{
		return resultMessage;
	}

	/**
	 * Sets the message the last conversion attempt ended with
	 *
	 * @param resultMessage
	 *            the message the last conversion attempt ended with
	 */
	public void setResultMessage(final String resultMessage)
	{
		this.resultMessage = resultMessage;
	}
}
