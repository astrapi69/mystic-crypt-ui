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
package io.github.astrapi69.mystic.crypt.app.file.xml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.Security;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.crypt.api.algorithm.key.KeyPairGeneratorAlgorithm;
import io.github.astrapi69.crypt.data.factory.KeyPairFactory;
import io.github.astrapi69.crypt.data.key.KeyModelExtensions;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * The readable database must never reach the disk.
 * <p>
 * Saving with a key file used to write the xml to the database file first and overwrite it with the
 * encrypted bytes immediately afterwards. Anything that came between - a crash, a full disk, a
 * backup tool reading the file at that moment - got the whole database as readable xml under the
 * name of an encrypted one.
 * <p>
 * Watching for that window from the outside is a race. This test removes the race by putting a
 * named pipe where the database file goes: whatever the save writes first is what comes out of the
 * pipe, and nothing is timing dependent about it.
 */
class CleartextIsNeverWrittenTest
{

	private static final int ENOUGH_TO_RECOGNISE_XML = 512;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@EnabledOnOs({ OS.LINUX, OS.MAC })
	@DisplayName("the first thing a key file save writes is already encrypted")
	void theFirstThingWrittenIsAlreadyEncrypted(@TempDir File directory) throws Exception
	{
		File vault = new File(directory, "watched.mcrdb");
		// the save writes its bytes next to the database and moves them onto it, so the pipe goes
		// where the writing actually happens
		File written = new File(directory,
			vault.getName() + VaultFileWriter.WORK_IN_PROGRESS_SUFFIX);
		namedPipeAt(written);
		KeyPair keyPair = KeyPairFactory.newKeyPair(KeyPairGeneratorAlgorithm.RSA, 2048);
		ApplicationModelBean applicationModelBean = ApplicationModelBean.builder()
			.masterPwFileModelBean(
				MasterPwFileModelBean.builder().applicationFileInfo(FileInfo.toFileInfo(vault))
					.selectedApplicationFilePath(vault.getAbsolutePath())
					.privateKeyInfo(KeyModelExtensions.toKeyModel(keyPair.getPrivate()))
					.withMasterPw(false).withKeyFile(true).build())
			.build();

		BlockingQueue<byte[]> whatWasWrittenFirst = new ArrayBlockingQueue<>(1);
		Thread reader = new Thread(() -> {
			try (InputStream pipe = Files.newInputStream(written.toPath()))
			{
				whatWasWrittenFirst.offer(pipe.readNBytes(ENOUGH_TO_RECOGNISE_XML));
			}
			catch (Exception exception)
			{
				whatWasWrittenFirst.offer(new byte[0]);
			}
		});
		reader.setDaemon(true);
		reader.start();

		Thread saver = new Thread(() -> {
			try
			{
				ApplicationXmlFileStoreWorker.saveToFileWithPrivateKey(applicationModelBean);
			}
			catch (RuntimeException expectedWhenTheReaderIsGone)
			{
				// the pipe is closed as soon as the first bytes have been taken, which is all this
				// test needs; what the save does afterwards is not its subject
			}
		});
		saver.setDaemon(true);
		saver.start();

		byte[] first = whatWasWrittenFirst.poll(30, TimeUnit.SECONDS);
		assertNotNull(first, "nothing was written to the database file at all");
		assertTrue(first.length > 0, "nothing was written to the database file at all");

		String readable = new String(first, StandardCharsets.UTF_8);
		assertFalse(
			readable.contains("masterPwFileModelBean") || readable.contains("applicationFileInfo")
				|| readable.contains("<io.github.astrapi69"),
			"the readable database was written to disk before it was encrypted: "
				+ readable.substring(0, Math.min(120, readable.length())));
	}

	private static void namedPipeAt(File file) throws Exception
	{
		Process mkfifo = new ProcessBuilder("mkfifo", file.getAbsolutePath()).start();
		if (!mkfifo.waitFor(10, TimeUnit.SECONDS) || mkfifo.exitValue() != 0)
		{
			throw new IllegalStateException("could not create a named pipe for the test");
		}
	}
}
