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

import java.awt.CardLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.danekja.java.util.function.serializable.SerializableConsumer;
import org.danekja.java.util.function.serializable.SerializableSupplier;

import io.github.astrapi69.lethenon.SeedPhrase;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.component.JMCheckBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The backup assistant of the wallet window (#690), as the decision record describes it: step 1
 * shows the 24 words once, step 2 asks three of them back at random positions and offers to store
 * them in the vault (off by default, the risk said plainly), step 3 writes the sealed wallet file to
 * a place the person picks and shows its SHA-256 as read back. Besides the steps: the check of all
 * 24 words, and the wallet password that opens an existing wallet for a backup on request.
 * <p>
 * UI over {@link LethenonVault} and {@link WalletInput}; the words stay in memory from step 1 until
 * step 2 is answered, because "Back" shows them again, and are overwritten then, when the assistant
 * is left, and when the vault locks.
 */
public class LethenonBackupPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** The number of columns the 24 words are shown in */
	private static final int WORD_COLUMNS = 4;

	private final transient LethenonVault vault;

	private final Locale locale;

	private final transient Consumer<UUID> finished;

	private final transient QuestionPositions questionPositions;

	private final LethenonBackupPanelModel modelObject = new LethenonBackupPanelModel();

	private final CardLayout cards = new CardLayout();

	private final JPanel pnlSteps = new JPanel(cards);

	private final List<JLabel> wordLabels = new ArrayList<>();

	private final List<JLabel> answerLabels = new ArrayList<>();

	private final List<JMTextField> answerFields = new ArrayList<>();

	private final JMPasswordField txtUnsealPassword = new JMPasswordField(34);

	private final JMCheckBox chkStoreWords = new JMCheckBox();

	private final JMTextField txtExportFile = new JMTextField(34);

	private final JTextArea txtChecksum = LethenonWalletPanel.message("txtChecksum", false);

	/** The result of step 3, shown only once there is one (README defect G3) */
	private final JPanel pnlExportResult = new JPanel(ToolForm.newLayout());

	private final JMTextArea txtCheckWords = new JMTextArea(4, 40);

	private final JTextArea txtCheckResult = LethenonWalletPanel.message("txtCheckResult", true);

	private final JTextArea txtBackupRefusal = LethenonWalletPanel.message("txtBackupRefusal", true);

	/** The wallet the assistant works on */
	private transient UUID walletId;

	/** A wallet just created, taken back when its creation is cancelled while the words show */
	private transient NewWallet pending;

	/**
	 * Instantiates a new {@link LethenonBackupPanel}
	 *
	 * @param vault
	 *            the plugin's view of the vault
	 * @param locale
	 *            the language of the window
	 * @param finished
	 *            told when the assistant ends: the wallet's identifier, or null when a new wallet
	 *            was taken back
	 */
	LethenonBackupPanel(final LethenonVault vault, final Locale locale,
		final Consumer<UUID> finished)
	{
		super(new MigLayout("fillx, insets 0, gap 0, wrap 1", "[grow,fill]", "[][]push"));
		this.vault = vault;
		this.locale = locale;
		this.finished = finished;
		SecureRandom random = new SecureRandom();
		this.questionPositions = new QuestionPositions(random::nextInt);
		pnlSteps.add(unsealStep(), BackupStep.UNSEAL.name());
		pnlSteps.add(wordsStep(), BackupStep.WORDS.name());
		pnlSteps.add(questionStep(), BackupStep.QUESTION.name());
		pnlSteps.add(exportStep(), BackupStep.EXPORT.name());
		pnlSteps.add(checkStep(), BackupStep.CHECK.name());
		add(pnlSteps, "wmin 0");
		txtBackupRefusal.setToolTipText(text("lethenon.wallet.tooltip.refusal",
			"why the last action did not happen, and what to do"));
		txtBackupRefusal.setVisible(false);
		add(txtBackupRefusal, "wmin 0, gapleft 12, gapright 12, hidemode 3");
	}

	/**
	 * Gets the assistant's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonBackupPanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Starts at step 1 with the words of a wallet just created; cancelling there takes the wallet
	 * out of the vault again
	 *
	 * @param created
	 *            the new wallet
	 */
	void startWithNewWallet(final NewWallet created)
	{
		walletId = created.id();
		pending = created;
		showTheWords(created.words());
	}

	/**
	 * Starts a backup of an existing wallet: its password first
	 *
	 * @param id
	 *            the wallet's entry identifier
	 */
	void startOnRequest(final UUID id)
	{
		walletId = id;
		show(BackupStep.UNSEAL);
	}

	/**
	 * Starts the check of all 24 words
	 *
	 * @param id
	 *            the wallet's entry identifier
	 */
	void startCheck(final UUID id)
	{
		walletId = id;
		show(BackupStep.CHECK);
	}

	/** Opens the wallet with the typed password and shows its words */
	void onUnsealConfirmed()
	{
		try
		{
			showTheWords(vault.wordsOf(walletId, modelObject.getPassword()));
		}
		catch (final SecurityException wrongPassword)
		{
			refuse(text("lethenon.backup.unseal.refused",
				"This password does not open the wallet. Type the wallet password again."));
		}
		finally
		{
			txtUnsealPassword.setText("");
			modelObject.setPassword(new char[0]);
		}
	}

	/** Step 1 done: the words leave the screen and three of them are asked */
	void onWrittenDown()
	{
		clearTheBoard();
		int[] positions = questionPositions.next();
		modelObject.setPositions(positions);
		for (int index = 0; index < positions.length; index++)
		{
			answerLabels.get(index).setText(LethenonMessages.format("lethenon.backup.question.field",
				"Word {0}", locale, positions[index]));
			answerFields.get(index).setText("");
		}
		show(BackupStep.QUESTION);
	}

	/** Back from step 2: the words are shown again */
	void onQuestionBack()
	{
		fillTheBoard(modelObject.getShownWords());
		show(BackupStep.WORDS);
	}

	/**
	 * Step 2 answered: a wrong or unknown word is refused with its position; the right three store
	 * the words when the box is ticked, overwrite them and lead to step 3
	 */
	void onQuestionConfirmed()
	{
		List<String> words = List.of(new String(modelObject.getShownWords()).split(" "));
		Optional<Refusal> refused = WalletInput.answers(words, modelObject.getPositions(),
			modelObject.getAnswers());
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		try
		{
			vault.storeWords(walletId, modelObject.getShownWords(), modelObject.isStoreWords()
				? WordsChoice.STORE_IN_VAULT
				: WordsChoice.KEEP_OUT);
		}
		catch (final VaultAccessRefusedException notWritable)
		{
			refuse(notWritable.getMessage());
			return;
		}
		pending = null;
		modelObject.setShownWords(new char[0]);
		clearTheAnswers();
		show(BackupStep.EXPORT);
	}

	/** Step 3: writes the sealed file after the warning the button itself confirms */
	void onExportConfirmed()
	{
		Optional<Refusal> refused = WalletInput.walletFile(modelObject.getExportFile());
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		Path target = Path.of(modelObject.getExportFile().trim());
		try
		{
			String checksum = vault.export(walletId, target, ExportConsent.GIVEN_AFTER_WARNING);
			modelObject.setChecksum(checksum);
			txtChecksum.setText(checksum);
			pnlExportResult.setVisible(true);
			refuse("");
		}
		catch (final FileAlreadyExistsException exists)
		{
			refuse(LethenonMessages.format("lethenon.backup.export.exists",
				"There is already a file {0} at this place. It is not overwritten; choose another "
					+ "name or place.",
				locale, String.valueOf(target.getFileName())));
		}
		catch (final IOException notWritten)
		{
			refuse(LethenonMessages.format("lethenon.backup.export.failed",
				"{0} could not be written: {1}", locale, target.toString(),
				notWritten.getMessage()));
		}
	}

	/** Ends the assistant on the wallet */
	void onDone()
	{
		UUID done = walletId;
		forget();
		finished.accept(done);
	}

	/** Leaves step 1: a wallet just created is taken out of the vault again */
	void onWordsCancelled()
	{
		UUID kept = pending == null ? walletId : null;
		forget();
		finished.accept(kept);
	}

	/** Checks all 24 typed words against the wallet's address */
	void onCheckConfirmed()
	{
		String typed = modelObject.getCheckWords();
		Optional<Refusal> refused = WalletInput.wordsToCheck(typed);
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		boolean matches = vault.checkBackup(walletId, SeedPhrase.parse(typed), LocalDate.now());
		txtCheckWords.setText("");
		modelObject.setCheckWords("");
		String result = matches
			? LethenonMessages.format("lethenon.backup.check.match",
				"These words match your wallet. The check is recorded; the next reminder comes in "
					+ "{0} days.",
				locale, BackupReminder.INTERVAL_DAYS)
			: text("lethenon.backup.check.mismatch",
				"These words do not match the wallet in the vault. Compare them with your note, in "
					+ "order, and check again.");
		modelObject.setCheckResult(result);
		txtCheckResult.setText(result);
		refuse("");
	}

	/**
	 * Overwrites what the assistant holds: the words, the password, the answers; a new wallet whose
	 * words were never confirmed is taken back while the vault can still be written
	 */
	void forget()
	{
		if (pending != null && vault.isAvailable())
		{
			vault.discard(pending);
		}
		else if (pending != null)
		{
			pending.close();
		}
		pending = null;
		clearTheBoard();
		clearTheAnswers();
		txtUnsealPassword.setText("");
		txtCheckWords.setText("");
		txtExportFile.setText("");
		txtChecksum.setText("");
		pnlExportResult.setVisible(false);
		txtCheckResult.setText("");
		modelObject.clear();
		refuse("");
	}

	private void showTheWords(final char[] words)
	{
		modelObject.setShownWords(words);
		fillTheBoard(words);
		show(BackupStep.WORDS);
	}

	private void fillTheBoard(final char[] words)
	{
		String[] split = new String(words).split(" ");
		for (int position = 1; position <= wordLabels.size(); position++)
		{
			String word = position <= split.length ? split[position - 1] : "";
			wordLabels.get(position - 1).setText(position + ". " + word);
		}
	}

	private void clearTheBoard()
	{
		wordLabels.forEach(label -> label.setText(""));
	}

	private void clearTheAnswers()
	{
		answerFields.forEach(field -> field.setText(""));
		modelObject.setAnswer1("");
		modelObject.setAnswer2("");
		modelObject.setAnswer3("");
	}

	private void show(final BackupStep step)
	{
		modelObject.setStep(step);
		refuse("");
		cards.show(pnlSteps, step.name());
	}

	private void refuse(final String reason)
	{
		modelObject.setRefusal(reason);
		txtBackupRefusal.setText(reason);
		txtBackupRefusal.setVisible(!reason.isEmpty());
		revalidate();
	}

	private JPanel unsealStep()
	{
		JPanel step = new JPanel(ToolForm.newLayout());
		step.add(LethenonWalletPanel.title("lblUnsealTitle",
			text("lethenon.backup.unseal.title", "Back up this wallet")), ToolForm.WIDE);
		step.add(ToolForm.intro(text("lethenon.backup.unseal.text",
			"The wallet password opens the wallet in memory, so that its 24 words can be shown "
				+ "once more. Nothing is written.")),
			ToolForm.INTRO_ROW);
		txtUnsealPassword.setName("txtUnsealPassword");
		txtUnsealPassword.setToolTipText(text("lethenon.backup.tooltip.unseal.password",
			"the password this wallet is sealed with in the vault"));
		txtUnsealPassword.setPropertyModel(
			LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		step.add(new JLabel(text("lethenon.wallet.label.password", "Wallet password:")));
		step.add(txtUnsealPassword, ToolForm.FIELD);
		step.add(ToolForm.buttons(
			LethenonSwing.button("btnUnsealConfirmed",
				text("lethenon.backup.unseal.button", "Show the words"),
				event -> onUnsealConfirmed(),
				text("lethenon.backup.tooltip.unseal.button",
					"open the wallet in memory and show its 24 words once")),
			endButton("btnUnsealCancelled")), ToolForm.BUTTON_ROW);
		return step;
	}

	private JPanel wordsStep()
	{
		JPanel step = new JPanel(ToolForm.newLayout());
		step.add(LethenonWalletPanel.title("lblWordsTitle", text("lethenon.backup.words.title",
			"Backup, step 1 of 3: your 24 words")), ToolForm.WIDE);
		JLabel heading = new JLabel(
			text("lethenon.backup.words.heading", "Write these 24 words down on paper."));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD));
		step.add(heading, ToolForm.WIDE);
		step.add(ToolForm.intro(text("lethenon.backup.words.explanation",
			"With them you restore the wallet on any computer, even if the vault and the computer "
				+ "are lost. They are shown only now and stored nowhere.")),
			ToolForm.INTRO_ROW);
		step.add(wordBoard(), ToolForm.WIDE);
		JTextArea warning = LethenonWalletPanel.message("txtWordsWarning", true);
		warning.setText(text("lethenon.backup.words.warning",
			"No photo, no file, not to the clipboard. Whoever has the words has the wallet."));
		warning.setToolTipText(warning.getText());
		step.add(warning, ToolForm.WIDE + ", wmin 0");
		step.add(ToolForm.buttons(
			LethenonSwing.button("btnWrittenDown",
				text("lethenon.backup.words.button.next", "I have written them down"),
				event -> onWrittenDown(),
				text("lethenon.wallet.tooltip.written.down",
					"the words are taken off the screen and overwritten")),
			LethenonSwing.button("btnWordsCancelled",
				text("lethenon.backup.words.button.back", "Cancel"), event -> onWordsCancelled(),
				text("lethenon.wallet.tooltip.words.cancelled",
					"a new wallet is taken out of the vault again; nothing is kept"))),
			ToolForm.BUTTON_ROW);
		return step;
	}

	private JPanel wordBoard()
	{
		JPanel board = new JPanel(new MigLayout("insets 8, gap 24 4, wrap " + WORD_COLUMNS,
			"[150!]".repeat(WORD_COLUMNS)));
		board.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground")));
		Font wordFont = new JLabel().getFont().deriveFont(Font.BOLD);
		for (int position = 1; position <= BackupCheck.WORDS; position++)
		{
			JLabel word = new JLabel("");
			word.setName("lblWord" + position);
			word.setFont(wordFont);
			wordLabels.add(word);
		}
		int rows = BackupCheck.WORDS / WORD_COLUMNS;
		for (int row = 0; row < rows; row++)
		{
			for (int column = 0; column < WORD_COLUMNS; column++)
			{
				board.add(wordLabels.get(column * rows + row));
			}
		}
		return board;
	}

	private JPanel questionStep()
	{
		JPanel step = new JPanel(ToolForm.newLayout());
		step.add(LethenonWalletPanel.title("lblQuestionTitle", text("lethenon.backup.question.title",
			"Backup, step 2 of 3: three words from your note")), ToolForm.WIDE);
		step.add(ToolForm.intro(text("lethenon.backup.question.explanation",
			"The words are no longer on the screen. Type the words at these three positions from "
				+ "your note. The positions change every time.")),
			ToolForm.INTRO_ROW);
		addAnswer(step, 1, modelObject::getAnswer1, modelObject::setAnswer1);
		addAnswer(step, 2, modelObject::getAnswer2, modelObject::setAnswer2);
		addAnswer(step, 3, modelObject::getAnswer3, modelObject::setAnswer3);
		addTheChoiceToStoreTheWords(step);
		step.add(ToolForm.buttons(
			LethenonSwing.button("btnQuestionBack",
				text("lethenon.backup.question.button.back", "Back"), event -> onQuestionBack(),
				text("lethenon.backup.tooltip.question.back", "show the 24 words again")),
			LethenonSwing.button("btnQuestionConfirmed",
				text("lethenon.backup.question.button.next", "Next"),
				event -> onQuestionConfirmed(),
				text("lethenon.backup.tooltip.question.next",
					"compare the three words with the wallet's words"))),
			ToolForm.BUTTON_ROW);
		return step;
	}

	private void addAnswer(final JPanel step, final int number,
		final SerializableSupplier<String> getter, final SerializableConsumer<String> setter)
	{
		JLabel label = new JLabel("");
		label.setName("lblAnswer" + number);
		JMTextField field = new JMTextField(20);
		field.setName("txtAnswer" + number);
		field.setToolTipText(text("lethenon.backup.tooltip.answer",
			"the word at this position in your note"));
		field.setPropertyModel(LambdaModel.of(getter, setter));
		answerLabels.add(label);
		answerFields.add(field);
		step.add(label);
		step.add(field, "alignx left, width 200!");
	}

	private void addTheChoiceToStoreTheWords(final JPanel step)
	{
		JLabel question = new JLabel(text("lethenon.backup.store.question",
			"Also store the words in the vault? Default: no."));
		question.setFont(question.getFont().deriveFont(Font.BOLD));
		step.add(question, ToolForm.WIDE + ", gaptop 12");
		chkStoreWords.setName("chkStoreWords");
		chkStoreWords.setText(text("lethenon.backup.store.checkbox",
			"Yes, also store the 24 words in the vault, as a protected field."));
		chkStoreWords.setToolTipText(text("lethenon.backup.tooltip.store",
			"off by default; read the risk below before you tick it"));
		chkStoreWords.setPropertyModel(
			LambdaModel.of(modelObject::isStoreWords, modelObject::setStoreWords));
		step.add(chkStoreWords, ToolForm.WIDE);
		JTextArea risk = LethenonWalletPanel.message("txtStoreRisk", false);
		risk.setText(text("lethenon.backup.store.risk",
			"Then anyone who can open your vault has the whole wallet, even without the wallet "
				+ "password. If the vault is stolen while open, or together with its master "
				+ "password, the money is gone. For a wallet of real value: keep the paper note as "
				+ "well.")
			+ " " + text("lethenon.backup.store.removal",
				"You can remove the words from the vault at any time."));
		risk.setToolTipText(text("lethenon.backup.tooltip.store.risk",
			"what storing the words in the vault means"));
		step.add(risk, ToolForm.WIDE + ", wmin 0");
	}

	private JPanel exportStep()
	{
		JPanel step = new JPanel(ToolForm.newLayout());
		step.add(LethenonWalletPanel.title("lblExportTitle", text("lethenon.backup.export.title",
			"Backup, step 3 of 3: the wallet file on a stick")), ToolForm.WIDE);
		JTextArea warning = LethenonWalletPanel.message("txtExportWarning", true);
		warning.setText(text("lethenon.backup.export.warning",
			"The file will then be outside the vault. It is sealed: the wallet password opens it, "
				+ "and the 24 words are not in it. Whoever has the file and the password has the "
				+ "wallet."));
		warning.setToolTipText(warning.getText());
		step.add(warning, ToolForm.WIDE + ", wmin 0");
		txtExportFile.setName("txtExportFile");
		txtExportFile.setToolTipText(text("lethenon.backup.tooltip.export.file",
			"where the sealed file goes, for example a USB stick; an existing file is never "
				+ "overwritten"));
		txtExportFile.setPropertyModel(
			LambdaModel.of(modelObject::getExportFile, modelObject::setExportFile));
		step.add(new JLabel(text("lethenon.backup.label.export.file", "File:")));
		step.add(txtExportFile, "growx, split 2");
		step.add(LethenonSwing.button("btnBrowseExportFile", "...", event -> onBrowseExportFile(),
			text("lethenon.backup.tooltip.browse.export.file", "pick the place and the name")),
			"alignx left, width pref!");
		step.add(ToolForm.buttons(LethenonSwing.button("btnExportConfirmed",
			text("lethenon.backup.export.confirm", "Understood, write the file"),
			event -> onExportConfirmed(),
			text("lethenon.backup.tooltip.export.confirm",
				"write the sealed file, read it back and show its SHA-256"))),
			ToolForm.BUTTON_ROW);
		pnlExportResult.setName("pnlExportResult");
		pnlExportResult.add(new JLabel(text("lethenon.backup.export.result",
			"Written and read back. SHA-256 of the file:")), ToolForm.WIDE);
		txtChecksum.setToolTipText(text("lethenon.backup.export.hint",
			"Compare this value with the copy on the stick, on another computer."));
		txtChecksum.setFont(new Font(Font.MONOSPACED, Font.PLAIN, txtChecksum.getFont().getSize()));
		pnlExportResult.add(txtChecksum, ToolForm.WIDE + ", wmin 0");
		pnlExportResult.add(new JLabel(text("lethenon.backup.export.hint",
			"Compare this value with the copy on the stick, on another computer.")), ToolForm.WIDE);
		pnlExportResult.setVisible(false);
		step.add(pnlExportResult, ToolForm.WIDE + ", hidemode 3");
		step.add(ToolForm.buttons(endButton("btnDone")), ToolForm.BUTTON_ROW);
		return step;
	}

	private JPanel checkStep()
	{
		JPanel step = new JPanel(ToolForm.newLayout());
		step.add(LethenonWalletPanel.title("lblCheckTitle",
			text("lethenon.backup.check.title", "Check your backup")), ToolForm.WIDE);
		step.add(ToolForm.intro(text("lethenon.backup.check.text",
			"Type the 24 words from your note. They are compared with the address of the wallet "
				+ "in the vault; nothing is written but the day of a check that matches.")),
			ToolForm.INTRO_ROW);
		txtCheckWords.setName("txtCheckWords");
		txtCheckWords.setLineWrap(true);
		txtCheckWords.setWrapStyleWord(true);
		txtCheckWords.setToolTipText(text("lethenon.wallet.tooltip.words",
			"the 24 words from your note, in order, separated by spaces"));
		txtCheckWords.setPropertyModel(
			LambdaModel.of(modelObject::getCheckWords, modelObject::setCheckWords));
		step.add(new JLabel(text("lethenon.wallet.label.words", "The 24 words:")));
		step.add(ToolForm.scrolled(txtCheckWords), ToolForm.FIELD);
		step.add(ToolForm.buttons(
			LethenonSwing.button("btnCheckConfirmed",
				text("lethenon.backup.check.button", "Check"), event -> onCheckConfirmed(),
				text("lethenon.backup.tooltip.check",
					"derive the address from the words and compare it with the wallet's")),
			endButton("btnCheckDone")), ToolForm.BUTTON_ROW);
		txtCheckResult.setToolTipText(text("lethenon.backup.tooltip.check.result",
			"whether the words you typed are the words of this wallet"));
		step.add(txtCheckResult, ToolForm.WIDE + ", wmin 0");
		return step;
	}

	private JButton endButton(final String name)
	{
		return LethenonSwing.button(name, text("lethenon.backup.button.done", "Done"),
			event -> onDone(),
			text("lethenon.backup.tooltip.done", "back to the wallet; nothing typed is kept"));
	}

	private void onBrowseExportFile()
	{
		JFileChooser chooser = new JFileChooser();
		if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			txtExportFile.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
