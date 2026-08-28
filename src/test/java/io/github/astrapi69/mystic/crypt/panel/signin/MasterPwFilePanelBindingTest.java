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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.astrapi69.model.BaseModel;
import io.github.astrapi69.model.api.IModel;

/**
 * Tests that the components of {@link MasterPwFilePanel} are bound to its
 * {@link MasterPwFileModelBean}: what is typed and checked is in the model as it happens, the OK
 * button is enabled from what the model holds, and the OK handler finds the master password there.
 * <p>
 * The proof is the value the OK button saw: it is recorded out of the model, so it can only have
 * arrived there through the binding of the password field, not through a read of the widget.
 */
class MasterPwFilePanelBindingTest
{

	/**
	 * The master password this test types. It is made up per run rather than written into the
	 * source, so nothing here reads as a credential.
	 */
	private static final String MASTER_PASSWORD = "open-" + UUID.randomUUID();

	/**
	 * A {@link MasterPwFilePanel} that records what the OK button found in the model when it was
	 * pressed
	 */
	private static final class RecordingMasterPwFilePanel extends MasterPwFilePanel
	{
		@Serial
		private static final long serialVersionUID = 1L;

		/**
		 * The master password as the OK button read it from the model, null until OK was pressed
		 */
		private transient char[] masterPwSeenByOk;

		private RecordingMasterPwFilePanel(final IModel<MasterPwFileModelBean> model)
		{
			super(model);
		}

		@Override
		protected void onOk(final ActionEvent actionEvent)
		{
			masterPwSeenByOk = getModelObject().getMasterPw();
		}
	}

	private static RecordingMasterPwFilePanel newPanel()
	{
		return new RecordingMasterPwFilePanel(
			BaseModel.<MasterPwFileModelBean> of(MasterPwFileModelBean.builder().build()));
	}

	/**
	 * The typed master password is in the model while it is typed, and it is what the OK button
	 * works with
	 */
	@Test
	void theTypedMasterPasswordReachesTheOkButtonThroughTheModel()
	{
		RecordingMasterPwFilePanel panel = newPanel();
		panel.getCbxMasterPw().doClick();

		panel.getTxtMasterPw().setText(MASTER_PASSWORD);

		assertArrayEquals(MASTER_PASSWORD.toCharArray(), panel.getModelObject().getMasterPw(),
			"the typed master password did not reach the model");
		assertTrue(panel.getBtnOk().getModel().isEnabled(),
			"a master password is entered, so OK must be enabled");

		panel.getBtnOk().doClick();

		assertArrayEquals(MASTER_PASSWORD.toCharArray(), panel.masterPwSeenByOk,
			"the OK button did not find the typed master password in the model");
	}

	/**
	 * The check box writes its state into the model, and the OK button follows the model while the
	 * master password is typed and erased again
	 */
	@Test
	void theOkButtonFollowsWhatTheModelHoldsWhileTypingAndErasing()
	{
		RecordingMasterPwFilePanel panel = newPanel();
		assertFalse(panel.getModelObject().isWithMasterPw(),
			"a fresh panel is signed in with neither a master password nor a key file");
		assertFalse(panel.getBtnOk().getModel().isEnabled(),
			"without a master key OK must stay disabled");

		panel.getCbxMasterPw().doClick();

		assertTrue(panel.getModelObject().isWithMasterPw(),
			"the check box did not write its state into the model");
		assertFalse(panel.getBtnOk().getModel().isEnabled(),
			"the master password is still empty, so OK must stay disabled");

		panel.getTxtMasterPw().setText(MASTER_PASSWORD);
		assertTrue(panel.getBtnOk().getModel().isEnabled(),
			"with a master password in the model OK must be enabled");

		panel.getTxtMasterPw().setText("");

		assertEquals(0, panel.getModelObject().getMasterPw().length,
			"erasing the field must empty the master password in the model");
		assertFalse(panel.getBtnOk().getModel().isEnabled(),
			"with an empty master password OK must be disabled again");
	}

	/**
	 * Unchecking the master password clears it out of the model and takes the OK button with it - a
	 * press then reaches no handler
	 */
	@Test
	void uncheckingTheMasterPasswordClearsItOutOfTheModel()
	{
		RecordingMasterPwFilePanel panel = newPanel();
		panel.getCbxMasterPw().doClick();
		panel.getTxtMasterPw().setText(MASTER_PASSWORD);

		panel.getCbxMasterPw().doClick();

		assertFalse(panel.getModelObject().isWithMasterPw(),
			"the unchecked check box did not write its state into the model");
		assertNull(panel.getModelObject().getMasterPw(),
			"the master password must be gone from the model");
		assertFalse(panel.getTxtMasterPw().isEnabled(),
			"the password field must be disabled without a master password");
		assertFalse(panel.getBtnOk().getModel().isEnabled(), "OK must be disabled again");

		panel.getBtnOk().doClick();

		assertNull(panel.masterPwSeenByOk, "a disabled OK button must not reach its handler");
	}

	/**
	 * The key file field writes what it shows into the model, and OK stays disabled as long as the
	 * model has no key file - the button asks the model, not the text of the field
	 */
	@Test
	void theKeyFileFieldWritesWhatItShowsIntoTheModel()
	{
		RecordingMasterPwFilePanel panel = newPanel();

		panel.getCbxKeyFile().doClick();

		assertTrue(panel.getModelObject().isWithKeyFile(),
			"the key file check box did not write its state into the model");
		assertTrue(panel.getTxtKeyFile().isEnabled(),
			"the key file field must be editable once the key file is checked");

		panel.getTxtKeyFile().setText("master-key.pem");

		assertEquals("master-key.pem", panel.getModelObject().getSelectedKeyFilePath(),
			"what the key file field shows did not reach the model");
		assertNull(panel.getModelObject().getKeyFileInfo(),
			"typing a name picks no key file, the file chooser does");
		assertFalse(panel.getBtnOk().getModel().isEnabled(),
			"without a key file in the model OK must stay disabled");

		panel.getCbxKeyFile().doClick();

		assertFalse(panel.getModelObject().isWithKeyFile(),
			"the unchecked check box did not write its state into the model");
		assertEquals("", panel.getModelObject().getSelectedKeyFilePath(),
			"the cleared key file field must clear the model with it");
	}

	/**
	 * The button that shows the master password in plain text records that in the model, so the
	 * state of the field is readable there and not only from its echo character
	 */
	@Test
	void showingTheMasterPasswordIsRecordedInTheModel()
	{
		RecordingMasterPwFilePanel panel = newPanel();
		panel.getCbxMasterPw().doClick();
		panel.getTxtMasterPw().setText(MASTER_PASSWORD);

		panel.getBtnMasterPw().doClick();

		assertTrue(panel.getModelObject().isShowMasterPw(),
			"showing the master password must be recorded in the model");
		assertEquals(0, panel.getTxtMasterPw().getEchoChar(),
			"the master password must be shown in plain text");

		panel.getBtnMasterPw().doClick();

		assertFalse(panel.getModelObject().isShowMasterPw(),
			"hiding the master password must be recorded in the model");
		assertEquals('*', panel.getTxtMasterPw().getEchoChar(),
			"the master password must be masked again");
	}
}
