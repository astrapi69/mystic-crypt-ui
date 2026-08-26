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
package io.github.astrapi69.mystic.crypt.plugin.kem;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Callable;

import javax.crypto.SecretKey;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * The command line side of the key exchange, with {@code mystic-crypt-ui --cli keyx}.
 * <p>
 * The three subcommands are the three steps, and each one is a separate run because the two sides
 * are normally two people on two machines:
 *
 * <pre>
 * keyx new      the recipient makes a key pair: the private half into a file,
 *               the public half to hand out
 * keyx send     the sender takes that public half and produces a handshake,
 *               optionally encrypting a message with the secret it just made
 * keyx receive  the recipient turns the handshake into the same secret,
 *               and reads the message
 * </pre>
 *
 * Both sides print the fingerprint of their secret. If the two differ, the exchange did not happen
 * between the two keys that were meant.
 * <p>
 * All of it calls {@link KeyExchangeSupport}, exactly like the tool window does.
 */
@Command(name = "keyx", mixinStandardHelpOptions = true, description = "Exchange a key with someone who holds only their own half", subcommands = {
		KeyExchangeCommand.NewCommand.class, KeyExchangeCommand.SendCommand.class,
		KeyExchangeCommand.ReceiveCommand.class })
public class KeyExchangeCommand implements Runnable
{

	@Spec
	CommandSpec spec;

	@Override
	public void run()
	{
		// without a subcommand the usage is the most useful answer
		spec.commandLine().usage(spec.commandLine().getOut());
	}

	@Command(name = "new", mixinStandardHelpOptions = true, description = "Make a key pair and write out its public half")
	public static class NewCommand implements Callable<Integer>
	{
		@Option(names = { "-a",
				"--algorithm" }, description = "one of: ${COMPLETION-CANDIDATES}", defaultValue = KeyExchangeSupport.ML_KEM_768)
		String algorithm;

		@Option(names = { "-k",
				"--key" }, required = true, description = "write the private key here; whoever holds this file can read everything sent to it")
		File keyFile;

		@Option(names = { "-p",
				"--public" }, description = "write the public key here as well as printing it")
		File publicKeyFile;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			if (keyFile.exists())
			{
				throw new IllegalArgumentException("'" + keyFile + "' already exists");
			}
			KeyExchangeSupport.Party party = KeyExchangeSupport.newParty(algorithm);
			write(keyFile, KeyExchangeSupport.privateKeyOf(party));
			String publicKey = KeyExchangeSupport.publicKeyOf(party);
			if (publicKeyFile != null)
			{
				write(publicKeyFile, publicKey);
			}
			spec.commandLine().getOut().println(publicKey);
			return 0;
		}
	}

	@Command(name = "send", mixinStandardHelpOptions = true, description = "Make a shared secret from someone's public key")
	public static class SendCommand implements Callable<Integer>
	{
		@Option(names = { "-r",
				"--recipient" }, required = true, description = "the file holding the recipient's public key")
		File recipientPublicKeyFile;

		@Option(names = { "-m", "--message" }, description = "encrypt this message with the secret")
		String message;

		@Option(names = { "-o", "--out" }, description = "write the handshake here as well as printing it")
		File handshakeFile;

		@Option(names = { "-e",
				"--encrypted" }, description = "write the encrypted message here as well as printing it")
		File encryptedFile;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyExchangeSupport.Handshake handshake = KeyExchangeSupport.encapsulate(read(
				recipientPublicKeyFile));
			if (handshakeFile != null)
			{
				write(handshakeFile, handshake.handshake());
			}
			spec.commandLine().getOut().println(handshake.handshake());
			spec.commandLine().getOut()
				.println("fingerprint " + KeyExchangeSupport.fingerprintOf(handshake.sharedSecret()));
			if (message != null)
			{
				String encrypted = KeyExchangeSupport.encryptMessage(handshake.sharedSecret(),
					message.getBytes(StandardCharsets.UTF_8));
				if (encryptedFile != null)
				{
					write(encryptedFile, encrypted);
				}
				spec.commandLine().getOut().println(encrypted);
			}
			return 0;
		}
	}

	@Command(name = "receive", mixinStandardHelpOptions = true, description = "Turn a handshake into the same shared secret")
	public static class ReceiveCommand implements Callable<Integer>
	{
		@Option(names = { "-k",
				"--key" }, required = true, description = "the file holding the private key of this side")
		File keyFile;

		@Option(names = { "-s",
				"--handshake" }, required = true, description = "the file holding the handshake that came back")
		File handshakeFile;

		@Option(names = { "-e",
				"--encrypted" }, description = "the file holding a message that was encrypted with the secret")
		File encryptedFile;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			SecretKey secret = KeyExchangeSupport
				.decapsulate(KeyExchangeSupport.partyFrom(read(keyFile)), read(handshakeFile));
			spec.commandLine().getOut()
				.println("fingerprint " + KeyExchangeSupport.fingerprintOf(secret));
			if (encryptedFile != null)
			{
				spec.commandLine().getOut().println(new String(
					KeyExchangeSupport.decryptMessage(secret, read(encryptedFile)),
					StandardCharsets.UTF_8));
			}
			return 0;
		}
	}

	private static String read(final File file) throws java.io.IOException
	{
		if (!file.isFile())
		{
			throw new IllegalArgumentException("'" + file + "' is not a file");
		}
		return Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
	}

	private static void write(final File file, final String content) throws java.io.IOException
	{
		Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
	}
}
