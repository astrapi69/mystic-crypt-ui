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
package io.github.astrapi69.mystic.crypt.plugin.sharing;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * The command line side of this plugin: splitting a secret into shares and putting it back
 * together, with {@code mystic-crypt-ui --cli share split|combine}.
 * <p>
 * Both call {@link SecretSharingSupport}, exactly like the tool window does.
 */
@Command(name = "share", mixinStandardHelpOptions = true, description = "Split a secret into shares and put it back together", subcommands = {
		SecretSharingCommand.SplitCommand.class, SecretSharingCommand.CombineCommand.class })
public class SecretSharingCommand implements Runnable
{

	@Spec
	CommandSpec spec;

	@Override
	public void run()
	{
		// without a subcommand the usage is the most useful answer
		spec.commandLine().usage(spec.commandLine().getOut());
	}

	@Command(name = "split", mixinStandardHelpOptions = true, description = "Split a secret into shares")
	public static class SplitCommand implements Callable<Integer>
	{
		@Option(names = { "-s", "--secret" }, arity = "0..1", interactive = true,
			description = "the secret; asked for when the value is left out")
		String secret;

		@Option(names = { "-f", "--file" }, description = "split this file instead of a typed secret")
		File file;

		@Option(names = { "-t",
				"--threshold" }, required = true, description = "how many shares are needed to rebuild the secret")
		int threshold;

		@Option(names = { "-n",
				"--shares" }, required = true, description = "how many shares to produce")
		int totalShares;

		@Option(names = { "-o", "--out" }, description = "write the shares here, one per line")
		File out;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			if (secret == null && file == null)
			{
				throw new IllegalArgumentException("give either --secret or --file");
			}
			if (secret != null && file != null)
			{
				throw new IllegalArgumentException("give either --secret or --file, not both");
			}
			List<String> shares = file != null
				? SecretSharingSupport.split(Files.readAllBytes(file.toPath()), threshold,
					totalShares)
				: SecretSharingSupport.splitText(secret, threshold, totalShares);
			if (out != null)
			{
				if (out.exists())
				{
					throw new IllegalArgumentException("'" + out + "' already exists");
				}
				Files.write(out.toPath(), shares, StandardCharsets.UTF_8);
				spec.commandLine().getOut().println("wrote " + shares.size() + " shares to " + out);
				return 0;
			}
			shares.forEach(spec.commandLine().getOut()::println);
			return 0;
		}
	}

	@Command(name = "combine", mixinStandardHelpOptions = true, description = "Put a secret back together from its shares")
	public static class CombineCommand implements Callable<Integer>
	{
		@Option(names = { "-s",
				"--share" }, description = "one share; give the option once per share")
		List<String> shares = new ArrayList<>();

		@Option(names = { "-f",
				"--file" }, description = "read the shares from this file, one per line")
		File file;

		@Option(names = { "-o", "--out" }, description = "write the rebuilt secret here")
		File out;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			List<String> all = new ArrayList<>(shares);
			if (file != null)
			{
				all.addAll(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
			}
			if (all.isEmpty())
			{
				throw new IllegalArgumentException("give --share once per share, or --file");
			}
			byte[] secret = SecretSharingSupport.combine(all);
			if (out != null)
			{
				if (out.exists())
				{
					throw new IllegalArgumentException("'" + out + "' already exists");
				}
				Files.write(out.toPath(), secret);
				spec.commandLine().getOut().println("wrote the secret to " + out);
				return 0;
			}
			spec.commandLine().getOut().println(new String(secret, StandardCharsets.UTF_8));
			return 0;
		}
	}
}
