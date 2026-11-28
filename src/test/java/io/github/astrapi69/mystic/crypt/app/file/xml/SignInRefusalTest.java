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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.Messages;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * What the sign-in shows and logs for a failure: a refusal of the reader as it says, any other
 * failure in the bundle words for a sign-in that could not be completed, also one whose text has
 * the form of a refusal, and in the log every failure by the types along its chain
 */
class SignInRefusalTest
{

	/** A text the messages of a failure carry, made per run */
	private static final String TEXT = "text-" + UUID.randomUUID();

	private static final String NOT_COMPLETED_TITLE = "dialog.signin.failed.title";

	private static final String NOT_COMPLETED_MESSAGE = "dialog.signin.failed.message";

	@TempDir
	File directory;

	@BeforeAll
	static void registerBouncyCastle()
	{
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
		{
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Test
	@DisplayName("a refusal of the reader is shown as its title and its dialog say, and logged by its types")
	void aRefusalOfTheReader_isShownAsItSays_andLoggedByItsTypes()
	{
		String html = SignInRefusal.asTheDialogShowsIt("Database not opened", "the words");
		SignInRefusedException refusal = new SignInRefusedException("Database not opened", html,
			new IllegalArgumentException(TEXT));

		SignInRefusal shown = SignInRefusal.of(refusal);

		assertEquals("Database not opened", shown.getTitle());
		assertEquals(html, shown.getHtml());
		assertEquals(List.of(SignInRefusedException.class.getName(),
			IllegalArgumentException.class.getName()), messagesOf(shown.getToldByItsTypes()));
		assertFalse(printed(shown.getToldByItsTypes()).contains(TEXT), "logged by its types");
	}

	@Test
	@DisplayName("the reader refuses a wrong password with the type the sign-in knows a refusal by, and it is shown as it says")
	void aWrongPassword_isRefusedByTheReader_withTheTypeOfARefusal()
	{
		File vault = SampleVaults.save(SampleVaults.aVaultWith(entry -> {
		}), directory, TestPasswords.throwawayChars());
		MasterPwFileModelBean wrongPassword = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath())
			.masterPw(TestPasswords.throwawayChars()).withMasterPw(true).withKeyFile(false)
			.minPasswordLength(6).build();
		RuntimeException refusal = assertThrows(RuntimeException.class,
			() -> ApplicationXmlFileReader.read(wrongPassword));

		SignInRefusal shown = SignInRefusal.of(refusal);

		assertEquals("Authentication with Password", shown.getTitle());
		assertEquals(SignInRefusal.asTheDialogShowsIt("Authentication with Password",
			"Wrong master password."), shown.getHtml());
		assertEquals(SignInRefusedException.class.getName(),
			messagesOf(shown.getToldByItsTypes()).get(0), "logged by its types");
		assertEquals(SignInRefusedException.class, refusal.getClass(),
			"the reader refuses with the type the sign-in knows a refusal by");
	}

	static Stream<Arguments> failuresThatAreNoRefusalOfTheReader()
	{
		return Stream.of(
			Arguments.of("an exception of the take-over", new NullPointerException(TEXT),
				List.of(NullPointerException.class.getName())),
			Arguments.of("an exception of the take-over whose text has the form of a refusal",
				new IllegalStateException("a title " + TEXT + "::a dialog " + TEXT),
				List.of(IllegalStateException.class.getName())),
			Arguments.of("an error the read ended with whose text has the form of a refusal",
				new ExecutionException(new LinkageError("a title " + TEXT + "::a dialog " + TEXT)),
				List.of(ExecutionException.class.getName(), LinkageError.class.getName())),
			Arguments.of("a stack overflow the read ended with",
				new ExecutionException(new StackOverflowError(TEXT)),
				List.of(ExecutionException.class.getName(), StackOverflowError.class.getName())),
			Arguments.of("a class the read could not link",
				new ExecutionException(new LinkageError(TEXT)),
				List.of(ExecutionException.class.getName(), LinkageError.class.getName())),
			Arguments.of("a failure without a message", new IllegalStateException(),
				List.of(IllegalStateException.class.getName())));
	}

	@ParameterizedTest(name = "{0}: shown in the words of the bundle, and logged by its types")
	@MethodSource("failuresThatAreNoRefusalOfTheReader")
	void aFailureThatIsNoRefusalOfTheReader_isShownInTheWordsOfTheBundle(final String caseName,
		final Exception failure, final List<String> types)
	{
		SignInRefusal shown = SignInRefusal.of(failure);

		String title = Messages.getString(NOT_COMPLETED_TITLE, "");
		assertEquals(title, shown.getTitle());
		assertEquals(
			SignInRefusal.asTheDialogShowsIt(title, Messages.getString(NOT_COMPLETED_MESSAGE, "")),
			shown.getHtml());
		assertFalse(shown.getTitle().contains(TEXT) || shown.getHtml().contains(TEXT),
			"nothing of the failure is shown");
		assertEquals(types, messagesOf(shown.getToldByItsTypes()));
		assertFalse(printed(shown.getToldByItsTypes()).contains(TEXT), "logged by its types");
	}

	@Test
	@DisplayName("the words for a sign-in that could not be completed, and for a database of the key file alone, read in English and in German")
	void theNewWordsOfTheSignIn_inBothLanguages()
	{
		ResourceBundle english = ResourceBundle.getBundle("ui.messages", Locale.ROOT);
		ResourceBundle german = ResourceBundle.getBundle("ui.messages", Locale.GERMAN);

		assertEquals("Database not opened", english.getString(NOT_COMPLETED_TITLE));
		assertEquals("The sign-in could not be completed. The database was not opened, and the "
			+ "file is unchanged.", english.getString(NOT_COMPLETED_MESSAGE));
		assertEquals("Datenbank nicht geöffnet", german.getString(NOT_COMPLETED_TITLE));
		assertEquals("Die Anmeldung ließ sich nicht abschließen. Die Datenbank wurde nicht "
			+ "geöffnet, die Datei ist unverändert.", german.getString(NOT_COMPLETED_MESSAGE));
		assertEquals(
			"This database opens with its key file alone, without a master password. It "
				+ "was not opened, and the file is unchanged.",
			english.getString("dialog.vault.key.file.alone.message"));
		assertEquals(
			"Diese Datenbank öffnet sich mit ihrer Schlüsseldatei allein, ohne "
				+ "Master-Passwort. Sie wurde nicht geöffnet, die Datei ist unverändert.",
			german.getString("dialog.vault.key.file.alone.message"));
		assertTrue(german.getString("dialog.vault.key.file.alone.title").contains("geöffnet"));
	}

	private static List<String> messagesOf(final Throwable thrown)
	{
		List<String> messages = new ArrayList<>();
		for (Throwable link = thrown; link != null; link = link.getCause())
		{
			messages.add(link.getMessage());
		}
		return messages;
	}

	private static String printed(final Throwable thrown)
	{
		StringWriter printed = new StringWriter();
		thrown.printStackTrace(new PrintWriter(printed));
		return printed.toString();
	}
}
