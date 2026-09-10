package io.github.astrapi69.mystic.crypt.plugin.checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Writing the checksum file, the half of the tool that was missing: it could say whether a file
 * matched a published checksum, and it could show a hash, but it could not produce the artifact
 * people exchange (#296).
 * <p>
 * The written form is the coreutils one, so {@code sha256sum -c} reads it - and so does this tool
 * itself, which is the property the round-trip test pins: what it writes, it can read back.
 */
class ChecksumFileWritingTest
{

	@ParameterizedTest(name = "{0} names its file .{1}")
	@CsvSource({ "SHA-256, sha256", "SHA-512, sha512", "SHA-384, sha384", "SHA-1, sha1",
			"MD5, md5", "MD2, md2" })
	@DisplayName("the extension is the algorithm as the coreutils tools spell it")
	void theExtensionIsTheAlgorithmWithoutItsPunctuation(final String algorithm,
		final String expected)
	{
		assertEquals(expected, ChecksumSupport.checksumFileExtension(algorithm));
	}

	@Test
	@DisplayName("the checksum file sits next to its file and keeps the whole name")
	void theChecksumFileSitsNextToTheFileItDescribes(@TempDir final File directory)
	{
		File described = new File(directory, "installer.jar");

		File checksumFile = ChecksumSupport.checksumFileFor(described, "SHA-256");

		assertEquals(new File(directory, "installer.jar.sha256"), checksumFile,
			"appended, not replacing the extension: 'installer.jar.sha256' says which file it "
				+ "belongs to, 'installer.sha256' does not");
	}

	@Test
	@DisplayName("what it writes, sha256sum reads - and so does this tool")
	void theWrittenFileIsTheCoreutilsFormAndReadsBack(@TempDir final File directory)
		throws Exception
	{
		File described = new File(directory, "installer.jar");
		Files.writeString(described.toPath(), "the bytes that get hashed");
		String checksum = ChecksumSupport.checksumOfFile(described, "SHA-256");

		File written = ChecksumSupport.writeChecksumFile(described, "SHA-256", checksum);

		assertTrue(written.isFile(), "the checksum file has to exist afterwards");
		String content = Files.readString(written.toPath(), StandardCharsets.UTF_8);
		assertEquals(checksum + "  installer.jar\n", content,
			"two spaces and the bare file name: that is what sha256sum -c expects, and the name "
				+ "has no directory in it so the file can be checked next to its download");
		assertEquals(checksum, ChecksumSupport.hashFrom(content),
			"and the tool reads its own output back - a format it writes but cannot verify would "
				+ "be worse than none");
	}

	@Test
	@DisplayName("a checksum that is not one is refused instead of being written")
	void anEmptyChecksumIsRefused(@TempDir final File directory) throws Exception
	{
		File described = new File(directory, "installer.jar");
		Files.writeString(described.toPath(), "content");

		assertThrows(IllegalArgumentException.class,
			() -> ChecksumSupport.writeChecksumFile(described, "SHA-256", "   "),
			"writing a file that holds no hash produces an artifact that fails every check later, "
				+ "with nothing saying why");
	}
}
