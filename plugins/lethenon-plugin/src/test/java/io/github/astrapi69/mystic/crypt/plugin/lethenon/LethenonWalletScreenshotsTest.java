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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;

/**
 * Pictures of the wallet window in English and German (#680), one per state, written to
 * {@code build/screenshots/lethenon-wallet} on the build's own display (gradle/own-display.gradle)
 * and kept by CI as an artifact. Each picture also checks what a picture can: the window has
 * content, and a refusal wraps instead of widening the window (README defect G4).
 */
class LethenonWalletScreenshotsTest
{

	/** Where the pictures go, relative to the plugin build's working directory */
	static final Path DIRECTORY = Path.of("build", "screenshots", "lethenon-wallet");

	/** The widest the window may be; the old windows grew to 1372 px on a refusal (G4) */
	private static final int WIDEST = 1000;

	@BeforeAll
	static void theApplicationsLookAndFeel() throws Exception
	{
		SwingUtilities.invokeAndWait(() -> {
			try
			{
				UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
			}
			catch (ReflectiveOperationException | UnsupportedLookAndFeelException absent)
			{
				// the pictures then show Swing's own look; the checks below do not depend on it
			}
		});
		Files.createDirectories(DIRECTORY);
		Files.createDirectories(EXPORTS);
	}

	/**
	 * A state of the window, named for its picture
	 *
	 * @param name
	 *            the file name without language and extension
	 * @param withWallets
	 *            how many wallets the vault holds before the window opens
	 * @param locked
	 *            whether the vault is locked
	 * @param action
	 *            what is done in the window before the picture
	 */
	record State(String name, int withWallets, boolean locked,
		Consumer<LethenonWalletPanel> action)
	{
		@Override
		public String toString()
		{
			return name;
		}
	}

	static Stream<Object[]> states()
	{
		List<State> states = List.of(new State("01-no-wallet", 0, false, panel -> {}),
			new State("02-vault-locked", 0, true, panel -> {}),
			new State("03-create", 0, false, LethenonWalletPanel::onCreate),
			new State("04-create-passwords-differ", 0, false, panel -> {
				panel.onCreate();
				panel.getModelObject().setPassword(ThrowawayPasswords.next());
				panel.getModelObject().setPasswordRepeated(ThrowawayPasswords.next());
				panel.onCreateConfirmed();
			}), new State("05-words-once", 0, false, panel -> {
				panel.onCreate();
				char[] password = ThrowawayPasswords.next();
				panel.getModelObject().setPassword(password.clone());
				panel.getModelObject().setPasswordRepeated(password);
				panel.onCreateConfirmed();
			}), new State("06-overview", 2, false, panel -> {}),
			new State("07-take-over", 0, false, LethenonWalletPanel::onTakeOver),
			new State("09-backup-unseal", 1, false, LethenonWalletPanel::onBackup),
			new State("10-backup-question-wrong-word", 1, false, panel -> {
				LethenonBackupPanel backup = atStepTwo(panel);
				backup.getModelObject().setAnswer1("abandon");
				backup.onQuestionConfirmed();
			}), new State("11-backup-export-written", 1, false, panel -> {
				LethenonBackupPanel backup = atStepTwo(panel);
				int[] positions = backup.getModelObject().getPositions();
				List<String> words = List.of(new String(backup.getModelObject().getShownWords())
					.split(" "));
				backup.getModelObject().setAnswer1(words.get(positions[0] - 1));
				backup.getModelObject().setAnswer2(words.get(positions[1] - 1));
				backup.getModelObject().setAnswer3(words.get(positions[2] - 1));
				backup.onQuestionConfirmed();
				backup.getModelObject().setExportFile(EXPORTS.resolve(
					"wallet-" + System.nanoTime() + ".lethwf").toString());
				backup.onExportConfirmed();
			}), new State("12-backup-check-mismatch", 1, false, panel -> {
				panel.onBackupCheck();
				panel.getBackupPanel().getModelObject().setCheckWords(Wallet.create().phrase());
				panel.getBackupPanel().onCheckConfirmed();
			}),
			new State("08-restore-unknown-word", 0, false, panel -> {
				panel.onRestore();
				List<String> words = new ArrayList<>(List.of(Wallet.create().phrase().split(" ")));
				words.set(11, "qqq");
				panel.getModelObject().setWords(String.join(" ", words));
				panel.onRestoreConfirmed();
			}));
		return states.stream().flatMap(state -> Stream.of(new Object[] { state, Locale.ENGLISH },
			new Object[] { state, Locale.GERMAN }));
	}

	@ParameterizedTest(name = "{0}, {1}")
	@MethodSource("states")
	@DisplayName("each state of the wallet window is pictured in both languages, with content and without growing wide")
	void eachState_isPictured(final State state, final Locale locale) throws Exception
	{
		BufferedImage[] picture = new BufferedImage[1];
		Dimension[] size = new Dimension[1];
		SwingUtilities.invokeAndWait(() -> {
			LethenonWalletPanel panel = new LethenonWalletPanel(vaultFor(state), locale);
			state.action().accept(panel);
			JFrame frame = new JFrame("lethenon wallet");
			frame.setContentPane(panel);
			frame.pack();
			frame.setSize(Math.max(frame.getWidth(), 760), Math.max(frame.getHeight(), 420));
			frame.setVisible(true);
			frame.validate();
			size[0] = panel.getPreferredSize();
			picture[0] = paint(panel);
			frame.dispose();
		});
		Path file = DIRECTORY.resolve(state.name() + "-" + locale.getLanguage() + ".png");
		write(picture[0], file);

		assertTrue(size[0].width <= WIDEST,
			state + " wants " + size[0].width + " px; a refusal wraps instead (G4)");
		assertTrue(hasContent(picture[0]), file + " is empty");
	}

	/** Where the export of the pictured step 3 writes, inside the build directory */
	private static final Path EXPORTS = Path.of("build", "screenshots-exports");

	/** Opens the backup on request and passes step 1, with the password every pictured wallet has */
	private static LethenonBackupPanel atStepTwo(final LethenonWalletPanel panel)
	{
		panel.onBackup();
		LethenonBackupPanel backup = panel.getBackupPanel();
		backup.getModelObject().setPassword(PASSWORD.clone());
		backup.onUnsealConfirmed();
		backup.onWrittenDown();
		return backup;
	}

	/** The password of every wallet a picture starts with, made up for this run */
	private static final char[] PASSWORD = ThrowawayPasswords.next();

	private static LethenonVault vaultFor(final State state)
	{
		InMemoryVaultAccess access = new InMemoryVaultAccess();
		LethenonVault vault = new LethenonVault();
		vault.connect(access);
		for (int count = 0; count < state.withWallets(); count++)
		{
			vault.restore(Wallet.create().phrase().toCharArray(), PASSWORD.clone(),
				count == 0 ? WalletNetwork.TESTNET : WalletNetwork.MAINNET);
		}
		access.available = !state.locked();
		return vault;
	}

	private static BufferedImage paint(final LethenonWalletPanel panel)
	{
		BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(),
			BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		panel.paint(graphics);
		graphics.dispose();
		return image;
	}

	private static void write(final BufferedImage image, final Path file) throws IOException
	{
		if (!ImageIO.write(image, "png", file.toFile()))
		{
			throw new IOException("no PNG writer for " + file);
		}
	}

	/** Whether more than one colour is in the picture: an unpainted window is one flat colour */
	private static boolean hasContent(final BufferedImage image)
	{
		int first = image.getRGB(0, 0);
		for (int y = 0; y < image.getHeight(); y += 3)
		{
			for (int x = 0; x < image.getWidth(); x += 3)
			{
				if (image.getRGB(x, y) != first)
				{
					return true;
				}
			}
		}
		return false;
	}
}
