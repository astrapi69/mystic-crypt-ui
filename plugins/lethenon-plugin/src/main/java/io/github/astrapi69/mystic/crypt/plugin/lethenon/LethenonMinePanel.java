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

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import io.github.astrapi69.lethenon.ChainRejected;
import io.github.astrapi69.lethenon.Genesis;
import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;

/**
 * Mines a pun (lethenon#2, milestone 5): the next block of the chain file, carrying the transfers
 * that wait next to it and paying the wallet's Ed25519 account - what lethenon's {@code mine} does
 * on the command line.
 * <p>
 * {@link MiningSupport} mines and replays the extended chain before it writes anything. Mining runs
 * on the event dispatch thread, as the other tools of this plugin do, so the window does not answer
 * while it mines: at the minimum difficulty that is a fraction of a second, every further bit of
 * difficulty doubles the expected attempts, and the attempts are capped at the command line's
 * default. The password is used once: wiped by the support class, cleared from the field
 * afterwards, whether a block was mined or not.
 * <p>
 * Where the chain file does not exist yet, the block is a genesis block, and the window asks which
 * chain it starts: the test network unless the main chain is chosen (#518). Before that choice it
 * shows "Before you start" (#540), what a public start of the main chain runs into. Once the file
 * exists its genesis block decides, and the choice is closed and the view hidden with it.
 * <p>
 * It answers in fields a child can read (#604): first one sentence in a larger font, "Block 3 is
 * yours: it paid you 1,000.00000000 LETH.", then the block's number, what it paid, the transfers
 * it carries and the pun as it went into the block. The chain, the account paid, what the replay
 * checked and all the money there is are behind "Show details", closed at first.
 */
public class LethenonMinePanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	/** As many variations of the pun as lethenon's command line tries by default */
	static final long ATTEMPTS = 10_000_000L;

	/** A field that shares its cell with the button that fills it from a file chooser */
	private static final String WITH_BUTTON = "growx, split 2";

	/** Something that keeps its own width instead of growing with the cell */
	private static final String OWN_WIDTH = "alignx left, width pref!";

	/** Both columns, and no room kept for it while it is hidden */
	private static final String WIDE_HIDDEN_WITHOUT_ROOM = ToolForm.WIDE + ", hidemode 3";

	private final LethenonMinePanelModel modelObject = new LethenonMinePanelModel();

	/** Whom to ask before a chain file of lethenon 0.5.0 or earlier is converted (#581) */
	private final ChainConversionConsent consent;

	private final JMTextField txtChainFile = new JMTextField(34);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMPasswordField txtPassword = new JMPasswordField(34);

	private final JMComboBox<ChainKind, ComboBoxModel<ChainKind>> cbxChainKind = new JMComboBox<>(
		new EnumComboBoxModel<>(ChainKind.class, ChainKind.TEST_NETWORK, Set.of()));

	private final LethenonBeforeYouStartPanel pnlBeforeYouStart = new LethenonBeforeYouStartPanel();

	private final JMTextField txtPun = new JMTextField(34);

	/**
	 * The language of the window; the field initializers below read it, so it has its value before
	 * the constructor sets the one it was given
	 */
	private Locale locale = Locale.getDefault();

	/** The wallet the window signs with: the vault's, or a wallet file (#706) */
	private final transient WalletRow walletRow;

	private final JMTextArea txtAnswer = AnswerFields.answerArea(text("lethenon.mine.tooltip.answer",
		"what your block did, in one sentence; the details are below"));

	private final JMTextField txtBlock = AnswerFields.readOnly("txtBlock",
		text("lethenon.mine.tooltip.block",
			"the place of your block in the chain; the first block of a chain is block 0"));

	private final JMTextField txtPaid = AnswerFields.readOnly("txtPaid",
		text("lethenon.mine.tooltip.paid",
			"what your block added to your Ed25519 account: the reward for finding it and the fees "
				+ "of the transfers it carries; see it in the window Show a Balance"));

	private final JMTextField txtCarried = AnswerFields.readOnly("txtCarried",
		text("lethenon.mine.tooltip.carried",
			"transfers that were waiting next to the chain file; they are in your block now, so "
				+ "they count"));

	private final JMTextField txtMinedPun = AnswerFields.readOnly("txtMinedPun",
		text("lethenon.mine.tooltip.mined.pun",
			"your words as they went into the block; the number at the end was counted up until "
				+ "the block was hard enough to find"));

	private final JButton btnDetails;

	private final JPanel pnlDetails = new JPanel(ToolForm.newLayout());

	private final JMTextField txtChain = AnswerFields.readOnly("txtChain",
		text("lethenon.mine.tooltip.chain",
			"the name of the chain your block belongs to; a test chain and the main chain never mix"));

	private final JMTextField txtPaidTo = AnswerFields.readOnly("txtPaidTo",
		text("lethenon.mine.tooltip.paid.to",
			"the account your block pays: your Ed25519 account, or nobody for the first block of "
				+ "a chain"));

	private final JMTextField txtChainChecked = AnswerFields.readOnly("txtChainChecked",
		AnswerFields.chainCheckedTooltip(locale));

	private final JMTextField txtSupply = AnswerFields.readOnly("txtSupply",
		AnswerFields.supplyTooltip(locale));

	private final JLabel lblResult = new JLabel(" ");

	/** Where a failure shows, in the pattern of draft 07 (#710) */
	private final transient FailureView failureView;

	/**
	 * Instantiates a new {@link LethenonMinePanel}, prefilled with the chain file from the plugin's
	 * settings and lethenon's default pun
	 */
	public LethenonMinePanel()
	{
		this(ChainConversionConsent.ASK);
	}

	/**
	 * Instantiates a new {@link LethenonMinePanel} that asks through the given consent before it
	 * converts a chain file of lethenon 0.5.0 or earlier (#581)
	 *
	 * @param consent
	 *            whom to ask
	 */
	LethenonMinePanel(final ChainConversionConsent consent)
	{
		this(consent, LethenonVault.current(), Locale.getDefault());
	}

	/**
	 * Instantiates a new {@link LethenonMinePanel} over the given vault
	 *
	 * @param consent
	 *            whom to ask before a chain file is converted
	 * @param vault
	 *            the plugin's view of the vault; its wallet is the one signed with
	 * @param locale
	 *            the language of the window
	 */
	LethenonMinePanel(final ChainConversionConsent consent, final LethenonVault vault,
		final Locale locale)
	{
		super(ToolForm.newLayout());
		this.consent = consent;
		this.locale = locale;
		walletRow = new WalletRow(vault, locale, txtWalletFile,
			() -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText));
		btnDetails = LethenonSwing.button("btnDetails",
			AnswerFields.detailsButtonText(false, locale), event -> onDetails(),
			text("lethenon.mine.tooltip.details",
				"show the chain, the account your block pays, what was checked and all the money "
					+ "there is"));
		nameTheComponents();
		explainTheComponents();
		bindToTheModel();
		layOutTheInputs();
		int answerStarts = getComponentCount();
		layOutTheAnswer();
		failureView = FailureView.below(this,
			List.of(getComponents()).subList(answerStarts, getComponentCount()), locale);
		add(lblResult, ToolForm.RESULT_LINE);
		pnlDetails.setVisible(false);
		followTheChainFile();
		txtChainFile.setText(LethenonSettingsContribution.chainFile());
		txtWalletFile.setText(LethenonSettingsContribution.walletFile());
		txtPun.setText(modelObject.getPun());
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonMinePanelModel getModelObject()
	{
		return modelObject;
	}

	/**
	 * Mines the next block. The password is used once: the support class wipes the array it gets,
	 * and the field and the model are cleared afterwards, whether a block was mined or not. A
	 * failure leaves no value of an earlier block on the screen.
	 */
	void onMine()
	{
		String failure = text("lethenon.mine.result.failed", "no block was mined");
		try
		{
			Path chainFile = Path.of(modelObject.getChainFile().trim());
			if (!ChainConversion.convertedIfAgreed(this, consent, chainFile))
			{
				showFailure(failure + ": " + ChainConversion.notConverted(),
					new IllegalStateException(ChainConversion.notConverted()));
				return;
			}
			char[] password = modelObject.getPassword();
			Path walletFile = Path.of(modelObject.getWalletFile().trim());
			MinedBlock mined;
			try
			{
				mined = MiningSupport.mine(new MiningOrder(chainFile, walletFile,
					modelObject.getChainKind(), modelObject.getPun(), ATTEMPTS),
					walletRow.source(password, walletFile), System.currentTimeMillis());
			}
			finally
			{
				Arrays.fill(password, '\0');
			}
			LethenonSettingsContribution.remember(Map.of(LethenonSettingsContribution.KEY_CHAIN_FILE,
				chainFile.toString(), LethenonSettingsContribution.KEY_WALLET_FILE,
				modelObject.getWalletFile().trim()));
			showTheAnswer(mined);
			setResult(text("lethenon.mine.result.written",
				"the block was mined and written to the chain file"));
		}
		catch (ChainRejected | IllegalArgumentException | IllegalStateException refused)
		{
			showFailure(failure + ": " + refused.getMessage(), refused);
		}
		catch (IOException unwritable)
		{
			showFailure(failure + ": " + unwritable.getMessage(), unwritable);
		}
		finally
		{
			modelObject.setPassword(null);
			txtPassword.setText("");
			updateTheChoiceOfChain(modelObject.getChainFile());
		}
	}

	/** Opens or closes "Show details" */
	void onDetails()
	{
		modelObject.setDetailsShown(!modelObject.isDetailsShown());
		pnlDetails.setVisible(modelObject.isDetailsShown());
		btnDetails.setText(AnswerFields.detailsButtonText(modelObject.isDetailsShown(), locale));
		revalidate();
		repaint();
	}

	/**
	 * The sentence that answers what the window was opened for (#604, rule 1): the block is yours
	 * and what it paid you, or, for the first block of a chain, that the next one is the first that
	 * pays
	 *
	 * @param mined
	 *            the block
	 * @param locale
	 *            the language of the window
	 * @return the sentence
	 */
	static String answerFor(final MinedBlock mined, final Locale locale)
	{
		if (mined.height() == 0L)
		{
			return LethenonMessages.getString("lethenon.mine.answer.genesis",
				"You started a new chain. Its first block belongs to nobody, so the next block you "
					+ "mine is the first that pays you.",
				locale);
		}
		return LethenonMessages.format("lethenon.mine.answer",
			"Block {0,number,integer} is yours: it paid you {1}.", locale, mined.height(),
			LethAmounts.format(mined.reward(), locale));
	}

	private void showTheAnswer(final MinedBlock mined)
	{
		modelObject.clearAnswer();
		modelObject.setAnswer(answerFor(mined, locale));
		modelObject.setBlock(LethenonMessages.format("lethenon.mine.block", "{0,number,integer}",
			locale, mined.height()));
		modelObject.setPaid(mined.beneficiary().equals(Genesis.NOBODY.toString())
			? text("lethenon.mine.paid.nobody", "nothing - the first block of a chain pays nobody")
			: LethAmounts.format(mined.reward(), locale));
		modelObject.setCarried(LethenonMessages.format("lethenon.mine.carried",
			"{0,choice,0#none|1#1 transfer|1<{0,number,integer} transfers}", locale,
			mined.transfers()));
		modelObject.setMinedPun(mined.pun());
		modelObject.setChain(mined.chainIdentifier());
		modelObject.setPaidTo(mined.beneficiary());
		modelObject.setChainChecked(AnswerFields.chainChecked(mined.replay(), locale));
		modelObject.setSupply(LethAmounts.format(mined.replay().supply(), locale));
		showTheModel();
	}

	/**
	 * Shows a failure in the pattern of draft 07 (#710): the cause and what to do, the reason under
	 * the details, the answer hidden; the status line does not repeat it, the model keeps the whole
	 * line for whoever reads the state
	 */
	private void showFailure(final String result, final Throwable thrown)
	{
		modelObject.clearAnswer();
		showTheModel();
		modelObject.setResultText(result);
		lblResult.setText(" ");
		failureView.show(Failures.of(thrown, new Refusal("lethenon.mine.failure", "No block was mined."), locale));
	}

	/** Puts what the model holds into the read-only fields */
	private void showTheModel()
	{
		txtAnswer.setText(modelObject.getAnswer());
		AnswerFields.setText(txtBlock, modelObject.getBlock());
		AnswerFields.setText(txtPaid, modelObject.getPaid());
		AnswerFields.setText(txtCarried, modelObject.getCarried());
		AnswerFields.setText(txtMinedPun, modelObject.getMinedPun());
		AnswerFields.setText(txtChain, modelObject.getChain());
		AnswerFields.setText(txtPaidTo, LongValues.shortened(modelObject.getPaidTo(), locale));
		AnswerFields.setText(txtChainChecked, modelObject.getChainChecked());
		AnswerFields.setText(txtSupply, modelObject.getSupply());
	}

	/**
	 * What the choice of chain shows: the main chain, as long as the library carries no genesis
	 * block for it, as starting with lethenon 1.0.0 (#535)
	 */
	private static String describe(final ChainKind kind)
	{
		String name = LethenonMessages.getString("lethenon.chain.kind." + kind.name(),
			kind.description(), Locale.getDefault());
		return kind.canStart() ? name
			: name + " - " + LethenonMessages.getString("lethenon.chain.kind.later",
				"it starts with lethenon 1.0.0", Locale.getDefault());
	}

	private void nameTheComponents()
	{
		txtChainFile.setName("txtChainFile");
		txtWalletFile.setName("txtWalletFile");
		txtPassword.setName("txtPassword");
		cbxChainKind.setName("cbxChainKind");
		cbxChainKind.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean selected, final boolean focused)
			{
				Object shown = value instanceof ChainKind kind ? describe(kind) : value;
				return super.getListCellRendererComponent(list, shown, index, selected, focused);
			}
		});
		txtPun.setName("txtPun");
		pnlDetails.setName("pnlDetails");
		lblResult.setName("lblResult");
		lblResult.setFont(lblResult.getFont().deriveFont(Font.BOLD));
		pnlBeforeYouStart.setBorder(BorderFactory.createTitledBorder(LethenonMessages
			.getString(LaunchChecklist.KEY_PREFIX + "title", "Before you start")));
	}

	private void explainTheComponents()
	{
		txtChainFile.setToolTipText(text("lethenon.mine.tooltip.chain.file",
			"the file that holds the chain; your block is added to it, and where there is no such "
				+ "file yet, your block starts a new chain"));
		txtWalletFile.setToolTipText(text("lethenon.mine.tooltip.wallet.file",
			"your wallet file; the block pays its Ed25519 account"));
		txtPassword.setToolTipText(text("lethenon.input.tooltip.password",
			"the password of your wallet file; it is used once and then forgotten"));
		cbxChainKind.setToolTipText(text("lethenon.mine.tooltip.chain.kind",
			"which chain a new chain file starts; choose the test network to try things out. Once "
				+ "the file exists, its first block decides, and this choice is closed"));
		txtPun.setToolTipText(text("lethenon.mine.tooltip.pun",
			"any words you like; they go into your block, with a number counted up at the end "
				+ "until the block is hard enough to find"));
	}

	private void layOutTheInputs()
	{
		add(new JLabel(text("lethenon.input.label.chain.file", "Chain file:")));
		add(txtChainFile, WITH_BUTTON);
		add(LethenonSwing.button("btnBrowseChainFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getChainFile())
				.ifPresent(txtChainFile::setText),
			text("lethenon.input.tooltip.browse.chain.file", "pick the chain file on your disk")),
			OWN_WIDTH);
		walletRow.addTo(this);
		add(new JLabel(text("lethenon.input.label.password", "Password:")));
		add(txtPassword, ToolForm.FIELD);
		add(pnlBeforeYouStart, WIDE_HIDDEN_WITHOUT_ROOM);
		add(new JLabel(text("lethenon.mine.label.chain.kind", "New chain:")));
		add(cbxChainKind, OWN_WIDTH);
		add(new JLabel(text("lethenon.mine.label.pun", "Your pun:")));
		add(txtPun, ToolForm.FIELD);
		add(ToolForm.buttons(LethenonSwing.button("btnMine",
			text("lethenon.mine.button.mine", "Mine the pun"), event -> onMine(),
			text("lethenon.mine.tooltip.mine",
				"look for a block with your words and add it to the chain file; the window waits "
					+ "until it is found, usually a few seconds"))),
			ToolForm.BUTTON_ROW);
	}

	private void layOutTheAnswer()
	{
		add(txtAnswer, ToolForm.WIDE + ", wmin 0");
		add(new JLabel(text("lethenon.mine.label.block", "Block number:")));
		add(txtBlock, ToolForm.FIELD);
		add(new JLabel(text("lethenon.mine.label.paid", "Paid to you:")));
		add(txtPaid, ToolForm.FIELD);
		add(new JLabel(text("lethenon.mine.label.carried", "Transfers it carries:")));
		add(txtCarried, ToolForm.FIELD);
		add(new JLabel(text("lethenon.mine.label.mined.pun", "Your pun in the block:")));
		add(txtMinedPun, ToolForm.FIELD);
		add(ToolForm.buttons(btnDetails), ToolForm.BUTTON_ROW);
		pnlDetails.add(new JLabel(text("lethenon.mine.label.chain", "Chain:")));
		pnlDetails.add(txtChain, ToolForm.FIELD);
		pnlDetails.add(new JLabel(text("lethenon.mine.label.paid.to", "Paid to the account:")));
		pnlDetails.add(txtPaidTo, ToolForm.FIELD);
		pnlDetails.add(AnswerFields.chainCheckedLabel(locale));
		pnlDetails.add(txtChainChecked, ToolForm.FIELD);
		pnlDetails.add(AnswerFields.supplyLabel(locale));
		pnlDetails.add(txtSupply, ToolForm.FIELD);
		add(pnlDetails, WIDE_HIDDEN_WITHOUT_ROOM);
	}

	private void bindToTheModel()
	{
		txtChainFile
			.setPropertyModel(LambdaModel.of(modelObject::getChainFile, modelObject::setChainFile));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		txtPassword
			.setPropertyModel(LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		cbxChainKind.setPropertyModel(
			LambdaModel.of(modelObject::getChainKind, modelObject::setChainKind));
		txtPun.setPropertyModel(LambdaModel.of(modelObject::getPun, modelObject::setPun));
		AnswerFields.show(txtAnswer, modelObject::getAnswer);
		AnswerFields.show(txtBlock, modelObject::getBlock);
		AnswerFields.show(txtPaid, modelObject::getPaid);
		AnswerFields.show(txtCarried, modelObject::getCarried);
		AnswerFields.show(txtMinedPun, modelObject::getMinedPun);
		AnswerFields.show(txtChain, modelObject::getChain);
		AnswerFields.show(txtPaidTo, () -> LongValues.shortened(modelObject.getPaidTo(), locale));
		AnswerFields.show(txtChainChecked, modelObject::getChainChecked);
		AnswerFields.show(txtSupply, modelObject::getSupply);
	}

	/**
	 * Opens the choice of chain while the chain file names no existing file, and closes it once it
	 * does: an existing chain's genesis block decides which chain it is. "Before you start" is
	 * shown exactly while the choice is open.
	 */
	private void followTheChainFile()
	{
		txtChainFile.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(final DocumentEvent event)
			{
				updateTheChoiceOfChain(txtChainFile.getText());
			}

			@Override
			public void removeUpdate(final DocumentEvent event)
			{
				updateTheChoiceOfChain(txtChainFile.getText());
			}

			@Override
			public void changedUpdate(final DocumentEvent event)
			{
				updateTheChoiceOfChain(txtChainFile.getText());
			}
		});
		updateTheChoiceOfChain(txtChainFile.getText());
	}

	/**
	 * Reads the path it is given rather than the model: the field's own listener, which updates the
	 * model, is called after this one
	 */
	private void updateTheChoiceOfChain(final String chainFilePath)
	{
		boolean open = !namesAnExistingFile(chainFilePath);
		cbxChainKind.setEnabled(open);
		if (pnlBeforeYouStart.isVisible() != open)
		{
			pnlBeforeYouStart.setVisible(open);
			revalidate();
		}
	}

	private static boolean namesAnExistingFile(final String text)
	{
		String trimmed = text.trim();
		try
		{
			return !trimmed.isEmpty() && Files.exists(Path.of(trimmed));
		}
		catch (InvalidPathException notAPath)
		{
			return false;
		}
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}

	private void setResult(final String text)
	{
		failureView.clear();
		modelObject.setResultText(text);
		lblResult.setText(text);
	}

	@Override
	public void removeNotify()
	{
		walletRow.stop();
		super.removeNotify();
	}
}
