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
package io.github.astrapi69.mystic.crypt.plugin.obfuscation;

import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Whether rule export and import can work at all (#357).
 * <p>
 * Both encrypt the exported rule table with the public key derived from the signed-in database's
 * private key, and decrypt an imported one with that same private key - so both need one. A
 * database opened with a master password alone has none:
 * {@link MasterPwFileModelBean#getPrivateKeyInfo()} is null, and asking
 * {@code KeyModelExtensions.toPrivateKey(null)} for a key from that threw a
 * {@link NullPointerException} out of both panels' export and import, on the ordinary case - the
 * sign-in dialog offers a master password without a key file, and that is the format this
 * application writes today.
 * <p>
 * No Swing type appears here: this is a question about the model, asked once by each panel to set
 * a button's enabled state and again, the same way, by the handler it guards - the same shape
 * {@code ChecksumSaveDecision} and {@code WorkspaceLockDecision} already use, so the button and the
 * refusal cannot drift apart.
 */
public final class ObfuscationKeyAvailability
{

	private ObfuscationKeyAvailability()
	{
	}

	/**
	 * Whether the signed-in database has a private key that rule export or import could use
	 *
	 * @param applicationModelBean
	 *            the application model; null is accepted and answers false
	 * @return true if there is a private key to encrypt or decrypt with
	 */
	public static boolean keyIsAvailable(final ApplicationModelBean applicationModelBean)
	{
		if (applicationModelBean == null)
		{
			return false;
		}
		MasterPwFileModelBean credentials = applicationModelBean.getMasterPwFileModelBean();
		return credentials != null && credentials.getPrivateKeyInfo() != null;
	}

	/**
	 * The tooltip for the export/import buttons while there is no key to use them with
	 *
	 * @return the tooltip text
	 */
	public static String disabledTooltip()
	{
		return ObfuscationMessages.getString("obfuscation.rule.table.tooltip.no.key",
			"disabled: this database was opened with a master password only, and rule export and "
				+ "import are protected with the database's key file instead");
	}

	/**
	 * The title of the dialog shown when export or import is reached without a key - the second
	 * line of the same refusal the disabled button is the first line of (#269/#270's shape: a
	 * decision asked in the action, not only at the button, so a keyboard shortcut or a caller
	 * added later cannot walk around it)
	 *
	 * @return the dialog title
	 */
	public static String refusalTitle()
	{
		return ObfuscationMessages.getString("obfuscation.rule.table.refused.no.key.title",
			"No key file for this database");
	}

	/**
	 * The message of the refusal dialog
	 *
	 * @return the message text
	 */
	public static String refusalMessage()
	{
		return ObfuscationMessages.getString("obfuscation.rule.table.refused.no.key",
			"Export and import of rule files are protected with this database's key file. This "
				+ "database was opened with a master password only, so it has none. Sign in with a "
				+ "database that has a key file to use them.");
	}
}
