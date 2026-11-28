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
package io.github.astrapi69.mystic.crypt.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dialog;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.finder.WindowFinder;
import org.assertj.swing.fixture.DialogFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.astrapi69.mystic.crypt.MysticCryptApplicationFrame;
import io.github.astrapi69.mystic.crypt.TestPasswords;

/**
 * The Wi-Fi QR plugin through the installed zip (#1120): its action sits in the context menu of the
 * entry table, and on the picked entry it shows the network as a QR code
 */
class WifiQrPluginUiTest extends AbstractUiTest
{

	private static final String MASTER_PASSWORD = TestPasswords.throwaway();

	private static final String NETWORK = "attic";

	private static final String ACTION = "Show Wi-Fi QR code...";

	@Test
	@DisplayName("the context menu of an entry offers the plugin's action, and the action shows the network as a QR code (#1120)")
	void theAction_showsTheNetworkAsAQrCode() throws Exception
	{
		installPluginRequiringItBuilt(WIFI_QR_ZIP);
		File databaseFile = new File(tempHome, "wifi-qr.mcrdb");
		createDatabaseFileHeadless(databaseFile, MASTER_PASSWORD);
		ApplicationSteps application = signInWithExistingDatabase(databaseFile, MASTER_PASSWORD);
		FrameFixture frame = application.showMainFrame();
		application.selectTreeRow(frame, 0);
		application.addEntry(frame, NETWORK, "router", TestPasswords.throwaway());
		application.selectEntryRowByTitle(frame, NETWORK);

		application.chooseFromSelectedEntryPopup(frame, ACTION);

		DialogFixture dialog = WindowFinder.findDialog(new GenericTypeMatcher<Dialog>(Dialog.class)
		{
			@Override
			protected boolean isMatching(final Dialog candidate)
			{
				return "dlgWifiQr".equals(candidate.getName()) && candidate.isShowing();
			}
		}).withTimeout(10, TimeUnit.SECONDS).using(robot);
		try
		{
			assertEquals("Network: " + NETWORK,
				GuiActionRunner.execute(() -> dialog.label("lblWifiNetwork").target().getText()));
			JLabel code = dialog.label("lblWifiQr").target();
			assertNotNull(GuiActionRunner.execute(code::getIcon), "the window shows the code");
			Icon actionIcon = GuiActionRunner
				.execute(() -> MysticCryptApplicationFrame.getInstance().getEntryActions().stream()
					.filter(offered -> "wifi-qr-plugin".equals(offered.pluginId())).findFirst()
					.orElseThrow().contribution().getIcon());
			assertEquals(16, actionIcon.getIconWidth(), "the action's icon is the 16 px image");
			assertTrue(
				actionIcon instanceof ImageIcon imageIcon
					&& imageIcon.getImage()instanceof MultiResolutionImage images
					&& images.getResolutionVariants().stream()
						.anyMatch(variant -> variant.getWidth(null) == 32),
				"the action's icon carries the 32 px variant a scaled screen draws (#1192)");
		}
		finally
		{
			GuiActionRunner.execute(() -> dialog.target().dispose());
		}
	}
}
