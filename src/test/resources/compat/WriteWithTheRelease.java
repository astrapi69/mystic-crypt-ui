import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.astrapi69.collection.pair.KeyValuePair;
import io.github.astrapi69.file.create.model.FileContentInfo;
import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.TreeIdNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Runs inside a child JVM with nothing but a published release's application jar on its class
 * path, and writes a vault through that release's own password save path - the counterpart of
 * OpenWithTheRelease, for the other direction (#490).
 * <p>
 * The vault carries what the release can write: every field of the sample vault the tests use,
 * plus an entry's history and the names of its protected properties, which 8.6 introduced, in a
 * root group and a group "mykeys" with each entry in its group's element, where the application
 * keeps entries (#757). What it wrote is printed one line per entry, sorted, in the shape
 * SampleVaults prints, so the test compares the same text on both sides.
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
		// the entries live in their group's element, where the release's tree panel reads them;
		// dataOfNodes is a field no code of the application fills (#757)
		BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root = BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(1L)
			.value(group("root", entry)).leaf(false).build();
		root.addChild(BaseTreeNode
			.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder().id(2L)
			.value(group("mykeys", second)).parent(root).leaf(false).build());
		Map<Long, TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long>> tree = BaseTreeNodeTransformer
			.toKeyMap(root);
		ApplicationModelBean model = ApplicationModelBean.builder().rootTreeAsMap(tree).lastId(2L)
			.build();
		model.setMasterPwFileModelBean(MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password)
			.withMasterPw(true).withKeyFile(false).build());

		ApplicationXmlFileStoreWorker.saveToFileWithPassword(model);

		List<String> lines = new ArrayList<>();
		for (TreeIdNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node : tree.values())
		{
			for (MysticCryptEntryModelBean written : node.getValue().getDefaultContent())
			{
				lines.add("ENTRY group=" + node.getValue().getName() + " title="
					+ text(written.getTitle()) + " userName=" + text(written.getUserName())
					+ " password=" + text(written.getPassword()) + " url=" + text(written.getUrl())
					+ " notes=" + text(written.getNotes()) + " properties="
					+ written.getProperties().size() + " attachments="
					+ written.getResources().size());
			}
		}
		lines.stream().sorted().forEach(OUT::println);
		OUT.println("WRITTEN");
	}

	private static GenericTreeElement<List<MysticCryptEntryModelBean>> group(final String name,
		final MysticCryptEntryModelBean entry)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).build();
		element.setDefaultContent(new ArrayList<>(List.of(entry)));
		return element;
	}

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}
}
