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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.lethenon.Wallet;

/**
 * The network badge of the lethenon plugin (#697, draft 02, README defect G2): which badge the hint
 * {@code lethenon.network} gives, in both languages; the session's wallet in {@link LethenonVault},
 * which every window follows; the overview's badge and risk note; and a tool window's header and
 * title, which change with the wallet the overview shows.
 */
class NetworkBadgeTest
{

	private InMemoryVaultAccess access;

	private LethenonVault vault;

	@BeforeEach
	void aConnectedVault()
	{
		access = new InMemoryVaultAccess();
		vault = new LethenonVault();
		vault.connect(access);
	}

	static Stream<Arguments> hints()
	{
		return Stream.of(Arguments.of("testnet", Locale.ENGLISH, NetworkKind.TEST, "TESTNET"),
			Arguments.of("testnet", Locale.GERMAN, NetworkKind.TEST, "TESTNET"),
			Arguments.of("mainnet", Locale.ENGLISH, NetworkKind.MAIN, "MAIN NETWORK"),
			Arguments.of("mainnet", Locale.GERMAN, NetworkKind.MAIN, "HAUPTNETZ"),
			Arguments.of("", Locale.ENGLISH, NetworkKind.UNKNOWN, "NETWORK UNKNOWN"),
			Arguments.of("", Locale.GERMAN, NetworkKind.UNKNOWN, "NETZ UNBEKANNT"),
			Arguments.of("devnet", Locale.ENGLISH, NetworkKind.UNKNOWN, "NETWORK UNKNOWN"));
	}

	@ParameterizedTest(name = "\"{0}\" in {1} is {3}")
	@MethodSource("hints")
	@DisplayName("the hint on the entry decides the badge; a missing or unknown hint is NETWORK UNKNOWN, never a guess")
	void theHint_decidesTheBadge(final String hint, final Locale locale, final NetworkKind kind,
		final String badge)
	{
		assertEquals(kind, NetworkKind.of(hint));
		assertEquals(badge, kind.badge(locale));
	}

	@Test
	@DisplayName("each network has its own risk note, the test network's as decided in the README")
	void eachNetwork_hasItsOwnRiskNote()
	{
		assertEquals("Test network, LETH has no monetary value.",
			NetworkKind.TEST.riskNote(Locale.ENGLISH));
		assertEquals("Testnetz, LETH hat keinen Geldwert.", NetworkKind.TEST.riskNote(Locale.GERMAN));
		assertNotEquals(NetworkKind.TEST.riskNote(Locale.ENGLISH),
			NetworkKind.MAIN.riskNote(Locale.ENGLISH));
		assertNotEquals(NetworkKind.MAIN.riskNote(Locale.ENGLISH),
			NetworkKind.UNKNOWN.riskNote(Locale.ENGLISH));
	}

	@Test
	@DisplayName("the session's wallet is told to every listener when it changes, and the lock clears it")
	void theSessionsWallet_isToldAndClearedByTheLock()
	{
		List<Optional<VaultWallet>> told = new ArrayList<>();
		vault.whenTheWalletChanges(told::add);
		vault.restore(Wallet.create().phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.MAINNET);
		VaultWallet wallet = vault.wallets().get(0);

		vault.choose(wallet);
		assertEquals(Optional.of(wallet), vault.chosen());

		access.available = false;
		vault.vaultClosing();

		assertEquals(Optional.empty(), vault.chosen());
		assertEquals(List.of(Optional.of(wallet), Optional.empty()), told);
	}

	@Test
	@DisplayName("the overview shows the badge and the risk note of the wallet it shows")
	void theOverview_showsBadgeAndNote()
	{
		vault.restore(Wallet.create().phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.TESTNET);

		LethenonWalletPanel panel = new LethenonWalletPanel(vault, Locale.ENGLISH);

		assertEquals("TESTNET", textOf(panel, "lblNetworkBadge"));
		assertEquals("Test network, LETH has no monetary value.", textOf(panel, "txtRiskNote"));
		assertEquals(vault.wallets().get(0), vault.chosen().orElseThrow(),
			"the wallet the overview shows is the session's wallet");
	}

	@Test
	@DisplayName("a tool window's badge follows the session's wallet, NETWORK UNKNOWN while there is none")
	void aToolWindow_followsTheSessionsWallet()
	{
		JPanel tool = new JPanel();
		NetworkHeader header = NetworkHeader.around(tool, vault, Locale.ENGLISH);
		assertEquals("NETWORK UNKNOWN", Windows.component(header, "lblNetworkBadge", JLabel.class)
			.getText());
		vault.restore(Wallet.create().phrase().toCharArray(), ThrowawayPasswords.next(),
			WalletNetwork.MAINNET);

		vault.choose(vault.wallets().get(0));

		assertEquals("MAIN NETWORK",
			Windows.component(header, "lblNetworkBadge", JLabel.class).getText());
		assertTrue(header.isAncestorOf(tool), "the tool's own panel sits below the badge");
	}
}
