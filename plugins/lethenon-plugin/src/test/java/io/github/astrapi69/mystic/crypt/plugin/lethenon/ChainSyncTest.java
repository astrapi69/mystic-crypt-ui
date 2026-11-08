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
package io.github.astrapi69.mystic.crypt.plugin.lethenon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.astrapi69.mystic.crypt.settings.PluginSettings;

/**
 * The record of the last sync this program did (#831)
 */
class ChainSyncTest
{

	private static final Instant TWO_PAST_TWO = Instant.parse("2026-11-08T14:02:00Z");

	@TempDir
	Path configuration;

	@TempDir
	Path directory;

	@BeforeEach
	void aConfigurationOfItsOwn()
	{
		System.setProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY,
			configuration.toString());
	}

	@AfterEach
	void releaseTheConfiguration()
	{
		System.clearProperty(PluginSettings.CONFIGURATION_DIRECTORY_PROPERTY);
	}

	@Test
	void aChainNeverSynced_hasNoTime()
	{
		assertEquals(Optional.empty(), ChainSync.lastOf(directory.resolve("chain.lethenon")));
	}

	@Test
	void aSync_isRead_back_howeverThePathIsSpelled()
	{
		Path chainFile = directory.resolve("chain.lethenon");

		ChainSync.record(chainFile, TWO_PAST_TWO);

		assertEquals(Optional.of(TWO_PAST_TWO),
			ChainSync.lastOf(directory.resolve(".").resolve("chain.lethenon")));
	}

	@Test
	void aSyncOfAnotherFile_saysNothingOfThisOne()
	{
		ChainSync.record(directory.resolve("other.lethenon"), TWO_PAST_TWO);

		assertEquals(Optional.empty(), ChainSync.lastOf(directory.resolve("chain.lethenon")));
	}

	@Test
	void aRecordWithoutATime_isNoTime() throws Exception
	{
		Path chainFile = directory.resolve("chain.lethenon");
		ChainSync.record(chainFile, TWO_PAST_TWO);
		Path record = PluginSettings.file(configuration.toFile(), ChainSync.RECORD_ID);
		Files.writeString(record,
			Files.readString(record).replaceAll("synced.at=\\d+", "synced.at=soon"));

		assertEquals(Optional.empty(), ChainSync.lastOf(chainFile));
	}

	@Test
	void aSyncThatWorked_isRecordedWithWhatItUsed()
	{
		Path chainFile = directory.resolve("chain.lethenon");
		Instant before = Instant.now();

		LethenonSettingsContribution.rememberASync(chainFile, "127.0.0.1:18480", "");

		Instant recorded = ChainSync.lastOf(chainFile).orElseThrow();
		assertFalse(recorded.isBefore(before.minusMillis(1)), "recorded before the sync: " + recorded);
		assertFalse(recorded.isAfter(Instant.now()), "recorded after the sync: " + recorded);
	}
}
