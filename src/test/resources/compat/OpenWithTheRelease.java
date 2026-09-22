import java.io.File;
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
			System.out.println("REFUSED " + refused.getMessage());
			for (Throwable cause = refused.getCause(); cause != null; cause = cause.getCause())
			{
				System.out.println("CAUSE " + cause.getClass().getName() + ": "
					+ String.valueOf(cause.getMessage()).lines().findFirst().orElse(""));
			}
			System.exit(2);
			return;
		}
		for (Map.Entry<Long, List<MysticCryptEntryModelBean>> node : model.getDataOfNodes().entrySet())
		{
			for (MysticCryptEntryModelBean entry : node.getValue())
			{
				System.out.println("ENTRY node=" + node.getKey() + " title=" + text(entry.getTitle())
					+ " userName=" + text(entry.getUserName()) + " password="
					+ text(entry.getPassword()) + " url=" + text(entry.getUrl()) + " notes="
					+ text(entry.getNotes()) + " properties=" + entry.getProperties().size()
					+ " attachments=" + entry.getResources().size());
			}
		}
		if (model.getRootTreeAsMap() != null)
		{
			model.getRootTreeAsMap().values().forEach(node -> System.out.println("GROUP name="
				+ node.getValue().getName() + " keepass.creationTime="
				+ node.getValue().getProperties().get("keepass.creationTime") + " keepass.expires="
				+ node.getValue().getProperties().get("keepass.expires")));
		}
		System.out.println("OPENED");
	}

	private static String text(final char[] characters)
	{
		return characters == null ? "" : new String(characters);
	}
}
