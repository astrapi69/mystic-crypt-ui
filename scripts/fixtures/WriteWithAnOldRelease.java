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
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes a vault with a release older than 8.6, through that release's own save path, the way the
 * release builds a new vault: a root group and a "mykeys" group from its own tree factory, and one
 * entry in each group with every field that release has. Reflection only, so one source runs
 * against both API generations (7.x: swing.tree, 8.x: swing.renderer.tree and swing.tree.factory)
 * and against String and char[] fields alike. It wrote the vaults of 7.2 and 7.3 in
 * {@code src/testSupport/resources/compat/vaults} (#624), through {@code write-vaults-of-7x.sh}.
 * <p>
 * Usage: {@code java -cp <release app jar> WriteWithAnOldRelease.java <vault file>}, the password
 * on standard input; with {@code --key-file <PEM file>} after the vault file, the vault is protected
 * by that key alone, through the release's key-file save path; with
 * {@code --password-and-key-file <PEM file>}, by the password and that key together, through the
 * release's save path for both (#650). With {@code --with-keepass-import <kdbx> <its password>}
 * the release's own "Import from KeePass" adds that database's groups and entries under the root;
 * with {@code --with-lethenon-wallet <network>} the release's lethenon plugin, whose classes and
 * lib jars stand on the class path, adds a new wallet through the host's plugin vault access. It
 * prints what it wrote, one line per entry ({@code EntryLines}), then WRITTEN. Nothing of this
 * checkout is on the class path.
 */
public class WriteWithAnOldRelease
{

	private static final PrintStream OUT = new PrintStream(
		new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8);

	private static final OffsetDateTime WHEN = OffsetDateTime.of(2024, 5, 6, 7, 8, 9, 0,
		ZoneOffset.ofHours(2));

	public static void main(String[] arguments) throws Exception
	{
		Security.addProvider((Provider)Class
			.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").getDeclaredConstructor()
			.newInstance());
		File vault = new File(arguments[0]).getAbsoluteFile();
		char[] password = new String(System.in.readAllBytes(), StandardCharsets.UTF_8).strip()
			.toCharArray();

		Class<?> modelType = Class.forName("io.github.astrapi69.mystic.crypt.ApplicationModelBean");
		Class<?> entryType = Class
			.forName("io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean");
		Class<?> credentialsType = Class
			.forName("io.github.astrapi69.mystic.crypt.panel.signin.MasterPwFileModelBean");

		Class<?> elementType = firstClass("io.github.astrapi69.swing.renderer.tree.GenericTreeElement",
			"io.github.astrapi69.swing.tree.GenericTreeElement");
		Class<?> factoryType = firstClass("io.github.astrapi69.swing.tree.factory.BaseTreeNodeFactory",
			"io.github.astrapi69.swing.tree.BaseTreeNodeFactory");
		Class<?> transformerType = Class
			.forName("io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer");
		Object idGenerator = Class.forName("io.github.astrapi69.id.generate.LongIdGenerator")
			.getMethod("of", Long.class).invoke(null, 0L);

		// the entries live in their group's element, as the release keeps them: its tree panel
		// reads and writes the element's default content, and the model's dataOfNodes is a field
		// no code of 7.x or 8.x fills (measured on the 7.3 application jar: only
		// ApplicationModelBean itself names it)
		Object first = newEntry(entryType, "the bank", "account holder", "written by the release",
			"https://bank.example.org", "Grüße, with umlauts", true);
		Object second = newEntry(entryType, "the mail", null, "also written by the release", null,
			null, false);
		Object rootElement = treeElement(elementType, "root", "io/github/astrapi69/silk/icons/book.png",
			first);
		Object keysElement = treeElement(elementType, "mykeys",
			"io/github/astrapi69/silk/icons/folder.png", second);
		Method initialize = Overloads.method(factoryType, "initializeTreeNodeWithTreeElement",
			rootElement, null, idGenerator);
		Object rootNode = initialize.invoke(null, rootElement, null, idGenerator);
		Object keysNode = initialize.invoke(null, keysElement, rootNode, idGenerator);
		Long keysId = (Long)keysNode.getClass().getMethod("getId").invoke(keysNode);
		int keePassImport = indexOf(arguments, "--with-keepass-import");
		if (0 < keePassImport)
		{
			keysId = importKeePass(rootNode, idGenerator,
				new File(arguments[keePassImport + 1]).getAbsoluteFile(), arguments[keePassImport + 2]);
		}
		int wallet = indexOf(arguments, "--with-lethenon-wallet");
		if (0 < wallet)
		{
			addLethenonWallet(rootElement, keysElement, arguments[wallet + 1], password);
		}
		Map<?, ?> treeAsMap = (Map<?, ?>)Overloads.method(transformerType, "toKeyMap", rootNode)
			.invoke(null, rootNode);

		Object credentials = credentialsType.getDeclaredConstructor().newInstance();
		Class<?> fileInfoType = credentialsType.getDeclaredField("applicationFileInfo").getType();
		set(credentials, "applicationFileInfo",
			fileInfoType.getMethod("toFileInfo", File.class).invoke(null, vault));
		set(credentials, "selectedApplicationFilePath", vault.getAbsolutePath());
		boolean withKeyFile = arguments.length >= 3 && "--key-file".equals(arguments[1]);
		boolean withBoth = arguments.length >= 3 && "--password-and-key-file".equals(arguments[1]);
		if (withBoth)
		{
			// the release's save path for both reads the key from the key file itself
			set(credentials, "keyFileInfo", fileInfoType.getMethod("toFileInfo", File.class)
				.invoke(null, new File(arguments[2]).getAbsoluteFile()));
			set(credentials, "privateKeyInfo", keyModelOf(new File(arguments[2])));
			set(credentials, "masterPw", password);
			set(credentials, "withMasterPw", true);
			set(credentials, "withKeyFile", true);
		}
		else if (withKeyFile)
		{
			set(credentials, "privateKeyInfo", keyModelOf(new File(arguments[2])));
			set(credentials, "withMasterPw", false);
			set(credentials, "withKeyFile", true);
		}
		else
		{
			set(credentials, "masterPw", password);
			set(credentials, "withMasterPw", true);
			set(credentials, "withKeyFile", false);
		}

		Object model = modelType.getDeclaredConstructor().newInstance();
		set(model, "rootTreeAsMap", treeAsMap);
		set(model, "lastId", keysId);
		set(model, "masterPwFileModelBean", credentials);

		Class.forName("io.github.astrapi69.mystic.crypt.app.file.xml.ApplicationXmlFileStoreWorker")
			.getMethod(withBoth
				? "saveToFileWithPasswordAndPrivateKey"
				: withKeyFile ? "saveToFileWithPrivateKey" : "saveToFileWithPassword", modelType)
			.invoke(null, model);
		List<String> lines = EntryLines.of(model);
		OUT.println("GROUPS " + treeAsMap.size() + " ENTRIES " + lines.size() + " FIELDS "
			+ fieldsOf(entryType));
		lines.forEach(OUT::println);
		OUT.println("WRITTEN");
	}

	private static int indexOf(final String[] arguments, final String flag)
	{
		for (int index = 1; index < arguments.length; index++)
		{
			if (flag.equals(arguments[index]))
			{
				return index;
			}
		}
		return -1;
	}

	/**
	 * Imports a KeePass database into the tree the way the release's own "Import from KeePass"
	 * does: {@code KdbxDatabase.read} with the release's KeePassJava2, then
	 * {@code KeePassTreeConverter.toTreeNode} under the root, ids from the release's generator. Only
	 * releases that have the converter (8.x with #384) can do it
	 *
	 * @return the last id the import gave out, which the model records
	 */
	private static Long importKeePass(final Object rootNode, final Object idGenerator,
		final File kdbx, final String kdbxPassword) throws Exception
	{
		// four generations of KeePassJava2 across 8.x, each with the call its release makes: 8.1.1
		// SimpleDatabase.load with KdbxCreds, then JacksonDatabase.load, in 8.7 and 8.8
		// KdbxDatabase.load with KdbxCredentials, from 8.9 KdbxDatabase.read
		String[][] generations = {
				{ "org.linguafranca.pwdb.format.KdbxCredentials",
						"org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase", "read" },
				{ "org.linguafranca.pwdb.format.KdbxCredentials",
						"org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase", "load" },
				{ "org.linguafranca.pwdb.kdbx.KdbxCreds",
						"org.linguafranca.pwdb.kdbx.jackson.JacksonDatabase", "load" },
				{ "org.linguafranca.pwdb.kdbx.KdbxCreds",
						"org.linguafranca.pwdb.kdbx.simple.SimpleDatabase", "load" } };
		Object database = null;
		for (String[] generation : generations)
		{
			Class<?> credentialsType;
			Class<?> databaseType;
			try
			{
				credentialsType = Class.forName(generation[0]);
				databaseType = Class.forName(generation[1]);
			}
			catch (ClassNotFoundException notThisGeneration)
			{
				continue;
			}
			Object credentials = credentialsType.getConstructor(byte[].class)
				.newInstance((Object)kdbxPassword.getBytes(StandardCharsets.UTF_8));
			try (java.io.InputStream in = new java.io.FileInputStream(kdbx))
			{
				Method read;
				try
				{
					read = Overloads.method(databaseType, generation[2], credentials, in);
				}
				catch (NoSuchMethodException notThisGeneration)
				{
					continue;
				}
				database = read.invoke(null, credentials, in);
			}
			break;
		}
		if (database == null)
		{
			throw new ClassNotFoundException("no KeePassJava2 database class of a known generation");
		}
		Object rootGroup = database.getClass().getMethod("getRootGroup").invoke(database);
		Method nextId = idGenerator.getClass().getMethod("getNextId");
		Long[] last = { null };
		java.util.function.Supplier<Long> ids = () -> {
			try
			{
				last[0] = (Long)nextId.invoke(idGenerator);
				return last[0];
			}
			catch (ReflectiveOperationException failed)
			{
				throw new IllegalStateException(failed);
			}
		};
		Class<?> converter = Class.forName("io.github.astrapi69.mystic.crypt.keepass.KeePassTreeConverter");
		Overloads.method(converter, "toTreeNode", rootGroup, rootNode, ids).invoke(null, rootGroup,
			rootNode, ids);
		OUT.println("IMPORTED " + kdbx.getName() + " up to id " + last[0]);
		return last[0];
	}

	/**
	 * Adds a lethenon wallet the way the release's lethenon plugin does: {@code WalletVault.create}
	 * seals a new wallet with lethenon's {@code WalletFile}, {@code VaultWalletEntry.write} puts it
	 * on a new entry, and the host's own plugin vault access, {@code MarkedEntryAccess}, adds it to
	 * the vault - the one path a plugin has into a vault (#675). The plugin's classes and its lib
	 * jars come from the release's installer and stand on the class path beside the application
	 * jar; the wallet is sealed with the vault's password, so no second password is written down
	 */
	private static void addLethenonWallet(final Object rootElement, final Object keysElement,
		final String network, final char[] password) throws Exception
	{
		Class<?> walletVault = Class.forName("io.github.astrapi69.mystic.crypt.plugin.lethenon.WalletVault");
		Method create = walletVault.getDeclaredMethod("create", String.class, char[].class);
		create.setAccessible(true);
		Object created = create.invoke(null, network, password.clone());
		Method entryOfCreated = created.getClass().getDeclaredMethod("entry");
		entryOfCreated.setAccessible(true);
		Object walletEntry = entryOfCreated.invoke(created);

		Class<?> entryType = Class
			.forName("io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean");
		Object entry = entryType.getDeclaredConstructor().newInstance();
		set(entry, "id", UUID.randomUUID());
		Class<?> vaultWalletEntry = Class
			.forName("io.github.astrapi69.mystic.crypt.plugin.lethenon.VaultWalletEntry");
		Method write = vaultWalletEntry.getDeclaredMethod("write", walletEntry.getClass(), entryType);
		write.setAccessible(true);
		write.invoke(null, walletEntry, entry);

		@SuppressWarnings("unchecked")
		List<Object> rootEntries = (List<Object>)rootElement.getClass().getMethod("getDefaultContent")
			.invoke(rootElement);
		@SuppressWarnings("unchecked")
		List<Object> keysEntries = (List<Object>)keysElement.getClass().getMethod("getDefaultContent")
			.invoke(keysElement);
		Class<?> stateType = Class.forName("io.github.astrapi69.mystic.crypt.plugin.vault.VaultState");
		Object state = java.lang.reflect.Proxy.newProxyInstance(stateType.getClassLoader(),
			new Class<?>[] { stateType }, (proxy, method, parameters) -> switch (method.getName())
			{
				case "isOpen", "isUnlocked" -> true;
				case "readOnlyReason" -> null;
				case "entryLists" -> List.of(rootEntries, keysEntries);
				case "rootEntries" -> rootEntries;
				case "markChanged" -> null;
				default -> throw new UnsupportedOperationException(method.getName());
			});
		Object access = Class.forName("io.github.astrapi69.mystic.crypt.plugin.vault.MarkedEntryAccess")
			.getConstructor(String.class, stateType).newInstance("lethenon.wallet", state);
		access.getClass().getMethod("add", entryType).invoke(access, entry);
		OUT.println("WALLET " + network + " added through MarkedEntryAccess");
	}

	/**
	 * The release's own key model of the private key in the given PEM file, read with the release's
	 * own reader, the way its sign-in reads a key file
	 */
	private static Object keyModelOf(final File keyFile) throws Exception
	{
		Object privateKey = Class.forName("io.github.astrapi69.crypt.data.key.reader.PrivateKeyReader")
			.getMethod("readPemPrivateKey", File.class).invoke(null, keyFile);
		return Class.forName("io.github.astrapi69.crypt.data.key.KeyModelExtensions")
			.getMethod("toKeyModel", java.security.PrivateKey.class).invoke(null, privateKey);
	}

	private static Object treeElement(final Class<?> elementType, final String name,
		final String iconPath, final Object entry) throws Exception
	{
		Object builder = elementType.getMethod("builder").invoke(null);
		builder.getClass().getMethod("name", String.class).invoke(builder, name);
		builder.getClass().getMethod("iconPath", String.class).invoke(builder, iconPath);
		builder.getClass().getMethod("withText", boolean.class).invoke(builder, true);
		Object element = builder.getClass().getMethod("build").invoke(builder);
		List<Object> content = new ArrayList<>(List.of(entry));
		Overloads.method(elementType, "setDefaultContent", content).invoke(element, content);
		return element;
	}

	private static Object newEntry(final Class<?> entryType, final String title,
		final String userName, final String password, final String url, final String notes,
		final boolean everything) throws Exception
	{
		Object entry = entryType.getDeclaredConstructor().newInstance();
		set(entry, "id", UUID.randomUUID());
		set(entry, "title", title);
		set(entry, "userName", userName);
		set(entry, "password", password);
		set(entry, "url", url);
		set(entry, "notes", notes);
		if (!everything)
		{
			return entry;
		}
		set(entry, "expires", LocalDate.of(2030, 1, 2));
		setIfPresent(entry, "preciseExpiryTime", WHEN);
		setIfPresent(entry, "creationTime", WHEN);
		setIfPresent(entry, "lastAccessTime", WHEN);
		setIfPresent(entry, "lastModificationTime", WHEN);
		setIfPresent(entry, "keePassIconIndex", 57);

		Object attachment = elementOf(entryType, "resources").getDeclaredConstructor().newInstance();
		set(attachment, "name", "codes.txt");
		set(attachment, "content", "the recovery codes".getBytes(StandardCharsets.UTF_8));
		set(entry, "resources", new ArrayList<>(List.of(attachment)));

		Object property = elementOf(entryType, "properties").getDeclaredConstructor().newInstance();
		set(property, "key", "TOTP seed");
		set(property, "value", "JBSWY3DPEHPK3PXP");
		set(entry, "properties", new ArrayList<>(List.of(property)));

		Field modifications = fieldOrNull(entryType, "dateTimesOfModification");
		if (modifications != null)
		{
			Object pair = modifications.getType().getDeclaredConstructor().newInstance();
			set(pair, "key", "title");
			set(pair, "values", new LinkedHashSet<>(List.of(WHEN, WHEN.plusDays(1))));
			set(entry, "dateTimesOfModification", pair);
		}
		return entry;
	}

	private static Class<?> elementOf(final Class<?> owner, final String listField)
		throws Exception
	{
		ParameterizedType listType = (ParameterizedType)owner.getDeclaredField(listField)
			.getGenericType();
		Object argument = listType.getActualTypeArguments()[0];
		return (Class<?>)(argument instanceof ParameterizedType parameterized
			? parameterized.getRawType()
			: argument);
	}

	private static Class<?> firstClass(final String... names) throws ClassNotFoundException
	{
		for (String name : names)
		{
			try
			{
				return Class.forName(name);
			}
			catch (ClassNotFoundException absent)
			{
				// the other API generation
			}
		}
		throw new ClassNotFoundException(String.join(" or ", names));
	}

	private static Field fieldOrNull(final Class<?> type, final String name)
	{
		for (Class<?> current = type; current != null; current = current.getSuperclass())
		{
			try
			{
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			}
			catch (NoSuchFieldException absent)
			{
				// look in the superclass
			}
		}
		return null;
	}

	private static void setIfPresent(final Object target, final String name, final Object value)
		throws Exception
	{
		if (fieldOrNull(target.getClass(), name) != null)
		{
			set(target, name, value);
		}
	}

	private static void set(final Object target, final String name, final Object value)
		throws Exception
	{
		Field field = fieldOrNull(target.getClass(), name);
		if (field == null)
		{
			throw new NoSuchFieldException(target.getClass().getName() + "." + name);
		}
		Object converted = value;
		if (value instanceof String text && field.getType() == char[].class)
		{
			converted = text.toCharArray();
		}
		else if (value instanceof char[] characters && field.getType() == String.class)
		{
			converted = new String(characters);
		}
		field.set(target, converted);
	}

	private static String fieldsOf(final Class<?> type)
	{
		List<String> names = new ArrayList<>();
		for (Field field : type.getDeclaredFields())
		{
			if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()))
			{
				names.add(field.getName());
			}
		}
		return String.join(",", names);
	}
}
