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
package io.github.astrapi69.mystic.crypt.plugin.keystore;

import java.io.File;
import java.io.PrintWriter;
import java.security.KeyStore;
import java.util.List;
import java.util.concurrent.Callable;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.api.type.KeystoreType;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * The command line side of this plugin: the very same operations the tool window offers, driven
 * from a terminal with {@code mystic-crypt-ui --cli keystore <subcommand>}.
 * <p>
 * Every subcommand calls {@link KeyStoreSupport}, exactly like the panel does - the behaviour cannot
 * drift apart between the two ways of using it.
 */
@Command(name = "keystore", description = "Inspect and manage a Java key store", subcommands = {
		KeyStoreCommand.ListCommand.class, KeyStoreCommand.CreateCommand.class,
		KeyStoreCommand.AddKeyPairCommand.class, KeyStoreCommand.ImportCertificateCommand.class,
		KeyStoreCommand.ExportCertificateCommand.class, KeyStoreCommand.DeleteCommand.class })
public class KeyStoreCommand implements Runnable
{

	@Spec
	CommandSpec spec;

	@Override
	public void run()
	{
		// without a subcommand the usage is the most useful answer
		spec.commandLine().usage(spec.commandLine().getOut());
	}

	/** The options every subcommand needs: which file, which type, which password */
	public static class StoreOptions
	{
		@Option(names = { "-f", "--file" }, required = true, description = "the key store file")
		File file;

		@Option(names = { "-t",
				"--type" }, description = "the store type: ${COMPLETION-CANDIDATES}; the configured default when left out")
		KeystoreType type;

		/** The store type to use: the given one, or the one configured in the settings */
		KeystoreType type()
		{
			return type != null
				? type
				: KeyStoreSettings.type();
		}

		@Option(names = { "-p", "--password" }, required = true, arity = "0..1", interactive = true,
			description = "the store password; asked for when the value is left out")
		String password;
	}

	@Command(name = "list", description = "List what a key store holds")
	public static class ListCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStore keyStore = KeyStoreSupport.open(store.file, store.type(), store.password);
			PrintWriter out = spec.commandLine().getOut();
			List<KeyStoreSupport.EntryInfo> entries = KeyStoreSupport.entries(keyStore);
			for (KeyStoreSupport.EntryInfo entry : entries)
			{
				out.println(entry.alias() + "\t" + entry.entryKind() + "\t" + entry.algorithm()
					+ "\t" + entry.subject() + "\t" + entry.validUntil() + "\t"
					+ entry.fingerprint());
			}
			out.println(entries.size() + " entries");
			return 0;
		}
	}

	@Command(name = "create", description = "Create a new, empty key store")
	public static class CreateCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStoreSupport.create(store.file, store.type(), store.password);
			spec.commandLine().getOut().println("created " + store.file);
			return 0;
		}
	}

	@Command(name = "add-keypair", description = "Generate a key pair with a self-signed certificate and store it")
	public static class AddKeyPairCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Option(names = { "-a", "--alias" }, required = true, description = "the alias to store under")
		String alias;

		@Option(names = { "-d",
				"--dname" }, description = "the certificate subject, for instance CN=my server; the configured default when left out")
		String distinguishedName;

		@Option(names = { "-A",
				"--algorithm" }, description = "the key algorithm: ${COMPLETION-CANDIDATES}; the configured default when left out")
		KeyPairGeneratorAlgorithm algorithm;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStore keyStore = KeyStoreSupport.open(store.file, store.type(), store.password);
			KeyPairGeneratorAlgorithm keyAlgorithm = algorithm != null
				? algorithm
				: KeyStoreSettings.algorithm();
			KeyStoreSupport.addKeyPair(keyStore, store.file, store.password, alias,
				distinguishedName != null ? distinguishedName : KeyStoreSettings.distinguishedName(),
				keyAlgorithm, KeyStoreSettings.keySize(),
				KeyStoreSupport.signatureAlgorithmFor(keyAlgorithm), KeyStoreSettings.daysValid());
			spec.commandLine().getOut().println("added '" + alias + "' (" + keyAlgorithm + ")");
			return 0;
		}
	}

	@Command(name = "import-cert", description = "Import a certificate from a pem or der file")
	public static class ImportCertificateCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Option(names = { "-a", "--alias" }, required = true, description = "the alias to store under")
		String alias;

		@Option(names = { "-c",
				"--certificate" }, required = true, description = "the certificate file to import")
		File certificate;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStore keyStore = KeyStoreSupport.open(store.file, store.type(), store.password);
			KeyStoreSupport.importCertificate(keyStore, store.file, store.password, alias,
				certificate);
			spec.commandLine().getOut().println("imported the certificate as '" + alias + "'");
			return 0;
		}
	}

	@Command(name = "export-cert", description = "Write the certificate of an alias as pem")
	public static class ExportCertificateCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Option(names = { "-a", "--alias" }, required = true, description = "the alias to export")
		String alias;

		@Option(names = { "-o", "--out" }, required = true, description = "the pem file to write")
		File out;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStore keyStore = KeyStoreSupport.open(store.file, store.type(), store.password);
			KeyStoreSupport.exportCertificate(keyStore, alias, out);
			spec.commandLine().getOut().println("exported '" + alias + "' to " + out);
			return 0;
		}
	}

	@Command(name = "delete", description = "Remove an alias from a key store")
	public static class DeleteCommand implements Callable<Integer>
	{
		@CommandLine.Mixin
		StoreOptions store = new StoreOptions();

		@Option(names = { "-a", "--alias" }, required = true, description = "the alias to remove")
		String alias;

		@Spec
		CommandSpec spec;

		@Override
		public Integer call() throws Exception
		{
			KeyStoreSupport.deleteAlias(store.file, store.type(), store.password, alias);
			spec.commandLine().getOut().println("deleted '" + alias + "'");
			return 0;
		}
	}
}
