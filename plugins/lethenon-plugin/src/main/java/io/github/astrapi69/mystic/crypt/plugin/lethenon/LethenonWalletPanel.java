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
import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import io.github.astrapi69.model.LambdaModel;
import io.github.astrapi69.mystic.crypt.plugin.api.VaultAccessRefusedException;
import io.github.astrapi69.mystic.crypt.ui.form.ToolForm;
import io.github.astrapi69.swing.model.combobox.EnumComboBoxModel;
import io.github.astrapi69.swing.model.component.JMComboBox;
import io.github.astrapi69.swing.model.component.JMPasswordField;
import io.github.astrapi69.swing.model.component.JMTextArea;
import io.github.astrapi69.swing.model.component.JMTextField;
import net.miginfocom.swing.MigLayout;

/**
 * The way into the lethenon plugin (#680): the wallets of the open vault. With no wallet yet it
 * offers the three ways to get one - create, take a wallet file over, restore from the 24 words -
 * and with wallets it shows the chosen one with its network and address. A new wallet's 24 words
 * are shown once, on paper-only terms, and overwritten when the person goes on, cancels, or the
 * vault locks.
 * <p>
 * UI over {@link LethenonVault} and {@link WalletInput}, which know no Swing type; the wallet
 * password is asked only by the action that needs it, and no field of this window keeps it after
 * the action.
 */
public class LethenonWalletPanel extends JPanel
{

	private static final long serialVersionUID = 1L;

	private final transient LethenonVault vault;

	private final Locale locale;

	private final LethenonWalletPanelModel modelObject = new LethenonWalletPanelModel();

	private final CardLayout cards = new CardLayout();

	private final JPanel pnlViews = new JPanel(cards);

	private final List<JMPasswordField> passwordFields = new ArrayList<>();

	private final DefaultComboBoxModel<VaultWallet> walletChoices = new DefaultComboBoxModel<>();

	private final JMComboBox<VaultWallet, ComboBoxModel<VaultWallet>> cbxWallet = new JMComboBox<>(
		walletChoices);

	private final JMTextField txtWalletTitle;

	private final JMTextField txtNetwork;

	private final JTextArea txtAddress = message("txtAddress", false);

	private final JMTextField txtWalletFile = new JMTextField(34);

	private final JMTextArea txtWords = new JMTextArea(4, 40);

	private final JTextArea txtRefusal = message("txtRefusal", true);

	private final JTextArea txtNotice = message("txtNotice", false);

	private final JTextArea txtBackupNotice = message("txtBackupNotice", true);

	private final JLabel lblNetworkBadge = NetworkHeader.badgeLabel();

	private final JLabel lblBalance = new JLabel("");

	private final JTextArea txtBalanceExact = message("txtBalanceExact", false);

	/** Where the chain is replayed and scanned, off the event dispatch thread in the application */
	private final transient Executor background;

	/** Where the window shows a result, the event dispatch thread in the application */
	private final transient Executor ui;

	/** The balance of the wallet shown, null until it is read */
	private transient AccountsBalance balance;

	/** The one-time payments of the wallet shown, null until they are checked with the password */
	private transient IncomingPayments incoming;

	private final JTextArea txtRiskNote = message("txtRiskNote", false);

	private final LethenonBackupPanel backup;

	/**
	 * Instantiates a new {@link LethenonWalletPanel} over the vault the host connected, in the
	 * language the application started in
	 */
	public LethenonWalletPanel()
	{
		this(LethenonVault.current(), Locale.getDefault(),
			Executors.newSingleThreadExecutor(Thread.ofVirtual().factory()),
			SwingUtilities::invokeLater);
	}

	/**
	 * Instantiates a new {@link LethenonWalletPanel} over the given vault, in the given language
	 *
	 * @param vault
	 *            the plugin's view of the vault
	 * @param locale
	 *            the language of the window
	 */
	LethenonWalletPanel(final LethenonVault vault, final Locale locale)
	{
		this(vault, locale, Runnable::run, Runnable::run);
	}

	/**
	 * Instantiates a new {@link LethenonWalletPanel} that reads the chain where it is told to
	 *
	 * @param vault
	 *            the plugin's view of the vault
	 * @param locale
	 *            the language of the window
	 * @param background
	 *            where the chain is replayed and scanned
	 * @param ui
	 *            where a result is shown
	 */
	LethenonWalletPanel(final LethenonVault vault, final Locale locale, final Executor background,
		final Executor ui)
	{
		super(new MigLayout("fillx, insets 0, gap 0, wrap 1", "[grow,fill]",
			"[][]push"));
		this.vault = vault;
		this.background = background;
		this.ui = ui;
		this.locale = locale;
		backup = new LethenonBackupPanel(vault, locale, this::refresh);
		txtWalletTitle = AnswerFields.readOnly("txtWalletTitle", text("lethenon.wallet.tooltip.title",
			"the name of this wallet's entry in the vault"));
		txtNetwork = AnswerFields.readOnly("txtNetwork", text("lethenon.wallet.tooltip.network",
			"the network this wallet is meant for, as recorded on its entry"));
		txtAddress.setToolTipText(text("lethenon.wallet.tooltip.address",
			"your address: give it to whoever pays you; it is public and opens nothing"));
		txtAddress.setFont(new Font(Font.MONOSPACED, Font.PLAIN, txtAddress.getFont().getSize()));
		pnlViews.add(noVaultView(), WalletView.NO_VAULT.name());
		pnlViews.add(emptyView(), WalletView.EMPTY.name());
		pnlViews.add(overviewView(), WalletView.OVERVIEW.name());
		pnlViews.add(createView(), WalletView.CREATE.name());
		pnlViews.add(backup, WalletView.BACKUP.name());
		pnlViews.add(takeOverView(), WalletView.TAKE_OVER.name());
		pnlViews.add(restoreView(), WalletView.RESTORE.name());
		pnlViews.add(incomingView(), WalletView.INCOMING.name());
		add(pnlViews, "wmin 0");
		add(messages(), "wmin 0");
		vault.whenClosing(this::onVaultClosing);
		refresh(null);
	}

	/**
	 * Gets the panel's state, which is what a test reads instead of the widgets
	 *
	 * @return the model object every component of this panel is bound to
	 */
	public LethenonWalletPanelModel getModelObject()
	{
		return modelObject;
	}

	/** Opens the form for a new wallet */
	void onCreate()
	{
		show(WalletView.CREATE);
	}

	/** Opens the form for taking a wallet file over */
	void onTakeOver()
	{
		show(WalletView.TAKE_OVER);
	}

	/** Opens the form for restoring a wallet from its 24 words */
	void onRestore()
	{
		show(WalletView.RESTORE);
	}

	/** Leaves a form without doing anything */
	void onFormCancelled()
	{
		refresh(null);
	}

	/**
	 * Creates the wallet in the vault and shows its 24 words once; two different passwords or an
	 * empty one are refused before anything is created
	 */
	void onCreateConfirmed()
	{
		Optional<Refusal> refused = WalletInput.passwords(modelObject.getPassword(),
			modelObject.getPasswordRepeated());
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		NewWallet created;
		try
		{
			created = vault.create(modelObject.getNetwork(), modelObject.getPassword());
		}
		catch (final VaultAccessRefusedException notWritable)
		{
			refuse(notWritable.getMessage());
			return;
		}
		finally
		{
			clearPasswordFields();
		}
		backup.startWithNewWallet(created);
		show(WalletView.BACKUP);
	}

	/** Takes the named wallet file over into the vault; the file is only read */
	void onTakeOverConfirmed()
	{
		Optional<Refusal> refused = WalletInput.walletFile(modelObject.getWalletFile());
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		String fileName = modelObject.getWalletFile().trim();
		try
		{
			Path walletFile = Path.of(fileName);
			UUID taken = vault.takeOver(walletFile, modelObject.getPassword(),
				modelObject.getNetwork());
			refresh(taken);
			notice(LethenonMessages.format("lethenon.wallet.notice.taken.over",
				"The wallet from {0} is in the vault now. The file was only read and is still "
					+ "there; whether you delete it is your decision.",
				locale, String.valueOf(walletFile.getFileName())));
		}
		catch (final SecurityException wrongPassword)
		{
			refuse(LethenonMessages.format("lethenon.wallet.form.refused.file.password",
				"The password does not open {0}. Type the password the file was sealed with.",
				locale, fileName));
		}
		catch (final IOException | IllegalArgumentException unreadable)
		{
			refuse(LethenonMessages.format("lethenon.wallet.form.refused.file.unreadable",
				"{0} could not be read as a wallet file: {1}", locale, fileName,
				unreadable.getMessage()));
		}
		catch (final VaultAccessRefusedException notWritable)
		{
			refuse(notWritable.getMessage());
		}
		finally
		{
			clearPasswordFields();
		}
	}

	/** Restores a wallet from the typed words into the vault */
	void onRestoreConfirmed()
	{
		Optional<Refusal> refused = WalletInput.words(modelObject.getWords())
			.or(() -> WalletInput.passwords(modelObject.getPassword(),
				modelObject.getPasswordRepeated()));
		if (refused.isPresent())
		{
			refuse(refused.get().text(locale));
			return;
		}
		try
		{
			UUID restored = vault.restore(modelObject.getWords().toCharArray(),
				modelObject.getPassword(), modelObject.getNetwork());
			clearWords();
			refresh(restored);
			notice(text("lethenon.wallet.notice.restored", "The wallet is restored into the vault."));
		}
		catch (final VaultAccessRefusedException notWritable)
		{
			refuse(notWritable.getMessage());
		}
		finally
		{
			clearPasswordFields();
		}
	}

	/**
	 * Shows another wallet of the vault
	 *
	 * @param wallet
	 *            one of {@link LethenonWalletPanelModel#getWallets()}
	 */
	void choose(final VaultWallet wallet)
	{
		if (wallet != modelObject.getChosen())
		{
			incoming = null;
		}
		modelObject.setChosen(wallet);
		vault.choose(wallet);
		walletChoices.setSelectedItem(wallet);
		showChosen();
	}

	/** Reads the wallets of the vault again, after the vault was unlocked */
	void onReload()
	{
		refresh(null);
	}

	/**
	 * The vault is about to lock or close: the words and every typed secret are overwritten, a
	 * creation whose words were not confirmed is taken back while the vault can still be written,
	 * and the window offers nothing until the vault is open again
	 */
	void onVaultClosing()
	{
		backup.forget();
		clearPasswordFields();
		clearWords();
		modelObject.setWallets(List.of());
		modelObject.setChosen(null);
		walletChoices.removeAllElements();
		show(WalletView.NO_VAULT);
	}

	private void refresh(final UUID preferred)
	{
		if (!vault.isAvailable())
		{
			vault.choose(null);
			show(WalletView.NO_VAULT);
			return;
		}
		List<VaultWallet> wallets = vault.wallets();
		modelObject.setWallets(wallets);
		walletChoices.removeAllElements();
		walletChoices.addAll(wallets);
		if (wallets.isEmpty())
		{
			vault.choose(null);
			show(WalletView.EMPTY);
			return;
		}
		choose(wallets.stream().filter(wallet -> wallet.id().equals(preferred)).findFirst()
			.orElse(wallets.get(0)));
		show(WalletView.OVERVIEW);
	}

	private void showChosen()
	{
		VaultWallet chosen = modelObject.getChosen();
		AnswerFields.setText(txtWalletTitle, chosen == null ? "" : chosen.title());
		AnswerFields.setText(txtNetwork,
			chosen == null ? "" : WalletNetwork.nameOf(chosen.network(), locale));
		txtAddress.setText(chosen == null ? "" : chosen.address());
		NetworkKind network = NetworkKind.of(chosen == null ? "" : chosen.network());
		network.paint(lblNetworkBadge, locale);
		txtRiskNote.setText(network.riskNote(locale));
		showTheBackupNotice(chosen);
		readTheBalance(chosen);
	}

	/**
	 * The notice of the decision record: clear while no check is recorded, a gentle reminder once
	 * the last one is older than the interval, nothing in between
	 */
	private void showTheBackupNotice(final VaultWallet chosen)
	{
		BackupState state = chosen == null ? BackupState.CURRENT
			: vault.backupStateOf(chosen.id(), LocalDate.now());
		String notice = switch (state)
		{
			case NOT_CHECKED -> text("lethenon.wallet.backup.not.checked",
				"Your backup is not checked yet. Check your 24 words once, then you know you can "
					+ "restore the wallet.");
			case DUE -> LethenonMessages.format("lethenon.wallet.backup.due",
				"Your last backup check is more than {0} days ago. Check your 24 words again.",
				locale, BackupReminder.INTERVAL_DAYS);
			case CURRENT -> "";
		};
		modelObject.setBackupNotice(notice);
		txtBackupNotice.setText(notice);
		txtBackupNotice.setVisible(!notice.isEmpty());
	}

	/**
	 * Reads both accounts' balances from the configured chain, without the wallet password; without
	 * a chain the window says so
	 */
	private void readTheBalance(final VaultWallet chosen)
	{
		balance = null;
		Optional<Path> chainFile = configuredChain();
		if (chosen == null || chainFile.isEmpty())
		{
			showTheBalance(chosen == null ? "" : text("lethenon.wallet.balance.no.chain",
				"No chain yet: the balance is read from your chain file. Fetch it with "
					+ "\"Synchronise with a Node\", then it shows here."));
			return;
		}
		background.execute(() -> {
			try
			{
				AccountsBalance read = OverviewBalance.of(chainFile.get(), chosen);
				ui.execute(() -> {
					balance = read;
					showTheBalance(exactBalance());
				});
			}
			catch (final IOException | RuntimeException unreadable)
			{
				ui.execute(() -> showTheBalance(LethenonMessages.format(
					"lethenon.wallet.balance.unreadable",
					"The chain file {0} could not be read: {1}", locale,
					chainFile.get().toString(), unreadable.getMessage())));
			}
		});
	}

	private Optional<Path> configuredChain()
	{
		String configured = LethenonSettingsContribution.chainFile();
		if (configured == null || configured.isBlank())
		{
			return Optional.empty();
		}
		Path chainFile = Path.of(configured.trim());
		return Files.isRegularFile(chainFile) ? Optional.of(chainFile) : Optional.empty();
	}

	private String exactBalance()
	{
		String exact = LethAmounts.format(balance.total(), locale);
		if (incoming == null)
		{
			return LethenonMessages.format("lethenon.wallet.balance.unchecked",
				"exactly {0}, without unchecked incoming payments", locale, exact);
		}
		return LethenonMessages.format("lethenon.wallet.balance.incoming",
			"exactly {0}; besides, {1} came to your address in {2,choice,0#no payment|1#1 payment|"
				+ "1<{2,number,integer} payments} and is not yet taken over",
			locale, exact, LethAmounts.rounded(incoming.total(), locale), incoming.count());
	}

	private void showTheBalance(final String exact)
	{
		String rounded = balance == null ? "" : LethAmounts.rounded(balance.total(), locale);
		modelObject.setBalance(rounded);
		modelObject.setBalanceExact(exact);
		lblBalance.setText(rounded);
		txtBalanceExact.setText(exact);
	}

	/** Opens the wallet password for the check of the one-time payments */
	void onCheckIncoming()
	{
		show(WalletView.INCOMING);
	}

	/**
	 * Scans the chain for the one-time payments with the typed password; the wallet is unsealed for
	 * this scan only and the password is overwritten
	 */
	void onCheckIncomingConfirmed()
	{
		Optional<Path> chainFile = configuredChain();
		VaultWallet chosen = modelObject.getChosen();
		if (chainFile.isEmpty() || chosen == null)
		{
			refuse(text("lethenon.wallet.balance.no.chain",
				"No chain yet: the balance is read from your chain file. Fetch it with "
					+ "\"Synchronise with a Node\", then it shows here."));
			return;
		}
		char[] password = modelObject.getPassword().clone();
		clearPasswordFields();
		background.execute(() -> {
			try
			{
				IncomingPayments found = vault.incomingPayments(chosen.id(), password,
					chainFile.get());
				ui.execute(() -> {
					incoming = found;
					show(WalletView.OVERVIEW);
					showTheBalance(balance == null ? "" : exactBalance());
				});
			}
			catch (final SecurityException wrongPassword)
			{
				ui.execute(() -> refuse(text("lethenon.backup.unseal.refused",
					"This password does not open the wallet. Type the wallet password again.")));
			}
			catch (final IOException | RuntimeException unreadable)
			{
				ui.execute(() -> refuse(LethenonMessages.format("lethenon.wallet.balance.unreadable",
					"The chain file {0} could not be read: {1}", locale,
					chainFile.get().toString(), unreadable.getMessage())));
			}
		});
	}

	/** Starts the backup assistant for the chosen wallet: its password, then steps 1 to 3 */
	void onBackup()
	{
		backup.startOnRequest(modelObject.getChosen().id());
		show(WalletView.BACKUP);
	}

	/** Starts the check of all 24 words for the chosen wallet */
	void onBackupCheck()
	{
		backup.startCheck(modelObject.getChosen().id());
		show(WalletView.BACKUP);
	}

	/**
	 * Gets the backup assistant, which a test drives through its own model
	 *
	 * @return the assistant shown in {@link WalletView#BACKUP}
	 */
	LethenonBackupPanel getBackupPanel()
	{
		return backup;
	}

	private void show(final WalletView view)
	{
		modelObject.setView(view);
		refuse("");
		notice("");
		cards.show(pnlViews, view.name());
	}

	private void refuse(final String reason)
	{
		modelObject.setRefusal(reason);
		txtRefusal.setText(reason);
		txtRefusal.setVisible(!reason.isEmpty());
		revalidate();
	}

	private void notice(final String text)
	{
		modelObject.setNotice(text);
		txtNotice.setText(text);
		txtNotice.setVisible(!text.isEmpty());
		revalidate();
	}

	private void clearPasswordFields()
	{
		passwordFields.forEach(field -> field.setText(""));
		modelObject.setPassword(new char[0]);
		modelObject.setPasswordRepeated(new char[0]);
	}

	private void clearWords()
	{
		txtWords.setText("");
		modelObject.setWords("");
	}

	private JPanel noVaultView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblNoVaultTitle", "lethenon.wallet.locked.title", "The vault is locked"),
			ToolForm.WIDE);
		view.add(ToolForm.intro(text("lethenon.wallet.locked.text",
			"Unlock the vault, then you see your wallets. While it is locked, the program keeps "
				+ "no unsealed wallet in memory.")),
			ToolForm.INTRO_ROW);
		view.add(ToolForm.buttons(LethenonSwing.button("btnReload",
			text("lethenon.wallet.button.reload", "Show my wallets"), event -> onReload(),
			text("lethenon.wallet.tooltip.reload", "read the wallets of the vault once it is unlocked"))),
			ToolForm.BUTTON_ROW);
		return view;
	}

	private JPanel emptyView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblEmptyTitle", "lethenon.wallet.empty.title", "No wallet in the vault yet"),
			ToolForm.WIDE);
		view.add(ToolForm.intro(text("lethenon.wallet.empty.text",
			"A wallet is the key to your LETH, stored sealed in the vault.")), ToolForm.INTRO_ROW);
		view.add(ToolForm.buttons(createButton("btnCreate"), takeOverButton("btnTakeOver"),
			restoreButton("btnRestore")), ToolForm.BUTTON_ROW);
		JLabel practice = new JLabel(text("lethenon.wallet.empty.practice",
			"On the test network you practise without real value."));
		view.add(practice, ToolForm.WIDE);
		return view;
	}

	private JPanel overviewView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		lblNetworkBadge.setToolTipText(text("lethenon.network.tooltip.badge",
			"the network of this wallet, from its vault entry"));
		txtRiskNote.setToolTipText(text("lethenon.network.tooltip.risk",
			"what this network means for your LETH"));
		view.add(lblNetworkBadge, "span 2, split 2, alignx left");
		view.add(txtRiskNote, "growx, wmin 0, gapleft 12");
		lblBalance.setName("lblBalance");
		Font base = lblBalance.getFont();
		lblBalance.setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 2.2f));
		lblBalance.setToolTipText(text("lethenon.wallet.tooltip.balance",
			"what both accounts of this wallet hold, rounded to two decimals"));
		txtBalanceExact.setToolTipText(text("lethenon.wallet.tooltip.balance.exact",
			"the exact balance, and whether payments to your address were checked"));
		view.add(lblBalance, ToolForm.WIDE + ", gaptop 6");
		view.add(txtBalanceExact, ToolForm.WIDE + ", wmin 0");
		view.add(ToolForm.buttons(LethenonSwing.button("btnCheckIncoming",
			text("lethenon.wallet.button.check.incoming", "Check incoming payments"),
			event -> onCheckIncoming(),
			text("lethenon.wallet.tooltip.check.incoming",
				"the wallet password finds payments to your address; nothing is kept"))),
			ToolForm.BUTTON_ROW);
		cbxWallet.setName("cbxWallet");
		cbxWallet.setToolTipText(text("lethenon.wallet.tooltip.choose",
			"the wallets of this vault; choose the one to show"));
		cbxWallet.setRenderer(walletRenderer());
		cbxWallet.setPropertyModel(LambdaModel.of(modelObject::getChosen, modelObject::setChosen));
		cbxWallet.addActionListener(event -> showChosen());
		view.add(new JLabel(text("lethenon.wallet.label.choose", "Wallet:")));
		view.add(cbxWallet, ToolForm.FIELD);
		view.add(new JLabel(text("lethenon.wallet.label.title", "Entry:")));
		view.add(txtWalletTitle, ToolForm.FIELD);
		view.add(new JLabel(text("lethenon.wallet.label.network", "Network:")));
		view.add(txtNetwork, ToolForm.FIELD);
		view.add(new JLabel(text("lethenon.wallet.label.address", "Your address (public):")));
		view.add(txtAddress, ToolForm.FIELD + ", wmin 0");
		view.add(ToolForm.buttons(AnswerFields.copyButton("btnCopyAddress", txtAddress::getText,
			text("lethenon.wallet.tooltip.copy.address",
				"copy the whole address, to give it to whoever pays you"),
			locale)), ToolForm.BUTTON_ROW);
		txtBackupNotice.setToolTipText(text("lethenon.wallet.tooltip.backup.notice",
			"whether your 24 words were checked, and when"));
		view.add(txtBackupNotice, ToolForm.WIDE + ", wmin 0, gaptop 8, hidemode 3");
		view.add(ToolForm.buttons(
			LethenonSwing.button("btnBackupCheck",
				text("lethenon.wallet.button.backup.check", "Check my backup"),
				event -> onBackupCheck(),
				text("lethenon.wallet.tooltip.backup.check",
					"type the 24 words from your note and see whether they are this wallet's")),
			LethenonSwing.button("btnBackup", text("lethenon.wallet.button.backup", "Back up"),
				event -> onBackup(),
				text("lethenon.wallet.tooltip.backup",
					"show the 24 words once more, ask three of them, write the sealed file"))),
			ToolForm.BUTTON_ROW);
		view.add(new JLabel(text("lethenon.wallet.label.another", "Another wallet:")),
			"newline, gaptop 12");
		view.add(ToolForm.buttons(createButton("btnAnotherCreate"),
			takeOverButton("btnAnotherTakeOver"), restoreButton("btnAnotherRestore")),
			"gaptop 12");
		return view;
	}

	private JPanel createView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblCreateTitle", "lethenon.wallet.create.title", "A new wallet"),
			ToolForm.WIDE);
		view.add(ToolForm.intro(text("lethenon.wallet.create.text",
			"The wallet password seals the wallet in the vault and is not stored. Next you see "
				+ "the 24 words that restore the wallet, once.")),
			ToolForm.INTRO_ROW);
		addNetworkChoice(view, "cbxCreateNetwork");
		addPasswordTwice(view, "txtCreatePassword", "txtCreatePasswordRepeated");
		view.add(ToolForm.buttons(
			LethenonSwing.button("btnCreateConfirmed",
				text("lethenon.wallet.button.create.confirmed", "Create the wallet"),
				event -> onCreateConfirmed(),
				text("lethenon.wallet.tooltip.create.confirmed",
					"create the wallet in the vault and show its 24 words")),
			cancelButton("btnCreateCancelled")), ToolForm.BUTTON_ROW);
		return view;
	}

	private JPanel takeOverView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblTakeOverTitle", "lethenon.wallet.take.over.title",
			"Take a wallet file over into the vault"), ToolForm.WIDE);
		view.add(ToolForm.intro(text("lethenon.wallet.take.over.text",
			"The sealed file is copied into the vault as it is. The file itself is only read; "
				+ "whether you delete it afterwards is your decision.")),
			ToolForm.INTRO_ROW);
		txtWalletFile.setName("txtWalletFile");
		txtWalletFile.setToolTipText(text("lethenon.wallet.tooltip.file",
			"the wallet file lethenon's 'wallet create' or 'wallet restore' wrote"));
		txtWalletFile.setPropertyModel(
			LambdaModel.of(modelObject::getWalletFile, modelObject::setWalletFile));
		view.add(new JLabel(text("lethenon.input.label.wallet.file", "Wallet file:")));
		view.add(txtWalletFile, "growx, split 2");
		view.add(LethenonSwing.button("btnBrowseWalletFile", "...",
			event -> LethenonSwing.chooseFile(this, modelObject.getWalletFile())
				.ifPresent(txtWalletFile::setText),
			text("lethenon.input.tooltip.browse.wallet.file", "pick the wallet file on your disk")),
			"alignx left, width pref!");
		addNetworkChoice(view, "cbxTakeOverNetwork");
		view.add(new JLabel(text("lethenon.wallet.label.file.password", "Its password:")));
		view.add(passwordField("txtTakeOverPassword", false,
			text("lethenon.wallet.tooltip.file.password",
				"the password the file is sealed with; it stays the wallet password")),
			ToolForm.FIELD);
		view.add(ToolForm.buttons(
			LethenonSwing.button("btnTakeOverConfirmed",
				text("lethenon.wallet.button.take.over.confirmed", "Take it over"),
				event -> onTakeOverConfirmed(),
				text("lethenon.wallet.tooltip.take.over.confirmed",
					"open the file with its password and copy it, sealed, into the vault")),
			cancelButton("btnTakeOverCancelled")), ToolForm.BUTTON_ROW);
		return view;
	}

	private JPanel incomingView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblIncomingTitle", "lethenon.wallet.incoming.title",
			"Check incoming payments"), ToolForm.WIDE);
		view.add(ToolForm.intro(text("lethenon.wallet.incoming.text",
			"Payments to your address can only be found with the wallet's private view key. The "
				+ "wallet password opens the wallet in memory for this check; nothing is kept.")),
			ToolForm.INTRO_ROW);
		view.add(new JLabel(text("lethenon.wallet.label.password", "Wallet password:")));
		view.add(passwordField("txtIncomingPassword", false,
			text("lethenon.backup.tooltip.unseal.password",
				"the password this wallet is sealed with in the vault")),
			ToolForm.FIELD);
		view.add(ToolForm.buttons(
			LethenonSwing.button("btnCheckIncomingConfirmed",
				text("lethenon.wallet.button.check.incoming.confirmed", "Check"),
				event -> onCheckIncomingConfirmed(),
				text("lethenon.wallet.tooltip.check.incoming.confirmed",
					"scan the chain for payments to your address")),
			cancelButton("btnCheckIncomingCancelled")), ToolForm.BUTTON_ROW);
		return view;
	}

	private JPanel restoreView()
	{
		JPanel view = new JPanel(ToolForm.newLayout());
		view.add(title("lblRestoreTitle", "lethenon.wallet.restore.title",
			"Restore a wallet from its 24 words"), ToolForm.WIDE);
		txtWords.setName("txtWords");
		txtWords.setLineWrap(true);
		txtWords.setWrapStyleWord(true);
		txtWords.setToolTipText(text("lethenon.wallet.tooltip.words",
			"the 24 words from your note, in order, separated by spaces"));
		txtWords.setPropertyModel(LambdaModel.of(modelObject::getWords, modelObject::setWords));
		view.add(new JLabel(text("lethenon.wallet.label.words", "The 24 words:")));
		view.add(ToolForm.scrolled(txtWords), ToolForm.FIELD);
		addNetworkChoice(view, "cbxRestoreNetwork");
		addPasswordTwice(view, "txtRestorePassword", "txtRestorePasswordRepeated");
		view.add(ToolForm.buttons(
			LethenonSwing.button("btnRestoreConfirmed",
				text("lethenon.wallet.button.restore.confirmed", "Restore the wallet"),
				event -> onRestoreConfirmed(),
				text("lethenon.wallet.tooltip.restore.confirmed",
					"derive the wallet from the words and seal it into the vault")),
			cancelButton("btnRestoreCancelled")), ToolForm.BUTTON_ROW);
		return view;
	}

	private JPanel messages()
	{
		JPanel messages = new JPanel(ToolForm.newLayout());
		messages.add(txtRefusal, ToolForm.WIDE + ", wmin 0, hidemode 3");
		messages.add(txtNotice, ToolForm.WIDE + ", wmin 0, hidemode 3");
		txtRefusal.setToolTipText(text("lethenon.wallet.tooltip.refusal",
			"why the last action did not happen, and what to do"));
		txtNotice.setToolTipText(text("lethenon.wallet.tooltip.notice", "what the last action did"));
		txtRefusal.setVisible(false);
		txtNotice.setVisible(false);
		return messages;
	}

	private void addNetworkChoice(final JPanel view, final String name)
	{
		JMComboBox<WalletNetwork, ComboBoxModel<WalletNetwork>> network = new JMComboBox<>(
			new EnumComboBoxModel<>(WalletNetwork.class, WalletNetwork.TESTNET, Set.of()));
		network.setName(name);
		network.setToolTipText(text("lethenon.wallet.tooltip.network.choice",
			"the network this wallet is for; on the test network LETH has no monetary value"));
		network.setRenderer(new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				Object shown = value instanceof WalletNetwork kind ? kind.displayName(locale) : value;
				return super.getListCellRendererComponent(list, shown, index, isSelected,
					cellHasFocus);
			}
		});
		network.setPropertyModel(LambdaModel.of(modelObject::getNetwork, modelObject::setNetwork));
		view.add(new JLabel(text("lethenon.wallet.label.network", "Network:")));
		view.add(network, "alignx left, width pref!");
	}

	private void addPasswordTwice(final JPanel view, final String name, final String repeatedName)
	{
		view.add(new JLabel(text("lethenon.wallet.label.password", "Wallet password:")));
		view.add(passwordField(name, false, text("lethenon.wallet.tooltip.password",
			"seals the wallet in the vault; it is not stored, so keep it in mind")), ToolForm.FIELD);
		view.add(new JLabel(text("lethenon.wallet.label.password.repeated", "Once more:")));
		view.add(passwordField(repeatedName, true, text("lethenon.wallet.tooltip.password.repeated",
			"the same password again, so a typing error does not lock you out")), ToolForm.FIELD);
	}

	private JMPasswordField passwordField(final String name, final boolean repeated,
		final String tooltip)
	{
		JMPasswordField field = new JMPasswordField(34);
		field.setName(name);
		field.setToolTipText(tooltip);
		field.setPropertyModel(repeated
			? LambdaModel.of(modelObject::getPasswordRepeated, modelObject::setPasswordRepeated)
			: LambdaModel.of(modelObject::getPassword, modelObject::setPassword));
		passwordFields.add(field);
		return field;
	}

	private JButton createButton(final String name)
	{
		return LethenonSwing.button(name,
			text("lethenon.wallet.button.create", "Create a new wallet"), event -> onCreate(),
			text("lethenon.wallet.tooltip.create",
				"a new wallet, sealed into the vault; you write its 24 words down once"));
	}

	private JButton takeOverButton(final String name)
	{
		return LethenonSwing.button(name,
			text("lethenon.wallet.button.take.over", "Take over a wallet file"),
			event -> onTakeOver(), text("lethenon.wallet.tooltip.take.over",
				"a wallet file lethenon's command line wrote, copied sealed into the vault"));
	}

	private JButton restoreButton(final String name)
	{
		return LethenonSwing.button(name,
			text("lethenon.wallet.button.restore", "Restore from 24 words"), event -> onRestore(),
			text("lethenon.wallet.tooltip.restore",
				"a wallet you have the 24 words of, restored into the vault"));
	}

	private JButton cancelButton(final String name)
	{
		return LethenonSwing.button(name, text("lethenon.wallet.button.cancel", "Cancel"),
			event -> onFormCancelled(),
			text("lethenon.wallet.tooltip.cancel", "back, without changing the vault"));
	}

	private JLabel title(final String name, final String key, final String defaultText)
	{
		return title(name, text(key, defaultText));
	}

	/**
	 * The title of a view: larger and bold
	 *
	 * @param name
	 *            the component name
	 * @param text
	 *            the title
	 * @return the label
	 */
	static JLabel title(final String name, final String text)
	{
		JLabel title = new JLabel(text);
		title.setName(name);
		Font base = title.getFont();
		title.setFont(base.deriveFont(Font.BOLD, base.getSize2D() * 1.4f));
		return title;
	}

	private DefaultListCellRenderer walletRenderer()
	{
		return new DefaultListCellRenderer()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				Object shown = value instanceof VaultWallet wallet ? wallet.title() : value;
				return super.getListCellRendererComponent(list, shown, index, isSelected,
					cellHasFocus);
			}
		};
	}

	/**
	 * A text that wraps instead of widening the window, read-only, for refusals, notices and
	 * values a person copies by eye
	 *
	 * @param name
	 *            the component name
	 * @param bold
	 *            whether it is set in bold
	 * @return the text area
	 */
	static JTextArea message(final String name, final boolean bold)
	{
		JTextArea message = new JTextArea();
		message.setName(name);
		message.setEditable(false);
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		message.setOpaque(false);
		Font base = new JLabel().getFont();
		message.setFont(bold ? base.deriveFont(Font.BOLD) : base);
		return message;
	}

	private String text(final String key, final String defaultValue)
	{
		return LethenonMessages.getString(key, defaultValue, locale);
	}
}
