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
package io.github.astrapi69.mystic.crypt.panel.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.astrapi69.mystic.crypt.app.file.JsonFileStore;

/**
 * The remembered sign-in keeps, per database, the key file and the factors the last sign-in to it
 * used (#997), and it stays a file the release before it reads as before: the file is a persisted
 * format of this application.
 */
class TheRememberedSignInPerDatabaseTest
{

	/** A {@code memoizedSignin.json} as the release before this change writes it */
	private static final String TODAYS_FILE = """
		{
		  "selectedApplicationFilePath": "/home/user/vaults/second.mcrdb",
		  "applicationFilePaths": ["", "/home/user/vaults/first.mcrdb", "/home/user/vaults/second.mcrdb"],
		  "selectedKeyFilePath": "/home/user/keys/second-key.pem",
		  "keyFilePaths": ["", "/home/user/keys/first-key.pem", "/home/user/keys/second-key.pem"],
		  "withKeyFile": true,
		  "withMasterPw": true
		}
		""";

	@TempDir
	File directory;

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', value = {
			"a database with an entry offers its key file and both factors|true|true|false|true|true",
			"a database without an entry offers nothing|false|true|false|false|false",
			"a key file that is gone is not offered, the factors are|true|false|false|true|false",
			"the same database by a relative path finds its entry|true|true|true|true|true" })
	void choiceFor_offersWhatTheDatabaseWasOpenedWith(final String caseName, final boolean hasEntry,
		final boolean keyFileExists, final boolean lookedUpRelative, final boolean expectedEntry,
		final boolean expectedKeyFileKept) throws Exception
	{
		File database = new File(directory, "first.mcrdb");
		File keyFile = new File(directory, "first-key.pem");
		if (keyFileExists)
		{
			Files.writeString(keyFile.toPath(), "a key file");
		}
		Map<String, DatabaseSignInChoice> remembered = new HashMap<>();
		if (hasEntry)
		{
			remembered.put(database.getAbsolutePath(),
				DatabaseSignInChoice.builder().keyFilePath(keyFile.getAbsolutePath())
					.withKeyFile(true).withMasterPw(true).build());
		}
		String lookedUp = lookedUpRelative
			? Path.of("").toAbsolutePath().relativize(database.toPath().toAbsolutePath()).toString()
			: database.getAbsolutePath();

		Optional<DatabaseSignInChoice> choice = RememberedKeyFileSupport.choiceFor(remembered,
			lookedUp);

		assertEquals(expectedEntry, choice.isPresent(), caseName);
		choice.ifPresent(found -> {
			assertEquals(expectedKeyFileKept ? keyFile.getAbsolutePath() : "",
				found.getKeyFilePath(), caseName);
			assertTrue(found.isWithKeyFile() && found.isWithMasterPw(), caseName);
		});
	}

	@Test
	@DisplayName("a null map and a blank path find nothing")
	void choiceFor_findsNothing_forNothing()
	{
		assertEquals(Optional.empty(), RememberedKeyFileSupport.choiceFor(null, "/a/b.mcrdb"));
		assertEquals(Optional.empty(), RememberedKeyFileSupport.choiceFor(new HashMap<>(), " "));
	}

	@Test
	@DisplayName("remember sets the entry of the database and keeps the entries of the others")
	void remember_replacesTheEntryOfOneDatabase_andKeepsTheOthers()
	{
		DatabaseSignInChoice firstChoice = DatabaseSignInChoice.builder()
			.keyFilePath("/keys/first.pem").withKeyFile(true).withMasterPw(true).build();
		DatabaseSignInChoice secondChoice = DatabaseSignInChoice.builder().keyFilePath("")
			.withKeyFile(false).withMasterPw(true).build();
		Map<String, DatabaseSignInChoice> before = new HashMap<>();
		before.put("/vaults/first.mcrdb", firstChoice);
		before.put("/vaults/./second.mcrdb", firstChoice);

		Map<String, DatabaseSignInChoice> after = RememberedKeyFileSupport.remember(before,
			"/vaults/second.mcrdb", secondChoice);

		assertEquals(2, after.size(), "the entry under the other spelling is replaced: " + after);
		assertEquals(firstChoice, after.get("/vaults/first.mcrdb"));
		assertEquals(secondChoice, after.get("/vaults/second.mcrdb"));
		assertEquals(2, before.size(), "the input is not changed");
	}

	@Test
	@DisplayName("today's memoizedSignin.json keeps the database, the key file and both checkboxes")
	void todaysFile_isReadUnchanged_withNoEntriesYet() throws Exception
	{
		File file = new File(directory, "memoizedSignin.json");
		Files.writeString(file.toPath(), TODAYS_FILE);

		MemoizedSigninModelBean read = JsonFileStore.read(file, MemoizedSigninModelBean.class)
			.orElseThrow();

		assertEquals("/home/user/vaults/second.mcrdb", read.getSelectedApplicationFilePath());
		assertEquals("/home/user/keys/second-key.pem", read.getSelectedKeyFilePath());
		assertTrue(read.isWithKeyFile());
		assertTrue(read.isWithMasterPw());
		assertEquals(3, read.getApplicationFilePaths().size());
		assertEquals(3, read.getKeyFilePaths().size());
		assertNotNull(read.getChoicesByDatabase(), "a file without entries reads as no entries");
		assertTrue(read.getChoicesByDatabase().isEmpty());

		MasterPwFileModelBean model = MasterPwFileModelBean.builder().build();
		model.merge(read);
		assertEquals("/home/user/keys/second-key.pem", model.getSelectedKeyFilePath());
		assertTrue(model.isWithKeyFile() && model.isWithMasterPw());
	}

	@Test
	@DisplayName("the file the new code writes keeps today's fields with today's meaning")
	void theWrittenFile_keepsTodaysFields_besideTheNewOne() throws Exception
	{
		File file = new File(directory, "memoizedSignin.json");
		Files.writeString(file.toPath(), TODAYS_FILE);
		MemoizedSigninModelBean read = JsonFileStore.read(file, MemoizedSigninModelBean.class)
			.orElseThrow();
		read.setChoicesByDatabase(
			RememberedKeyFileSupport.remember(null, "/home/user/vaults/second.mcrdb",
				DatabaseSignInChoice.builder().keyFilePath("/home/user/keys/second-key.pem")
					.withKeyFile(true).withMasterPw(true).build()));

		JsonFileStore.write(read, file, MemoizedSigninModelBean.class);

		JsonObject written = JsonParser.parseString(Files.readString(file.toPath()))
			.getAsJsonObject();
		JsonObject before = JsonParser.parseString(TODAYS_FILE).getAsJsonObject();
		for (String field : before.keySet())
		{
			assertEquals(before.get(field), written.get(field),
				"today's field " + field + " keeps its meaning");
		}
		assertTrue(written.has("choicesByDatabase"));
		assertFalse(before.has("choicesByDatabase"));
		assertEquals(before.size() + 1, written.size(), "nothing else is added: " + written);
	}

	@Test
	@DisplayName("with key files not remembered, the file read back holds no key file path and keeps the factors")
	void forgetKeyFiles_removesEveryKeyFilePath_andKeepsTheFactors() throws Exception
	{
		File file = new File(directory, "memoizedSignin.json");
		Files.writeString(file.toPath(), TODAYS_FILE);
		MemoizedSigninModelBean read = JsonFileStore.read(file, MemoizedSigninModelBean.class)
			.orElseThrow();
		read.setChoicesByDatabase(
			RememberedKeyFileSupport.remember(null, "/home/user/vaults/second.mcrdb",
				DatabaseSignInChoice.builder().keyFilePath("/home/user/keys/second-key.pem")
					.withKeyFile(true).withMasterPw(true).build()));
		JsonFileStore.write(read, file, MemoizedSigninModelBean.class);
		assertTrue(Files.readString(file.toPath()).contains("second-key.pem"), "the premise");

		assertTrue(RememberedKeyFileSupport.forgetKeyFiles(file));

		String text = Files.readString(file.toPath());
		assertFalse(text.contains("key.pem"), "no key file path anywhere in the file: " + text);
		MemoizedSigninModelBean back = JsonFileStore.read(file, MemoizedSigninModelBean.class)
			.orElseThrow();
		assertTrue(
			back.getSelectedKeyFilePath() == null || back.getSelectedKeyFilePath().isEmpty());
		assertEquals(java.util.List.of(""), back.getKeyFilePaths());
		DatabaseSignInChoice choice = back.getChoicesByDatabase()
			.get(Path.of("/home/user/vaults/second.mcrdb").toAbsolutePath().toString());
		assertEquals("", choice.getKeyFilePath());
		assertTrue(choice.isWithKeyFile() && choice.isWithMasterPw());
		assertEquals("/home/user/vaults/second.mcrdb", back.getSelectedApplicationFilePath(),
			"the database itself stays remembered");
		assertTrue(back.isWithKeyFile() && back.isWithMasterPw());
	}

	@Test
	@DisplayName("forgetting key files in a directory without a remembered sign-in writes nothing")
	void forgetKeyFiles_writesNothing_withoutAFile() throws Exception
	{
		File file = new File(directory, "memoizedSignin.json");

		assertFalse(RememberedKeyFileSupport.forgetKeyFiles(file));
		assertFalse(file.exists());
	}
}
