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
package io.github.astrapi69.mystic.crypt.plugin.releasekey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.astrapi69.mystic.crypt.plugin.releasekey.ThrowawayGnuPgHome.ListedKey;

/**
 * {@link GnuPgHomeSupport} reads GnuPG homes that gpg itself wrote, and every answer is measured
 * against what gpg says about the same home.
 * <p>
 * The home holds four certificates: one shaped like the release key (an RSA primary key that signs
 * and certifies and is offline, an encryption subkey and a signing subkey that expires, both
 * present), an Ed25519 key with a Curve25519 subkey whose revocation certificate was removed, an
 * Ed25519 key without a passphrase whose revocation certificate is another key's, and a certificate
 * imported without any secret part.
 */
class GnuPgHomeSupportTest
{

	private static final String RELEASE = "Release Test <release@example.org>";

	private static final String EDWARDS = "Edwards Test <edwards@example.org>";

	private static final String UNPROTECTED = "Unprotected Test <unprotected@example.org>";

	private static final String FOREIGN = "Foreign Test <foreign@example.org>";

	@TempDir
	static Path directory;

	private static ThrowawayGnuPgHome home;

	private static String releaseFingerprint;

	private static List<ListedKey> gpgSays;

	private static List<KeyRingState> read;

	@BeforeAll
	static void writeTheHomeWithGpg() throws IOException
	{
		home = ThrowawayGnuPgHome.create(directory.resolve("home"));
		releaseFingerprint = home.generate(RELEASE, "rsa2048", "sign,cert", "0");
		home.addSubkey(releaseFingerprint, "rsa2048", "encr", "0");
		home.addSubkey(releaseFingerprint, "rsa2048", "sign", "2y");
		home.takePrimaryOffline(releaseFingerprint);

		String edwards = home.generate(EDWARDS, "ed25519", "sign,cert", "0");
		home.addSubkey(edwards, "cv25519", "encr", "1y");
		Files.delete(revocationFile(edwards));

		String unprotected = home.generateUnprotected(UNPROTECTED, "ed25519", "sign,cert");
		Files.copy(revocationFile(releaseFingerprint), revocationFile(unprotected),
			java.nio.file.StandardCopyOption.REPLACE_EXISTING);

		try (ThrowawayGnuPgHome elsewhere = ThrowawayGnuPgHome
			.create(directory.resolve("elsewhere")))
		{
			String foreign = elsewhere.generate(FOREIGN, "ed25519", "sign,cert", "0");
			home.importKeys(elsewhere.exportPublicKey(foreign));
		}

		gpgSays = home.listKeys();
		read = GnuPgHomeSupport.read(home.directory());
	}

	@AfterAll
	static void stopTheAgent() throws IOException
	{
		home.close();
	}

	private static Path revocationFile(final String fingerprint)
	{
		return home.directory().resolve("openpgp-revocs.d").resolve(fingerprint + ".rev");
	}

	@Test
	@DisplayName("every key gpg lists is read, and nothing else")
	void read_findsEveryKeyGpgLists()
	{
		List<String> listed = gpgSays.stream().map(ListedKey::fingerprint).sorted().toList();
		List<String> found = read.stream().flatMap(ring -> ring.keys().stream())
			.map(KeyState::fingerprint).sorted().toList();

		assertEquals(7, listed.size(), "precondition: gpg lists the seven keys this test wrote");
		assertEquals(listed, found);
	}

	@Test
	@DisplayName("for every key the reader says what gpg says: primary, use, dates, algorithm and whether its secret part is here")
	void read_agreesWithGpg_onEveryKey()
	{
		for (ListedKey listed : gpgSays)
		{
			KeyState key = key(listed.fingerprint());
			String which = listed.fingerprint() + " " + key;

			assertEquals(listed.primary(), key.primary(), which);
			assertEquals(listed.capabilities(), key.usage().toLowerCase(), which);
			assertEquals(listed.created(), key.created(), which);
			assertEquals(listed.expires(), key.expires(), which);
			assertEquals(listed.curve().isEmpty() ? "rsa" + listed.bits() : listed.curve(),
				key.algorithm(), which);
			assertEquals(listed.secretHere() ? SecretPart.HERE : SecretPart.OFFLINE, key.secret(),
				which);
		}
	}

	@Test
	@DisplayName("the release key's shape: the primary key offline, the encryption and the signing subkey here, the signing subkey expiring")
	void read_seesTheReleaseKeysShape()
	{
		KeyRingState release = ring(RELEASE);
		List<KeyState> keys = release.keys();

		assertEquals(releaseFingerprint, release.fingerprint());
		assertEquals(3, keys.size(), keys.toString());
		assertEquals(List.of("SC", "E", "S"), keys.stream().map(KeyState::usage).toList());
		assertEquals(List.of(SecretPart.OFFLINE, SecretPart.HERE, SecretPart.HERE),
			keys.stream().map(KeyState::secret).toList());
		assertTrue(keys.get(0).primary(), "the primary key comes first");
		Duration valid = Duration.between(keys.get(2).created(), keys.get(2).expires());
		assertTrue(valid.toDays() >= 729 && valid.toDays() <= 731,
			"the signing subkey was made to expire in two years: " + valid);
	}

	@ParameterizedTest(name = "{0}: {1}")
	@CsvSource({ "'Release Test <release@example.org>', VALID",
			"'Edwards Test <edwards@example.org>', ABSENT",
			"'Unprotected Test <unprotected@example.org>', NOT_VALID",
			"'Foreign Test <foreign@example.org>', ABSENT" })
	@DisplayName("the revocation certificate gpg keeps counts only when it revokes this very key")
	void read_judgesTheRevocationCertificate(final String userId,
		final RevocationCertificate expected)
	{
		assertEquals(expected, ring(userId).revocationCertificate());
	}

	@Test
	@DisplayName("a revocation certificate whose comment gpg wrote in German, with umlauts, is still read")
	void read_aRevocationCertificateWithAGermanComment_isValid() throws IOException
	{
		Path copy = copyOfTheHome("german-revocation-certificate");
		Path file = copy.resolve(GnuPgHomeSupport.REVOCATIONS)
			.resolve(releaseFingerprint + ".rev");
		// the first line gpg 2.4.8 writes under a German locale, measured on 2026-10-29: a
		// certificate of the maintainer's keyring was refused as unreadable for its umlaut
		String english = Files.readString(file, StandardCharsets.UTF_8);
		Files.writeString(file,
			"Dies ist ein Widerrufszertifikat für den OpenPGP Schlüssel:\n" + english,
			StandardCharsets.UTF_8);

		KeyRingState release = GnuPgHomeSupport.find(GnuPgHomeSupport.read(copy),
			releaseFingerprint).orElseThrow();

		assertEquals(RevocationCertificate.VALID, release.revocationCertificate());
	}

	@Test
	@DisplayName("a certificate imported without its secret part is offline throughout")
	void read_aCertificateWithoutSecret_isOfflineThroughout()
	{
		assertEquals(List.of(SecretPart.OFFLINE),
			ring(FOREIGN).keys().stream().map(KeyState::secret).distinct().toList());
	}

	@Test
	@DisplayName("reading changes nothing in the home, not a byte and not a file")
	void read_writesNothing() throws Exception
	{
		Map<String, String> before = contentOf(home.directory());

		GnuPgHomeSupport.read(home.directory());

		assertEquals(before, contentOf(home.directory()));
	}

	@Test
	@DisplayName("a key file that cannot be read turns every key it might hold into unknown, never into offline")
	void read_anUnreadableKeyFile_makesTheUnmatchedKeysUnknown() throws IOException
	{
		Path copy = copyOfTheHome("unreadable-key-file");
		Files.writeString(copy.resolve("private-keys-v1.d").resolve("0".repeat(40) + ".key"),
			"Key: (this is not what gpg writes\n");

		List<KeyRingState> withAnUnreadableFile = GnuPgHomeSupport.read(copy);

		assertEquals(read.size(), withAnUnreadableFile.size(),
			"the keyring is still read, or this test measures nothing");
		for (KeyRingState ring : withAnUnreadableFile)
		{
			for (KeyState key : ring.keys())
			{
				SecretPart before = key(key.fingerprint()).secret();
				assertEquals(before == SecretPart.HERE ? SecretPart.HERE : SecretPart.UNKNOWN,
					key.secret(), key.toString());
			}
		}
	}

	@Test
	@DisplayName("a home that does not exist is refused with its path")
	void read_aMissingHome_isRefused()
	{
		Path missing = directory.resolve("no-such-home");

		IOException refused = assertThrows(IOException.class,
			() -> GnuPgHomeSupport.read(missing));

		assertTrue(refused.getMessage().contains(missing.toString()), refused.getMessage());
	}

	@Test
	@DisplayName("a home without a keyring holds no keys")
	void read_aHomeWithoutKeyring_holdsNoKeys() throws IOException
	{
		Path empty = Files.createDirectories(directory.resolve("empty-home"));

		assertEquals(List.of(), GnuPgHomeSupport.read(empty));
	}

	@Test
	@DisplayName("a keyring in the format before GnuPG 2.1 is refused by name, not read as empty")
	void read_aLegacyKeyring_isRefused() throws IOException
	{
		Path legacy = Files.createDirectories(directory.resolve("legacy-home"));
		Files.write(legacy.resolve("pubring.gpg"), home.exportPublicKey(releaseFingerprint));

		IOException refused = assertThrows(IOException.class, () -> GnuPgHomeSupport.read(legacy));

		assertTrue(refused.getMessage().contains("pubring.gpg"), refused.getMessage());
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("waysToNameTheReleaseKey")
	@DisplayName("the release key is found by its fingerprint or its key id, written the ways people write them")
	void find_findsTheReleaseKey(final String way, final UnaryOperator<String> written)
	{
		Optional<KeyRingState> found = GnuPgHomeSupport.find(read,
			written.apply(releaseFingerprint));

		assertTrue(found.isPresent(), way);
		assertEquals(releaseFingerprint, found.get().fingerprint());
	}

	static Stream<Arguments> waysToNameTheReleaseKey()
	{
		return Stream.of(Arguments.of("the fingerprint", UnaryOperator.identity()),
			Arguments.of("in lower case", (UnaryOperator<String>)String::toLowerCase),
			Arguments.of("in groups of four, as gpg prints it",
				(UnaryOperator<String>)fingerprint -> fingerprint.replaceAll("(.{4})", "$1 ")),
			Arguments.of("the long key id", (UnaryOperator<String>)fingerprint -> fingerprint
				.substring(fingerprint.length() - 16)),
			Arguments.of("with a 0x in front", (UnaryOperator<String>)fingerprint -> "0x"
				+ fingerprint));
	}

	@ParameterizedTest(name = "\"{0}\"")
	@CsvSource({ "0000000000000000", "'not a key'", "'   '" })
	@DisplayName("a name that is no key of the home finds nothing")
	void find_findsNothing_forANameThatIsNoKeyHere(final String name)
	{
		assertFalse(GnuPgHomeSupport.find(read, name).isPresent());
	}

	@Test
	@DisplayName("a subkey's fingerprint does not stand for its certificate")
	void find_aSubkeysFingerprint_findsNothing()
	{
		String subkey = ring(RELEASE).keys().get(1).fingerprint();

		assertFalse(GnuPgHomeSupport.find(read, subkey).isPresent());
	}

	@Test
	@DisplayName("the default home is GNUPGHOME when it is set, as gpg does it")
	void defaultHome_isGnupgHome_whenSet()
	{
		assertEquals(Path.of("/somewhere/gnupg"), GnuPgHomeSupport
			.defaultHome(Map.of("GNUPGHOME", "/somewhere/gnupg"), "/home/someone"));
	}

	@ParameterizedTest(name = "GNUPGHOME \"{0}\"")
	@CsvSource(value = { "''", "'  '" })
	@DisplayName("the default home is ~/.gnupg when GNUPGHOME is blank")
	void defaultHome_isDotGnupg_whenGnupgHomeIsBlank(final String blank)
	{
		assertEquals(Path.of("/home/someone", ".gnupg"),
			GnuPgHomeSupport.defaultHome(Map.of("GNUPGHOME", blank), "/home/someone"));
	}

	@Test
	@DisplayName("the default home is ~/.gnupg when GNUPGHOME is not set at all")
	void defaultHome_isDotGnupg_whenGnupgHomeIsMissing()
	{
		assertEquals(Path.of("/home/someone", ".gnupg"),
			GnuPgHomeSupport.defaultHome(Map.of(), "/home/someone"));
	}

	private static KeyRingState ring(final String userId)
	{
		return read.stream().filter(ring -> ring.userId().equals(userId)).findFirst()
			.orElseThrow(() -> new AssertionError("no certificate of " + userId + " in " + read));
	}

	private static KeyState key(final String fingerprint)
	{
		KeyState found = read.stream().flatMap(ring -> ring.keys().stream())
			.filter(key -> key.fingerprint().equals(fingerprint)).findFirst().orElse(null);
		assertNotNull(found, "the reader has no key " + fingerprint);
		return found;
	}

	private static Path copyOfTheHome(final String name) throws IOException
	{
		Path copy = directory.resolve(name);
		try (Stream<Path> files = Files.walk(home.directory()))
		{
			for (Path file : files.toList())
			{
				Path target = copy.resolve(home.directory().relativize(file).toString());
				if (Files.isDirectory(file))
				{
					Files.createDirectories(target);
				}
				else if (Files.isRegularFile(file))
				{
					Files.copy(file, target);
				}
			}
		}
		return copy;
	}

	/** Every file of the directory with the SHA-256 of its content */
	private static Map<String, String> contentOf(final Path root)
		throws IOException, NoSuchAlgorithmException
	{
		Map<String, String> content = new TreeMap<>();
		try (Stream<Path> files = Files.walk(root))
		{
			for (Path file : files.filter(Files::isRegularFile).toList())
			{
				content.put(root.relativize(file).toString(), HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))));
			}
		}
		return content;
	}
}
