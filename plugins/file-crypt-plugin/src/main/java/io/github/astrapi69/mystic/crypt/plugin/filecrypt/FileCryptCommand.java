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
package io.github.astrapi69.mystic.crypt.plugin.filecrypt;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * The command line side of this plugin: the same two operations the tool window offers, driven from
 * a terminal with {@code mystic-crypt-ui --cli filecrypt encrypt|decrypt}.
 * <p>
 * Both work on a file or on a piece of text, and both call {@link FileCryptSupport}, exactly like
 * the panel does - the behaviour cannot drift apart between the two ways of using it.
 */
@Command(name = "filecrypt", mixinStandardHelpOptions = true, description = "Encrypt and decrypt a file or a text with a passphrase", subcommands = {
		FileCryptCommand.EncryptCommand.class, FileCryptCommand.DecryptCommand.class })
public class FileCryptCommand implements Runnable
{

	@Spec
	CommandSpec spec;

	@Override
	public void run()
	{
		// without a subcommand the usage is the most useful answer
		spec.commandLine().usage(spec.commandLine().getOut());
	}

	/** What both subcommands need: something to work on and a passphrase */
	public static class CryptOptions
	{
		@Option(names = { "-f", "--file" }, description = "the file to work on")
		File file;

		@Option(names = { "-o", "--out" }, description = "the file to write; derived from the input when left out")
		File out;

		@Option(names = { "-t", "--text" }, description = "work on this text instead of a file")
		String text;

		@Option(names = { "-p", "--passphrase" }, required = true, arity = "0..1",
			interactive = true, description = "the passphrase; asked for when the value is left out")
		String passphrase;

		/** Whether text was given rather than a file */
		boolean isText()
		{
			return text != null;
		}

		void requireOneInput()
		{
			if (text == null && file == null)
			{
				throw new IllegalArgumentException("give either --file or --text");
			}
			if (text != null && file != null)
			{
				throw new IllegalArgumentException("give either --file or --text, not both");
			}
		}
	}

	@Command(name = "encrypt", mixinStandardHelpOptions = true, description = "Encrypt a file or a text with a passphrase")
	public static class EncryptCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		CryptOptions options = new CryptOptions();

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			options.requireOneInput();
			if (options.isText())
			{
				spec.commandLine().getOut()
					.println(FileCryptSupport.encryptText(options.text, options.passphrase));
				return 0;
			}
			File written = FileCryptSupport.encryptFile(options.file, options.out,
				options.passphrase);
			spec.commandLine().getOut().println("encrypted to " + written);
			return 0;
		}
	}

	@Command(name = "decrypt", mixinStandardHelpOptions = true, description = "Decrypt a file or a text that was encrypted with 'encrypt'")
	public static class DecryptCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		CryptOptions options = new CryptOptions();

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			options.requireOneInput();
			if (options.isText())
			{
				spec.commandLine().getOut()
					.println(FileCryptSupport.decryptText(options.text, options.passphrase));
				return 0;
			}
			File written = FileCryptSupport.decryptFile(options.file, options.out,
				options.passphrase);
			spec.commandLine().getOut().println("decrypted to " + written);
			return 0;
		}
	}
}
