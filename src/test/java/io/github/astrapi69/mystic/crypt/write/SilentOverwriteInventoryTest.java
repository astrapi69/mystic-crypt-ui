package io.github.astrapi69.mystic.crypt.write;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Nothing writes over an existing file without going through the question, and what still does is
 * named here rather than remembered (#300).
 * <p>
 * The application answered "this file already exists" in three ways across 23 call sites: seven
 * refused through five separately written guards whose messages had already drifted apart, two
 * asked, and fourteen overwrote in silence. Two of the fourteen replaced a vault, which is data
 * loss on an ordinary menu item, and those two are migrated.
 * <p>
 * The rest are listed below with what they write. That is the point of this test: the list IS the
 * work that is left, it is visible in a run rather than in somebody's memory, and a file that
 * starts overwriting tomorrow without being on it fails here. A sweep alone would hold until the
 * next feature.
 */
class SilentOverwriteInventoryTest
{

	/** What can replace a file that is already there */
	private static final Pattern OVERWRITING_WRITE = Pattern
		.compile("REPLACE_EXISTING|Files\\.write|Files\\.writeString|Files\\.newBufferedWriter|"
			+ "new FileOutputStream|new FileWriter");

	/** Where the sources live, relative to the module root this test runs in */
	private static final List<String> SOURCE_ROOTS = List.of("src/main/java", "plugins");

	/**
	 * The writers that have NOT been migrated yet, each with what it writes and therefore which
	 * half of the rule it will land in. Removing one from this map without routing it through
	 * {@link OverwriteConfirmation} turns this test red, and so does a new writer appearing
	 * anywhere.
	 * <p>
	 * Derived data asks and then writes; a vault or key material is never replaced silently. The
	 * two vault paths are already gone from this list, which is why it is shorter than the count in
	 * the issue.
	 */
	private static final Map<String, String> NOT_MIGRATED_YET = Map.ofEntries(
		Map.entry("ChecksumSupport.java", "derived: the checksum file, its caller already asks"),
		Map.entry("ConversionSupport.java", "derived: refuses on its own, one of the five guards"),
		Map.entry("FileCryptSupport.java", "derived: refuses on its own"),
		Map.entry("KeyExchangePanel.java", "key material: writes the exchanged key, silent today"),
		Map.entry("KeygenSupport.java", "key material: writes generated keys, silent today"),
		Map.entry("PqcSignaturePanel.java", "derived: writes a signature, silent today"),
		Map.entry("MenuLayoutSupport.java", "internal state: the persisted menu layout"),
		Map.entry("SecretSharingPanel.java", "key material: refuses on its own, twice"),
		Map.entry("ExportKeePassDatabaseAction.java", "derived from the vault: silent today"),
		Map.entry("ApplicationXmlFileStoreWorker.java",
			"the vault itself, writing to its OWN file"),
		Map.entry("VaultFileWriter.java", "the vault itself, writing to its OWN file"),
		Map.entry("PluginSettings.java", "internal state: plugin settings"),
		Map.entry("PluginsSettingsPanel.java", "internal state: copies an installed plugin zip"));

	@Test
	@DisplayName("every file that can overwrite is either behind the question or named as pending")
	void everyOverwritingWriterIsBehindTheQuestionOrOnTheList() throws IOException
	{
		List<Path> sources = collectMainSources();
		assertTrue(sources.size() > 50,
			"the scan has to find the sources at all - an empty file set is not a clean result, "
				+ "it is a broken scan reporting green");

		TreeSet<String> unaccounted = new TreeSet<>();
		for (Path source : sources)
		{
			String content = Files.readString(source, StandardCharsets.UTF_8);
			if (!OVERWRITING_WRITE.matcher(content).find()
				|| content.contains("OverwriteConfirmation"))
			{
				continue;
			}
			String name = source.getFileName().toString();
			if (!NOT_MIGRATED_YET.containsKey(name))
			{
				unaccounted.add(name);
			}
		}

		assertTrue(unaccounted.isEmpty(),
			"these write over an existing file without the question and without being on the "
				+ "pending list: " + unaccounted
				+ ". Route the write through OverwriteConfirmation, or add it to NOT_MIGRATED_YET "
				+ "with what it writes - a fifteenth silent writer arriving unnoticed is exactly "
				+ "what this test exists to stop (#300)");
	}

	/**
	 * Every production source of the application and of its plugins
	 *
	 * @return the java files under the main source sets
	 * @throws IOException
	 *             if the tree cannot be walked
	 */
	private static List<Path> collectMainSources() throws IOException
	{
		List<Path> sources = new ArrayList<>();
		for (String root : SOURCE_ROOTS)
		{
			Path start = Path.of(root);
			if (!Files.isDirectory(start))
			{
				continue;
			}
			try (Stream<Path> walk = Files.walk(start))
			{
				walk.filter(path -> path.toString().endsWith(".java"))
					.filter(path -> path.toString().contains("main" + File.separator + "java"))
					.forEach(sources::add);
			}
		}
		return sources;
	}
}
