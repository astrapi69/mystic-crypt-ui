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

import java.io.File;

/**
 * The class {@link ReviewPanelModel} holds what the entry form of the {@link ReviewPanel} currently
 * contains: the read-only preview text, the file name the certificate will be saved as and the
 * directory it will be saved in. This is the state of the form, not the state of the certificate
 * wizard, which the panel keeps in its own wizard model
 */
public class ReviewPanelModel
{

	/** The read-only preview of what will be saved */
	private String preview = "";

	/** The file name the certificate will be saved as */
	private String fileName;

	/** The directory the certificate will be saved in */
	private File saveDirectory;

	/**
	 * Gets the read-only preview of what will be saved
	 *
	 * @return the preview text
	 */
	public String getPreview()
	{
		return preview;
	}

	/**
	 * Sets the read-only preview of what will be saved
	 *
	 * @param preview
	 *            the preview text
	 */
	public void setPreview(final String preview)
	{
		this.preview = preview;
	}

	/**
	 * Gets the file name the certificate will be saved as
	 *
	 * @return the file name
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * Sets the file name the certificate will be saved as
	 *
	 * @param fileName
	 *            the file name
	 */
	public void setFileName(final String fileName)
	{
		this.fileName = fileName;
	}

	/**
	 * Gets the directory the certificate will be saved in
	 *
	 * @return the save directory
	 */
	public File getSaveDirectory()
	{
		return saveDirectory;
	}

	/**
	 * Sets the directory the certificate will be saved in
	 *
	 * @param saveDirectory
	 *            the save directory
	 */
	public void setSaveDirectory(final File saveDirectory)
	{
		this.saveDirectory = saveDirectory;
	}

	/**
	 * Gets the directory the certificate will be saved in, as the path a text field shows
	 *
	 * @return the save directory's absolute path, or the empty string when none is set
	 */
	public String getSaveDirectoryPath()
	{
		return saveDirectory == null ? "" : saveDirectory.getAbsolutePath();
	}

	/**
	 * Sets the directory the certificate will be saved in, from the path a text field holds
	 *
	 * @param saveDirectoryPath
	 *            the path, or blank for none
	 */
	public void setSaveDirectoryPath(final String saveDirectoryPath)
	{
		this.saveDirectory = saveDirectoryPath == null || saveDirectoryPath.isBlank()
			? null
			: new File(saveDirectoryPath);
	}
}
