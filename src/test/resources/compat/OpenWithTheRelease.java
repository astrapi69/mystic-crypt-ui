import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.Map;

import io.github.astrapi69.file.create.model.FileInfo;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileReader;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean;

/**
 * Run with a RELEASE's application jar on the class path, never this build's: opens the vault named
 * in the first argument through the password sign-in path of that release, the master password
 * arriving on standard input, and prints what it read in a form the test compares (#402).
 * <p>
 * Only APIs that exist in that release may be used here. Exit 0 and "OPENED" when the release opens
 * the vault; exit 2 and "REFUSED" with the release's own message when it does not.
 */
public class OpenWithTheRelease
{

	/**
	 * What this probe prints is compared character by character, so it says which encoding it
	 * prints in rather than inheriting one: {@code System.out} follows this JVM's
	 * {@code stdout.encoding}, which follows the locale, and on a machine with no {@code LANG} an
	 * umlaut leaves as {@code ?} long before the test sees it (#456). The test reads this stream as
	 * UTF-8, and this is the other half of that agreement.
	 */
	private static final PrintStream OUT = new PrintStream(
		new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

	public static void main(String[] arguments) throws Exception
	{
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		File vault = new File(arguments[0]);
		char[] password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip()
			.toCharArray();
		MasterPwFileModelBean credentials = MasterPwFileModelBean.builder()
			.applicationFileInfo(FileInfo.toFileInfo(vault))
			.selectedApplicationFilePath(vault.getAbsolutePath()).masterPw(password)
			.withMasterPw(true).withKeyFile(false).build();
		ApplicationModelBean model;
		try
		{
			model = ApplicationXmlFileReader.readApplicationFileWithPassword(credentials);
		}
		catch (RuntimeException refused)
		{
			OUT.println("REFUSED " + refused.getMessage());
			for (Throwable cause = refused.getCause(); cause != null; cause = cause.getCause())
			{
				OUT.println("CAUSE " + cause.getClass().getName() + ": "
					+ String.valueOf(cause.getMessage()).lines().findFirst().orElse(""));
			}
			System.exit(2);
			return;
		}
		for (Map.Entry<Long, List<MysticCryptEntryModelBean>> node : model.getDataOfNodes().entrySet())
		{
			for (MysticCryptEntryModelBean entry : node.getValue())
			{
				OUT.println("ENTRY node=" + node.getKey() + " title=" + text(entry.getTitle())
					+ " userName=" + text(entry.getUserName()) + " password="
					+ text(entry.getPassword()) + " url=" + text(entry.getUrl()) + " notes="
					+ text(entry.getNotes()) + " properties=" + entry.getProperties().size()
					+ " attachments=" + entry.getResources().size());
			}
		}
		if (model.getRootTreeAsMap() != null)
		{
			model.getRootTreeAsMap().values().forEach(node -> OUT.println("GROUP name="
				+ node.getValue().getName() + " keepass.creationTime="
				+ node.getValue().getProperties().get("keepass.creationTime") + " keepass.expires="
				+ node.getValue().getProperties().get("keepass.expires")));
		}
		OUT.println("OPENED");
	}

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}
}
