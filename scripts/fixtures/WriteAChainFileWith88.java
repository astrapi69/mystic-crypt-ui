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
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.astrapi69.lethenon.Wallet;
import io.github.astrapi69.lethenon.WalletFile;
import io.github.astrapi69.mystic.crypt.plugin.lethenon.ChainKind;
import io.github.astrapi69.mystic.crypt.plugin.lethenon.MinedBlock;
import io.github.astrapi69.mystic.crypt.plugin.lethenon.MiningOrder;
import io.github.astrapi69.mystic.crypt.plugin.lethenon.MiningSupport;

/**
 * Writes a test network chain file the way mystic-crypt-ui 8.8 writes one: through the 8.8 lethenon
 * plugin's own MiningSupport, with the lethenon 0.4.0 that plugin carries. Run with a class path of
 * only those, taken from the 8.8 installer (see README.md).
 */
public class WriteAChainFileWith88
{
	public static void main(final String[] arguments) throws Exception
	{
		Path chainFile = Path.of(arguments[0]);
		Path walletFile = Path.of(arguments[1]);
		char[] password = arguments[2].toCharArray();
		long start = Long.parseLong(arguments[3]);
		if (Files.exists(chainFile) || Files.exists(walletFile))
		{
			throw new IllegalStateException("not overwriting " + chainFile + " or " + walletFile);
		}
		WalletFile.write(walletFile, Wallet.create(), password.clone());
		String[] puns = { "written by 8.8", "a second block from 8.8", "a third block from 8.8" };
		for (int block = 0; block < puns.length; block++)
		{
			MinedBlock mined = MiningSupport.mine(new MiningOrder(chainFile, walletFile,
				ChainKind.TEST_NETWORK, puns[block], 10_000_000L), password.clone(),
				start + block * 60_000L);
			System.out.println("height " + mined.height() + " pays " + mined.beneficiary() + ": "
				+ mined.replaySummary());
		}
	}
}
