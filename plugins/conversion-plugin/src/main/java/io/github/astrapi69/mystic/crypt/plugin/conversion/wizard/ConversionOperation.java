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

import java.io.File;

import io.github.astrapi69.mystic.crypt.plugin.conversion.ConversionSupport;

/**
 * The four conversions the wizard's Target step can offer, each valid only for what the Source step
 * detected. Mirrors {@code ConversionPanel.setConversionsFor(...)}, the logic this enum replaces:
 * only the conversions that make sense for the detected file kind are ever offered.
 */
public enum ConversionOperation
{

	/** Writes a PEM file as the binary DER encoding */
	PEM_TO_DER("to DER")
	{
		@Override
		public boolean isValidFor(final ConversionSupport.FileKind fileKind, final File sourceFile)
		{
			return fileKind != null && fileKind.pem();
		}

		@Override
		public void execute(final File source, final File target) throws Exception
		{
			ConversionSupport.pemToDer(source, target);
		}

		@Override
		public File defaultTargetFile(final File source)
		{
			return new File(parentDirectory(source), withoutExtension(source) + ".der");
		}
	},

	/** Writes a DER file as PEM, under the header that fits what it holds */
	DER_TO_PEM("to PEM")
	{
		@Override
		public boolean isValidFor(final ConversionSupport.FileKind fileKind, final File sourceFile)
		{
			return fileKind != null && !fileKind.pem()
				&& !fileKind.description().startsWith("nothing");
		}

		@Override
		public void execute(final File source, final File target) throws Exception
		{
			ConversionSupport.derToPem(source, target);
		}

		@Override
		public File defaultTargetFile(final File source)
		{
			// pemFileFor already computes exactly this default, and is already tested
			return ConversionSupport.pemFileFor(source, null);
		}
	},

	/** Writes a private key file as PKCS#8 - what Java reads and writes */
	TO_PKCS8("to PKCS#8 (Java)")
	{
		@Override
		public boolean isValidFor(final ConversionSupport.FileKind fileKind, final File sourceFile)
		{
			return sourceFile != null && ConversionSupport.holdsAPrivateKey(sourceFile);
		}

		@Override
		public void execute(final File source, final File target) throws Exception
		{
			ConversionSupport.toPkcs8(source, target);
		}

		@Override
		public File defaultTargetFile(final File source)
		{
			return new File(parentDirectory(source), withoutExtension(source) + "-pkcs8.pem");
		}
	},

	/** Writes a private key file as PKCS#1 - what openssl and nginx expect */
	TO_PKCS1("to PKCS#1 (openssl)")
	{
		@Override
		public boolean isValidFor(final ConversionSupport.FileKind fileKind, final File sourceFile)
		{
			return sourceFile != null && ConversionSupport.holdsAPrivateKey(sourceFile);
		}

		@Override
		public void execute(final File source, final File target) throws Exception
		{
			ConversionSupport.toPkcs1(source, target);
		}

		@Override
		public File defaultTargetFile(final File source)
		{
			return new File(parentDirectory(source), withoutExtension(source) + "-pkcs1.pem");
		}
	};

	private final String label;

	ConversionOperation(final String label)
	{
		this.label = label;
	}

	/**
	 * The label the Target step shows on this conversion's button
	 *
	 * @return the label
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Whether this conversion makes sense for what the source file was detected to hold
	 *
	 * @param fileKind
	 *            what the source file was detected to hold, null while nothing was read yet
	 * @param sourceFile
	 *            the source file itself - {@code TO_PKCS8} and {@code TO_PKCS1} re-read it, since
	 *            whether it holds a private key is not derivable from {@code fileKind} alone
	 * @return true if this conversion can be applied to the source file
	 */
	public abstract boolean isValidFor(ConversionSupport.FileKind fileKind, File sourceFile);

	/**
	 * Applies this conversion
	 *
	 * @param source
	 *            the file to convert
	 * @param target
	 *            the file to write
	 * @throws Exception
	 *             if the source cannot be converted this way or the target cannot be written
	 */
	public abstract void execute(File source, File target) throws Exception;

	/**
	 * The file the Target step pre-fills before the user types a target of their own: the source
	 * file's name, adapted for this conversion, next to the source file
	 *
	 * @param source
	 *            the file to convert
	 * @return the default target file
	 */
	public abstract File defaultTargetFile(File source);

	private static File parentDirectory(final File source)
	{
		return source.getAbsoluteFile().getParentFile();
	}

	private static String withoutExtension(final File source)
	{
		String name = source.getName();
		int lastDot = name.lastIndexOf('.');
		return 0 < lastDot ? name.substring(0, lastDot) : name;
	}
}
