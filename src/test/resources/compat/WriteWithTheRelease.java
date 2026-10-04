import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Runs inside a child JVM with nothing but a published release's application jar on its class
 * path, and writes a vault through that release's own password save path - the counterpart of
 * OpenWithTheRelease, for the other direction (#490).
 * <p>
 * The vault carries what the release can write: every field of the sample vault the tests use,
 * plus an entry's history and the names of its protected properties, which 8.6 introduced. What
 * it wrote is printed one line per entry, in the shape SampleVaults prints, so the test compares
 * the same text on both sides.
 * <p>
 * Usage: {@code java -cp <release jar> WriteWithTheRelease.java <vault file>}, the master password
 * on standard input. Exits 0 and prints WRITTEN when the vault is on disk.
 */
public class WriteWithTheRelease
{

	/** Printed in UTF-8 whatever the locale says, as in OpenWithTheRelease (#456) */
	private static final PrintStream OUT = new PrintStream(
		new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

	public static void main(String[] arguments) throws Exception
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		File vault = new File(arguments[0]);
		char[] password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip()
			.toCharArray();

		MysticCryptEntryModelBean entry = MysticCryptEntryModelBean.builder()
			.title("the bank".toCharArray()).userName("account holder".toCharArray())
			.password("written by the release".toCharArray())
			.url("https://bank.example.org".toCharArray())
			.notes("Grüße, with umlauts".toCharArray()).keePassIconIndex(57)
			.resources(new ArrayList<>(List.of(FileContentInfo.builder().name("codes.txt")
				.content("the recovery codes".getBytes(StandardCharsets.UTF_8)).build())))
			.properties(new ArrayList<>(List.of(KeyValuePair.<String, String> builder()
				.key("TOTP seed").value("JBSWY3DPEHPK3PXP").build())))
			.build();
		entry.setHistory(new ArrayList<>(List.of(
			MysticCryptEntryModelBean.builder().title("the bank, before".toCharArray()).build())));
		entry.setProtectedPropertyKeys(Set.of("TOTP seed"));
		MysticCryptEntryModelBean second = MysticCryptEntryModelBean.builder()
			.title("the mail".toCharArray()).password("also written by the release".toCharArray())
			.build();
		Map<Long, List<MysticCryptEntryModelBean>> dataOfNodes = new LinkedHashMap<>();
		dataOfNodes.put(1L, new ArrayList<>(List.of(entry)));
		dataOfNodes.put(2L, new ArrayList<>(List.of(second)));
		ApplicationModelBean model = ApplicationModelBean.builder().dataOfNodes(dataOfNodes)
			.lastId(2L).build();
		model.setMasterPwFileModelBean(MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password)
			.withMasterPw(true).withKeyFile(false).build());

		ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);

		for (Map.Entry<Long, List<MysticCryptEntryModelBean>> node : dataOfNodes.entrySet())
		{
			for (MysticCryptEntryModelBean written : node.getValue())
			{
				OUT.println("ENTRY node=" + node.getKey() + " title=" + text(written.getTitle())
					+ " userName=" + text(written.getUserName()) + " password="
					+ text(written.getPassword()) + " url=" + text(written.getUrl()) + " notes="
					+ text(written.getNotes()) + " properties=" + written.getProperties().size()
					+ " attachments=" + written.getResources().size());
			}
		}
		OUT.println("WRITTEN");
	}

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}
}
