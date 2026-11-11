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

import static io.github.astrapi69.mystic.crypt.plugin.lethenon.Windows.textOf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.mystic.crypt.panel.dbtree.VaultEntryProperties;

/**
 * The backup assistant of the wallet window (#690), read through its model and the names of its
 * components: step 1 the words, step 2 three of them asked back with the choice to store them,
 * step 3 the sealed file to a place the person picks, the check of all 24 words, and the backup on
 * request through the wallet password. What the person typed and the words are overwritten when
 * the assistant ends or the vault locks.
 */
class LethenonBackupPanelBindingTest
{

	@TempDir
	Path directory;

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	private char[] password;

	private Wallet wallet;

	private UUID id;

	private final List<UUID> finished = new ArrayList<>();

	@BeforeEach
	void aVaultWithOneWallet()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
		password = ThrowawayPasswords.next();
		wallet = Wallet.create();
		id = vault.restore(wallet.phrase().toCharArray(), password.clone(), WalletNetwork.TESTNET);
	}

	private LethenonBackupPanel assistant()
	{
		return new LethenonBackupPanel(vault, Locale.ENGLISH, finished::add);
	}

	private String wordAt(final int position)
	{
		return wallet.phrase().split(" ")[position - 1];
	}

	/** Starts on request and passes step 1 */
	private LethenonBackupPanel atStepTwo()
	{
		LethenonBackupPanel panel = assistant();
		panel.startOnRequest(id);
		panel.getModelObject().setPassword(password.clone());
		panel.onUnsealConfirmed();
		panel.onWrittenDown();
		return panel;
	}

	private void answerRight(final LethenonBackupPanel panel)
	{
		int[] positions = panel.getModelObject().getPositions();
		panel.getModelObject().setAnswer1(wordAt(positions[0]));
		panel.getModelObject().setAnswer2(wordAt(positions[1]));
		panel.getModelObject().setAnswer3(wordAt(positions[2]));
	}

	@Test
	@DisplayName("on request the wallet password shows the 24 words; a wrong one is refused and shows nothing")
	void onRequest_theWalletPasswordShowsTheWords()
	{
		LethenonBackupPanel panel = assistant();
		panel.startOnRequest(id);
		assertEquals(BackupStep.UNSEAL, panel.getModelObject().getStep());

		panel.getModelObject().setPassword(ThrowawayPasswords.next());
		panel.onUnsealConfirmed();
		assertEquals(BackupStep.UNSEAL, panel.getModelObject().getStep());
		assertEquals("This password does not open the wallet. Type the wallet password again.",
			textOf(panel, "txtBackupRefusal"));
		assertTrue(textOf(panel, "lblWord1").isEmpty());

		panel.getModelObject().setPassword(password.clone());
		panel.onUnsealConfirmed();

		assertEquals(BackupStep.WORDS, panel.getModelObject().getStep());
		assertEquals("1. " + wordAt(1), textOf(panel, "lblWord1"));
		assertEquals("24. " + wordAt(24), textOf(panel, "lblWord24"));
	}

	@Test
	@DisplayName("step 2 asks three different positions, takes the board off the screen, and \"Back\" shows the words again")
	void stepTwo_asksThreePositions_andBackShowsTheWords()
	{
		LethenonBackupPanel panel = atStepTwo();

		assertEquals(BackupStep.QUESTION, panel.getModelObject().getStep());
		int[] positions = panel.getModelObject().getPositions();
		assertEquals(3, positions.length);
		assertTrue(positions[0] < positions[1] && positions[1] < positions[2], List.of(positions[0],
			positions[1], positions[2]).toString());
		assertEquals("Word " + positions[0], textOf(panel, "lblAnswer1"));
		assertTrue(textOf(panel, "lblWord1").isEmpty(), "the words are off the screen");

		panel.onQuestionBack();

		assertEquals(BackupStep.WORDS, panel.getModelObject().getStep());
		assertEquals("1. " + wordAt(1), textOf(panel, "lblWord1"));
	}

	@Test
	@DisplayName("a wrong word in step 2 is refused with its position and the step stays")
	void aWrongWord_isRefusedWithItsPosition()
	{
		LethenonBackupPanel panel = atStepTwo();
		answerRight(panel);
		int second = panel.getModelObject().getPositions()[1];
		panel.getModelObject().setAnswer2(wordAt(second).equals("abandon") ? "ability" : "abandon");

		panel.onQuestionConfirmed();

		assertEquals(BackupStep.QUESTION, panel.getModelObject().getStep());
		assertEquals("Word " + second + " is not right. Look at your note and try again.",
			textOf(panel, "txtBackupRefusal"));
	}

	@Test
	@DisplayName("the words shown again on request say that the password shows them again, not \"shown only now and stored nowhere\" (#803)")
	void theWordsShownAgainOnRequest_sayThePasswordShowsThemAgain()
	{
		LethenonBackupPanel panel = assistant();
		panel.startOnRequest(id);
		panel.getModelObject().setPassword(password.clone());

		panel.onUnsealConfirmed();

		List<String> texts = textsAnywhereIn(panel);
		assertTrue(texts.stream().anyMatch(
			text -> text.contains("The wallet password shows them again at any time")), texts.toString());
		assertFalse(texts.stream().anyMatch(text -> text.contains("shown only now")
			|| text.contains("stored nowhere")), texts.toString());
	}

	@Test
	@DisplayName("the words of a new wallet say they are not stored as words unless step 2 says so, and nothing claims \"only now\" (#803)")
	void theWordsOfANewWallet_sayTheyAreNotStoredUnlessChosen()
	{
		LethenonBackupPanel panel = assistant();

		panel.startWithNewWallet(vault.create(WalletNetwork.TESTNET, ThrowawayPasswords.next()));

		List<String> texts = textsAnywhereIn(panel);
		assertTrue(texts.stream().anyMatch(
			text -> text.contains("They are not stored as words, unless you choose so in step 2.")),
			texts.toString());
		assertFalse(texts.stream().anyMatch(text -> text.contains("shown only now")
			|| text.contains("stored nowhere")), texts.toString());
	}

	@Test
	@DisplayName("after \"Create the wallet\" step 1 says which entry the wallet is, that wallet.lethwf is attached and that the vault has to be saved (#818)")
	void afterCreate_stepOneSaysWhereTheWalletIs()
	{
		LethenonBackupPanel panel = assistant();

		panel.startWithNewWallet(vault.create(WalletNetwork.TESTNET, ThrowawayPasswords.next()));

		JTextArea place = Windows.component(panel, "txtWalletPlace", JTextArea.class);
		String title = vault.wallets().stream().filter(listed -> !listed.id().equals(id))
			.map(VaultWallet::title).findFirst().orElseThrow();
		assertTrue(place.isVisible(), "the hint does not show");
		assertEquals("The wallet is now the entry \"" + title + "\" of the vault, with the sealed "
			+ "file wallet.lethwf attached. Save the vault to keep it: until then it exists only in "
			+ "the vault that is open.", place.getText());
	}

	@Test
	@DisplayName("the hint after \"Create the wallet\" is German for a German user (#818)")
	void afterCreate_theHintIsGerman()
	{
		LethenonBackupPanel panel = new LethenonBackupPanel(vault, Locale.GERMAN, finished::add);

		panel.startWithNewWallet(vault.create(WalletNetwork.TESTNET, ThrowawayPasswords.next()));

		String text = Windows.component(panel, "txtWalletPlace", JTextArea.class).getText();
		assertTrue(text.startsWith("Die Wallet ist jetzt der Eintrag \""), text);
		assertTrue(text.contains("Speichere den Tresor, um sie zu behalten"), text);
	}

	@Test
	@DisplayName("a backup on request does not say where the wallet is, also after a created one was cancelled (#818)")
	void aBackupOnRequest_doesNotShowTheHint()
	{
		LethenonBackupPanel panel = assistant();
		panel.startWithNewWallet(vault.create(WalletNetwork.TESTNET, ThrowawayPasswords.next()));
		panel.onWordsCancelled();

		panel.startOnRequest(id);
		panel.getModelObject().setPassword(password.clone());
		panel.onUnsealConfirmed();

		JTextArea place = Windows.component(panel, "txtWalletPlace", JTextArea.class);
		assertFalse(place.isVisible(), "the hint of a new wallet shows for an existing one");
		assertEquals("", place.getText());
	}

	@Test
	@DisplayName("step 2 promises no removal of the words while nothing can remove them (#803)")
	void stepTwo_promisesNoRemoval()
	{
		LethenonBackupPanel panel = atStepTwo();

		assertFalse(textsAnywhereIn(panel).stream()
			.anyMatch(text -> text.contains("remove the words from the vault")));
	}

	/** The text of every text component of the assistant, named or not, shown or not */
	private static List<String> textsAnywhereIn(final Container container)
	{
		List<String> texts = new ArrayList<>();
		for (Component component : container.getComponents())
		{
			if (component instanceof JTextComponent text)
			{
				texts.add(text.getText());
			}
			if (component instanceof Container inner)
			{
				texts.addAll(textsAnywhereIn(inner));
			}
		}
		return texts;
	}

	@Test
	@DisplayName("the right three words lead to step 3, the words are overwritten, and by default they are not stored")
	void theRightWords_leadToStepThree_withoutStoringTheWords()
	{
		LethenonBackupPanel panel = atStepTwo();
		char[] shown = panel.getModelObject().getShownWords();
		answerRight(panel);
		assertFalse(panel.getModelObject().isStoreWords(), "storing the words is off by default");

		panel.onQuestionConfirmed();

		assertEquals(BackupStep.EXPORT, panel.getModelObject().getStep());
		assertArrayEquals(new char[shown.length], shown, "the words are overwritten after step 2");
		assertFalse(VaultEntryProperties.valuesOf(access.stored.get(0))
			.containsKey(WalletInVault.MNEMONIC));
	}

	@Test
	@DisplayName("with the box ticked the words are stored as a protected field of the entry")
	void tickingTheBox_storesTheWords()
	{
		LethenonBackupPanel panel = atStepTwo();
		answerRight(panel);
		panel.getModelObject().setStoreWords(true);

		panel.onQuestionConfirmed();

		Map<String, String> stored = VaultEntryProperties.valuesOf(access.stored.get(0));
		assertEquals(wallet.phrase(), stored.get(WalletInVault.MNEMONIC));
	}

	@Test
	@DisplayName("step 3 writes the sealed file only after the confirmation, shows its SHA-256 and never overwrites a file")
	void stepThree_writesTheFileAndShowsItsChecksum() throws Exception
	{
		LethenonBackupPanel panel = atStepTwo();
		answerRight(panel);
		panel.onQuestionConfirmed();
		Path target = directory.resolve("stick.lethwf");
		panel.getModelObject().setExportUnderstood(true);
		panel.getModelObject().setExportFile(target.toString());
		JPanel result = Windows.component(panel, "pnlExportResult", JPanel.class);
		assertFalse(result.isVisible(), "no result area before there is a result (G3)");

		panel.onExportConfirmed();

		assertTrue(result.isVisible());
		assertTrue(Files.exists(target));
		String checksum = panel.getModelObject().getChecksum();
		assertEquals(64, checksum.length(), checksum);
		assertEquals(checksum, textOf(panel, "txtChecksum"));

		panel.onExportConfirmed();

		assertEquals("There is already a file stick.lethwf at this place. It is not overwritten; "
			+ "choose another name or place.", textOf(panel, "txtBackupRefusal"));
	}

	private LethenonBackupPanel atStepThree()
	{
		LethenonBackupPanel panel = atStepTwo();
		answerRight(panel);
		panel.onQuestionConfirmed();
		return panel;
	}

	/** An assistant whose save dialog answers with the given file, null for Cancel */
	private LethenonBackupPanel atStepThreeSavingTo(final Path target, final List<File> asked)
	{
		LethenonBackupPanel panel = new LethenonBackupPanel(vault, Locale.ENGLISH, finished::add)
		{
			@Override
			Optional<File> chooseExportTarget()
			{
				asked.add(proposedExportFile());
				return Optional.ofNullable(target).map(Path::toFile);
			}
		};
		panel.startOnRequest(id);
		panel.getModelObject().setPassword(password.clone());
		panel.onUnsealConfirmed();
		panel.onWrittenDown();
		answerRight(panel);
		panel.onQuestionConfirmed();
		return panel;
	}

	@Test
	@DisplayName("step 3 writes nothing before the box \"Understood, write the file\" is ticked, and says so (#812)")
	void stepThree_writesNothingBeforeTheBoxIsTicked()
	{
		LethenonBackupPanel panel = atStepThree();
		Path target = directory.resolve("stick.lethwf");
		panel.getModelObject().setExportFile(target.toString());

		panel.onExportConfirmed();

		assertFalse(Files.exists(target), "the file was written with the box not ticked");
		assertEquals("Tick \"Understood, write the file\" first: the file will then be outside "
			+ "the vault.", textOf(panel, "txtBackupRefusal"));
	}

	@Test
	@DisplayName("the save dialog of step 3 proposes lethenon-<network>.lethwf in the home folder (#812)")
	void stepThree_proposesTheNetworkInTheName()
	{
		LethenonBackupPanel panel = atStepThree();

		assertEquals(new File(System.getProperty("user.home"), "lethenon-testnet.lethwf"),
			panel.proposedExportFile());
	}

	@Test
	@DisplayName("\"Choose location ...\" with the box ticked writes the file where it is put, and shows its SHA-256")
	void chooseLocation_writesTheFileWhereItIsPut()
	{
		Path target = directory.resolve("lethenon-testnet.lethwf");
		List<File> asked = new ArrayList<>();
		LethenonBackupPanel panel = atStepThreeSavingTo(target, asked);
		panel.getModelObject().setExportUnderstood(true);

		panel.onChooseExportFile();

		assertEquals(List.of(panel.proposedExportFile()), asked,
			"the save dialog did not open with the proposed name");
		assertTrue(Files.exists(target));
		assertEquals(target.toString(), panel.getModelObject().getExportFile());
		assertEquals(64, panel.getModelObject().getChecksum().length());
	}

	@Test
	@DisplayName("\"Choose location ...\" without the box ticked opens no save dialog and says why")
	void chooseLocation_withoutTheBox_opensNoDialog()
	{
		List<File> asked = new ArrayList<>();
		LethenonBackupPanel panel = atStepThreeSavingTo(directory.resolve("x.lethwf"), asked);

		panel.onChooseExportFile();

		assertEquals(List.of(), asked, "the save dialog opened with the box not ticked");
		assertEquals("Tick \"Understood, write the file\" first: the file will then be outside "
			+ "the vault.", textOf(panel, "txtBackupRefusal"));
	}

	@Test
	@DisplayName("Cancel in the save dialog of step 3 writes nothing")
	void cancelInTheSaveDialog_writesNothing() throws Exception
	{
		LethenonBackupPanel panel = atStepThreeSavingTo(null, new ArrayList<>());
		panel.getModelObject().setExportUnderstood(true);

		panel.onChooseExportFile();

		try (var written = Files.list(directory))
		{
			assertEquals(0, written.count(), "a file was written after Cancel");
		}
		assertEquals("", panel.getModelObject().getChecksum());
	}

	@Test
	@DisplayName("the box of step 3 is unticked again when the assistant starts over")
	void theBoxIsUntickedWhenTheAssistantStartsOver()
	{
		LethenonBackupPanel panel = atStepThree();
		panel.getModelObject().setExportUnderstood(true);

		panel.onDone();

		assertFalse(panel.getModelObject().isExportUnderstood());
		assertFalse(Windows.component(panel, "chkExportUnderstood", javax.swing.JCheckBox.class)
			.isSelected());
	}

	@Test
	@DisplayName("\"Done\" ends the assistant on the wallet, with nothing typed left behind")
	void done_endsTheAssistant()
	{
		LethenonBackupPanel panel = atStepTwo();
		answerRight(panel);
		panel.onQuestionConfirmed();

		panel.onDone();

		assertEquals(List.of(id), finished);
		assertEquals("", panel.getModelObject().getAnswer1());
		assertEquals(0, panel.getModelObject().getPassword().length);
	}

	@Test
	@DisplayName("the check of all 24 words says whether they match and records the day only when they do")
	void theCheck_saysWhetherTheWordsMatch()
	{
		LethenonBackupPanel panel = assistant();
		panel.startCheck(id);
		assertEquals(BackupStep.CHECK, panel.getModelObject().getStep());

		panel.getModelObject().setCheckWords(Wallet.create().phrase());
		panel.onCheckConfirmed();
		assertTrue(textOf(panel, "txtCheckResult").startsWith("The words do not match this wallet"),
			textOf(panel, "txtCheckResult"));
		assertEquals(BackupState.NOT_CHECKED, vault.backupStateOf(id, LocalDate.now()));

		panel.getModelObject().setCheckWords(wallet.phrase());
		panel.onCheckConfirmed();

		assertTrue(textOf(panel, "txtCheckResult").startsWith("These words match your wallet"),
			textOf(panel, "txtCheckResult"));
		assertEquals(BackupState.CURRENT, vault.backupStateOf(id, LocalDate.now()));
		assertEquals("", panel.getModelObject().getCheckWords(), "the typed words are cleared");
	}

	/**
	 * A check whose words are not this wallet's says so, says what to look for and that the day of
	 * the last check that matched stays, in both languages of the window (#804, draft 05)
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("theMismatchSentence")
	void aCheckThatDoesNotMatch_saysWhatToLookFor(final Locale locale, final String expected)
	{
		LethenonBackupPanel panel = new LethenonBackupPanel(vault, locale, finished::add);
		panel.startCheck(id);
		panel.getModelObject().setCheckWords(Wallet.create().phrase());

		panel.onCheckConfirmed();

		assertEquals(expected, textOf(panel, "txtCheckResult"));
	}

	static Stream<Arguments> theMismatchSentence()
	{
		return Stream.of(Arguments.of(Locale.ENGLISH,
			"The words do not match this wallet. One is wrong or two are swapped. The date of the "
				+ "last successful check stays."),
			Arguments.of(Locale.GERMAN,
				"Die Wörter passen nicht zu dieser Wallet. Eines ist falsch oder zwei sind vertauscht. "
					+ "Das Datum der letzten erfolgreichen Prüfung bleibt."));
	}

	@Test
	@DisplayName("a phrase pasted into any of the 24 fields of the check fills them all, and the check matches")
	void aPastedPhrase_isChecked()
	{
		LethenonBackupPanel panel = assistant();
		panel.startCheck(id);

		Windows.paste(panel, "txtCheckWords17", wallet.phrase());

		assertEquals(wordAt(1), textOf(panel, "txtCheckWords1"));
		panel.onCheckConfirmed();

		assertTrue(textOf(panel, "txtCheckResult").startsWith("These words match your wallet"),
			textOf(panel, "txtCheckResult"));
		for (int position = 1; position <= 24; position++)
		{
			assertEquals("", textOf(panel, "txtCheckWords" + position), "field " + position);
		}
	}

	@Test
	@DisplayName("the check names a word that is not in the word list as soon as its field is left")
	void theCheck_namesAnUnknownWord_whenItsFieldIsLeft()
	{
		LethenonBackupPanel panel = assistant();
		panel.startCheck(id);
		Windows.component(panel, "txtCheckWords3", JTextField.class).setText("zzz");

		Windows.leave(panel, "txtCheckWords3");

		assertEquals("Word 3, \"zzz\", is not in the word list. Check your note for a typing error.",
			textOf(panel, "txtBackupRefusal"));
		assertEquals(BackupState.NOT_CHECKED, vault.backupStateOf(id, LocalDate.now()));
	}

	@Test
	@DisplayName("a check with a mistyped word names the word and its position and records nothing")
	void aCheckWithAMistypedWord_namesIt()
	{
		List<String> words = new ArrayList<>(List.of(wallet.phrase().split(" ")));
		words.set(2, "zzz");
		LethenonBackupPanel panel = assistant();
		panel.startCheck(id);
		panel.getModelObject().setCheckWords(String.join(" ", words));

		panel.onCheckConfirmed();

		assertEquals("Word 3, \"zzz\", is not in the word list. Check your note for a typing error.",
			textOf(panel, "txtBackupRefusal"));
		assertEquals(BackupState.NOT_CHECKED, vault.backupStateOf(id, LocalDate.now()));
	}

	@Test
	@DisplayName("when the vault locks in step 2 the words and the answers are overwritten")
	void locking_overwritesTheWordsAndTheAnswers()
	{
		LethenonBackupPanel panel = atStepTwo();
		char[] shown = panel.getModelObject().getShownWords();
		answerRight(panel);

		panel.forget();

		assertArrayEquals(new char[shown.length], shown);
		assertEquals("", panel.getModelObject().getAnswer1());
		assertTrue(textOf(panel, "lblWord1").isEmpty());
	}

	@Test
	@DisplayName("every field, button and choice of the assistant explains itself")
	void everyComponent_hasATooltip()
	{
		assertEquals(List.of(), Windows.withoutATooltip(assistant()));
	}
}
