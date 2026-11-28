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
package io.github.astrapi69.mystic.crypt.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.linguafranca.pwdb.format.KdbxCredentials;
import org.linguafranca.pwdb.kdbx.jackson.KdbxDatabase;
import org.linguafranca.pwdb.kdbx.jackson.KdbxEntry;
import org.linguafranca.pwdb.kdbx.jackson.KdbxGroup;
import org.linguafranca.pwdb.kdbx.jackson.KdbxHistory;
import org.linguafranca.pwdb.kdbx.jackson.model.Times;

import io.github.astrapi69.gen.tree.BaseTreeNode;
import io.github.astrapi69.gen.tree.convert.BaseTreeNodeTransformer;
import io.github.astrapi69.id.generate.LongIdGenerator;
import io.github.astrapi69.mystic.crypt.ApplicationModelBean;
import io.github.astrapi69.mystic.crypt.TestPasswords;
import io.github.astrapi69.mystic.crypt.app.file.xml.VaultXmlCodec;
import io.github.astrapi69.mystic.crypt.keepass.KeePassEntryConverter;
import io.github.astrapi69.mystic.crypt.keepass.KeePassLibraryFields;
import io.github.astrapi69.mystic.crypt.panel.dbtree.MysticCryptEntryModelBean;
import io.github.astrapi69.swing.renderer.tree.GenericTreeElement;

/**
 * Importing a KeePass file whose entries the vault already holds (#1119): an entry is matched by
 * its UUID whatever its group, stays in the group the vault has it in, and of the two versions the
 * one modified later becomes current while the other goes into the entry's history. Equal times
 * keep the vault's version, and two versions with the same content write no history.
 * <p>
 * Every file is written at runtime with a password made up per run and read back through the
 * library before it is imported, the way the import action reads it. The model side of the import,
 * {@link ImportKeePassDatabaseAction#importUnder}, is driven with the conversion the import uses.
 */
class KeePassImportMergeTest
{

	private static final String VAULT_ROOT = "Vault root";
	private static final String KEEPASS_ROOT = "KeePass root";
	private static final String WORK = "Work";
	private static final String PRIVATE = "Private";

	private static final Instant T1 = Instant.parse("2026-01-01T10:00:00Z");
	private static final Instant T2 = Instant.parse("2026-02-01T10:00:00Z");
	private static final Instant T3 = Instant.parse("2026-03-01T10:00:00Z");

	private static final UUID MAIL = UUID.fromString("00000000-0000-4000-8000-00000000000a");
	private static final UUID BANK = UUID.fromString("00000000-0000-4000-8000-00000000000b");
	private static final UUID SHOP = UUID.fromString("00000000-0000-4000-8000-00000000000c");

	private static final String ENTRY_PASSWORD = TestPasswords.throwaway();

	private final String filePassword = TestPasswords.throwaway();

	@TempDir
	Path directory;

	@Test
	@DisplayName("importing the same file twice leaves one entry per UUID, and no history")
	void aSecondImportOfTheSameFile_addsNoEntryAndWritesNoHistory() throws Exception
	{
		Vault vault = new Vault();
		File file = file(Map.of(WORK, List.of(mail("mail", T1), bank("bank", T1)), PRIVATE,
			List.of(shop("shop", T1))));

		vault.importFile(file);
		vault.importFile(file);

		assertEquals(3, vault.entries().size(), "the second import added entries again");
		assertEquals(List.of(1L, 1L, 1L),
			List.of(vault.countOf(MAIL), vault.countOf(BANK), vault.countOf(SHOP)));
		vault.entries().forEach(entry -> assertNull(entry.getHistory(),
			"two versions with the same content wrote a history"));
	}

	@Test
	@DisplayName("a version in the file modified later becomes current, the vault's goes into the history")
	void aLaterVersionInTheFile_becomesCurrent_andTheVaultsGoesIntoTheHistory() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail old", T1), bank("bank", T1)))));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail new", T2)))));

		MysticCryptEntryModelBean mail = vault.only(MAIL);
		assertEquals("mail new", title(mail));
		assertEquals(T2, mail.getLastModificationTime().toInstant());
		assertEquals(List.of("mail old"), titlesOf(mail.getHistory()));
		assertEquals(1L, vault.countOf(BANK), "an entry the file does not hold changed");
		assertNull(vault.only(BANK).getHistory());
	}

	@Test
	@DisplayName("a version in the file modified earlier goes into the history, the vault's stays current")
	void anEarlierVersionInTheFile_goesIntoTheHistory_andTheVaultsStaysCurrent() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail new", T2)))));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail old", T1)))));

		MysticCryptEntryModelBean mail = vault.only(MAIL);
		assertEquals("mail new", title(mail));
		assertEquals(T2, mail.getLastModificationTime().toInstant());
		assertEquals(List.of("mail old"), titlesOf(mail.getHistory()));
	}

	@Test
	@DisplayName("equal modification times keep the vault's version current and put the other one into the history")
	void equalTimes_keepTheVaultsVersion() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail vault", T1)))));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail file", T1)))));

		MysticCryptEntryModelBean mail = vault.only(MAIL);
		assertEquals("mail vault", title(mail));
		assertEquals(List.of("mail file"), titlesOf(mail.getHistory()));
	}

	@Test
	@DisplayName("an entry moved to another group in the file stays in the group of the vault")
	void anEntryInAnotherGroupOfTheFile_staysInItsGroupInTheVault() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail", T1)))));
		assertEquals(List.of(KEEPASS_ROOT, WORK), vault.groupPathOf(MAIL));

		vault.importFile(file(Map.of(PRIVATE, List.of(mail("mail moved", T2), shop("shop", T1)))));

		assertEquals(1L, vault.countOf(MAIL));
		assertEquals(List.of(KEEPASS_ROOT, WORK), vault.groupPathOf(MAIL),
			"the merged entry left its group");
		assertEquals("mail moved", title(vault.only(MAIL)));
		assertEquals(List.of(KEEPASS_ROOT, PRIVATE), vault.groupPathOf(SHOP),
			"an entry with a new UUID is added where the file has it");
	}

	@Test
	@DisplayName("an entry with a UUID the vault does not hold is added, as before")
	void anEntryWithANewUuid_isAdded() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail", T1)))));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail", T1), bank("bank", T1)))));

		assertEquals(2, vault.entries().size());
		assertEquals(1L, vault.countOf(BANK));
	}

	@Test
	@DisplayName("an entry the vault never edited counts as modified at its creation")
	void aMissingModificationTime_countsAsTheCreationTime() throws Exception
	{
		Vault vault = new Vault();
		MysticCryptEntryModelBean neverEdited = vault.addEntryToExistingGroup(MAIL, "mail vault",
			T2, null);

		vault.importFile(file(Map.of(WORK, List.of(mail("mail file", T1)))));

		assertEquals("mail vault", title(neverEdited),
			"a file version edited before the vault's creation replaced it");
		assertEquals(List.of("mail file"), titlesOf(neverEdited.getHistory()));

		Vault later = new Vault();
		MysticCryptEntryModelBean created = later.addEntryToExistingGroup(MAIL, "mail vault", T1,
			null);
		later.importFile(file(Map.of(WORK, List.of(mail("mail file", T3)))));
		assertEquals("mail file", title(created));
		assertEquals(List.of("mail vault"), titlesOf(created.getHistory()));
	}

	@Test
	@DisplayName("an entry that only the node data of the vault holds is found")
	void anEntryOnlyTheNodeDataHolds_isFound() throws Exception
	{
		Vault vault = new Vault();
		MysticCryptEntryModelBean inNodeData = entryBean(MAIL, "mail vault", T1);
		vault.model.setDataOfNodes(new LinkedHashMap<>());
		vault.model.getDataOfNodes().put(1L, new ArrayList<>(List.of(inNodeData)));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail file", T2)))));

		assertEquals("mail file", title(inNodeData));
		assertEquals(0L, vault.countOf(MAIL), "the entry was added to the tree as well");
	}

	@Test
	@DisplayName("a history that is there stays, and the replaced version joins it without a cap")
	void anExistingHistoryStays_andTheReplacedVersionJoinsIt() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail 1", T1)))));
		vault.importFile(file(Map.of(WORK, List.of(mail("mail 2", T2)))));

		vault.importFile(file(Map.of(WORK, List.of(mail("mail 3", T3)))));

		MysticCryptEntryModelBean mail = vault.only(MAIL);
		assertEquals("mail 3", title(mail));
		assertEquals(List.of("mail 1", "mail 2"), titlesOf(mail.getHistory()));
	}

	@Test
	@DisplayName("the history of the file's version is taken over, each version once")
	void theHistoryOfTheFilesVersion_isTakenOver() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail 1", T1)))));
		File withHistory = file(Map.of(WORK, List.of(mail("mail 3", T3, mail("mail 2", T2)))));

		vault.importFile(withHistory);
		vault.importFile(withHistory);

		MysticCryptEntryModelBean mail = vault.only(MAIL);
		assertEquals("mail 3", title(mail));
		assertEquals(List.of("mail 1", "mail 2"), titlesOf(mail.getHistory()));
	}

	@Test
	@DisplayName("a refused import changes no entry of the vault")
	void aRefusedImport_changesNoEntryOfTheVault() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail old", T1)))));
		File newer = file(
			Map.of(WORK, List.of(mail("mail new", T2)), PRIVATE, List.of(shop("shop", T1))));

		assertThrows(NumberFormatException.class,
			() -> ImportKeePassDatabaseAction.importUnder(vault.root, read(newer), vault.model,
				LongIdGenerator.of(vault.model.getLastId() + 1), entry -> {
					if ("shop".equals(entry.getTitle()))
					{
						throw new NumberFormatException("refused by the test");
					}
					return KeePassEntryConverter.toEntryModelBean(entry);
				}));

		assertEquals("mail old", title(vault.only(MAIL)));
		assertNull(vault.only(MAIL).getHistory());
	}

	@Test
	@DisplayName("saved and reopened, the merged vault holds the current versions and their history")
	void savedAndReopened_theMergedEntriesAndTheirHistoryAreThere() throws Exception
	{
		Vault vault = new Vault();
		vault.importFile(file(Map.of(WORK, List.of(mail("mail old", T1), bank("bank", T1)))));
		vault.importFile(file(Map.of(WORK, List.of(mail("mail new", T2), bank("bank", T1)))));

		char[] xml = VaultXmlCodec.toXml(vault.model);
		ApplicationModelBean reopened = VaultXmlCodec.toModel(xml);

		Vault reread = new Vault(reopened);
		assertEquals(2, reread.entries().size());
		MysticCryptEntryModelBean mail = reread.only(MAIL);
		assertEquals("mail new", title(mail));
		assertEquals(T2, mail.getLastModificationTime().toInstant());
		assertEquals(List.of("mail old"), titlesOf(mail.getHistory()));
		assertEquals(T1, mail.getHistory().get(0).getLastModificationTime().toInstant());
		assertNull(reread.only(BANK).getHistory());
	}

	private static String title(final MysticCryptEntryModelBean entry)
	{
		return new String(entry.getTitle());
	}

	private static List<String> titlesOf(final List<MysticCryptEntryModelBean> versions)
	{
		assertTrue(versions != null, "the entry has no history");
		return versions.stream().map(KeePassImportMergeTest::title).toList();
	}

	private static MysticCryptEntryModelBean entryBean(final UUID id, final String title,
		final Instant modified)
	{
		return MysticCryptEntryModelBean.builder().id(id).title(title.toCharArray())
			.password(ENTRY_PASSWORD.toCharArray())
			.lastModificationTime(
				modified == null ? null : OffsetDateTime.ofInstant(modified, ZoneOffset.UTC))
			.build();
	}

	// ---- the files ----

	private record Spec(UUID id, String title, Instant modified, Spec previous) {
	}

	private static Spec mail(final String title, final Instant modified)
	{
		return new Spec(MAIL, title, modified, null);
	}

	private static Spec mail(final String title, final Instant modified, final Spec previous)
	{
		return new Spec(MAIL, title, modified, previous);
	}

	private static Spec bank(final String title, final Instant modified)
	{
		return new Spec(BANK, title, modified, null);
	}

	private static Spec shop(final String title, final Instant modified)
	{
		return new Spec(SHOP, title, modified, null);
	}

	/**
	 * Writes a KeePass file with a root group and one group per key of the given map, each with the
	 * entries of its list, and returns the file; its password is made up per run
	 */
	private File file(final Map<String, List<Spec>> groups) throws Exception
	{
		KdbxDatabase database = new KdbxDatabase();
		KdbxGroup root = database.getRootGroup();
		root.setName(KEEPASS_ROOT);
		for (Map.Entry<String, List<Spec>> group : new LinkedHashMap<>(groups).entrySet())
		{
			KdbxGroup keePassGroup = (KdbxGroup)database.newGroup(group.getKey());
			for (Spec spec : group.getValue())
			{
				keePassGroup.addEntry(entryOf(database, spec));
			}
			root.addGroup(keePassGroup);
		}
		File file = directory.resolve(UUID.randomUUID() + ".kdbx").toFile();
		try (OutputStream stream = new FileOutputStream(file))
		{
			database.write(credentials(), stream);
		}
		return file;
	}

	private static KdbxEntry entryOf(final KdbxDatabase database, final Spec spec)
	{
		KdbxEntry entry = (KdbxEntry)database.newEntry(spec.title());
		entry.setPassword(ENTRY_PASSWORD);
		if (spec.previous() != null)
		{
			KdbxHistory history = new KdbxHistory();
			history.setEntry(new ArrayList<>(List.of(entryOf(database, spec.previous()))));
			KeePassLibraryFields.setHistory(entry, history);
		}
		KeePassLibraryFields.setUuid(entry, spec.id());
		Times times = KeePassLibraryFields.getTimes(entry);
		times.setCreationTime(T1);
		times.setLastModificationTime(spec.modified());
		KeePassLibraryFields.setTimes(entry, times);
		return entry;
	}

	private KdbxCredentials credentials()
	{
		return new KdbxCredentials(filePassword.getBytes(StandardCharsets.UTF_8));
	}

	private KdbxGroup read(final File file) throws Exception
	{
		try (InputStream stream = new FileInputStream(file))
		{
			return KdbxDatabase.read(credentials(), stream).getRootGroup();
		}
	}

	// ---- the vault ----

	/**
	 * An open vault as the import sees it: a live root with one group, and the model whose tree map
	 * was taken from it
	 */
	private final class Vault
	{
		final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> root;
		final ApplicationModelBean model;

		Vault()
		{
			root = node(0L, VAULT_ROOT);
			root.addChild(node(1L, "Existing group"));
			model = new ApplicationModelBean();
			model.setRootTreeAsMap(BaseTreeNodeTransformer.toKeyMap(root));
			model.setLastId(1L);
		}

		/** A vault read back from a saved model, its live root rebuilt from the tree map */
		Vault(final ApplicationModelBean reopened)
		{
			model = reopened;
			root = BaseTreeNodeTransformer.getRoot(reopened.getRootTreeAsMap());
		}

		void importFile(final File file) throws Exception
		{
			ImportKeePassDatabaseAction.importUnder(root, read(file), model,
				LongIdGenerator.of(model.getLastId() + 1), KeePassEntryConverter::toEntryModelBean);
		}

		/** An entry the vault already holds in its first group, as an open vault has it */
		MysticCryptEntryModelBean addEntryToExistingGroup(final UUID id, final String title,
			final Instant created, final Instant modified)
		{
			MysticCryptEntryModelBean entry = entryBean(id, title, modified);
			entry.setCreationTime(OffsetDateTime.ofInstant(created, ZoneOffset.UTC));
			root.getChildren().iterator().next().getValue().getDefaultContent().add(entry);
			return entry;
		}

		List<MysticCryptEntryModelBean> entries()
		{
			List<MysticCryptEntryModelBean> found = new ArrayList<>();
			collect(root, new ArrayList<>(), found, null);
			return found;
		}

		long countOf(final UUID id)
		{
			return entries().stream().filter(entry -> id.equals(entry.getId())).count();
		}

		MysticCryptEntryModelBean only(final UUID id)
		{
			List<MysticCryptEntryModelBean> found = entries().stream()
				.filter(entry -> id.equals(entry.getId())).toList();
			assertEquals(1, found.size(), "the vault holds " + found.size() + " entries of " + id);
			return found.get(0);
		}

		/** The names of the groups from under the vault root down to the one holding the entry */
		List<String> groupPathOf(final UUID id)
		{
			List<List<String>> paths = new ArrayList<>();
			collect(root, new ArrayList<>(), new ArrayList<>(), (path, entry) -> {
				if (id.equals(entry.getId()))
				{
					paths.add(new ArrayList<>(path));
				}
			});
			assertEquals(1, paths.size());
			return paths.get(0);
		}

		private void collect(
			final BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node,
			final List<String> path, final List<MysticCryptEntryModelBean> found,
			final BiConsumer<List<String>, MysticCryptEntryModelBean> visitor)
		{
			List<MysticCryptEntryModelBean> content = node.getValue().getDefaultContent();
			if (content != null)
			{
				for (MysticCryptEntryModelBean entry : content)
				{
					found.add(entry);
					if (visitor != null)
					{
						visitor.accept(path, entry);
					}
				}
			}
			if (node.getChildren() == null)
			{
				return;
			}
			for (BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> child : node
				.getChildren())
			{
				List<String> childPath = new ArrayList<>(path);
				childPath.add(child.getDisplayValue());
				collect(child, childPath, found, visitor);
			}
		}
	}

	private static BaseTreeNode<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> node(
		final long id, final String name)
	{
		GenericTreeElement<List<MysticCryptEntryModelBean>> element = GenericTreeElement
			.<List<MysticCryptEntryModelBean>> builder().name(name).leaf(false).build();
		element.setDefaultContent(new ArrayList<>());
		return BaseTreeNode.<GenericTreeElement<List<MysticCryptEntryModelBean>>, Long> builder()
			.id(id).value(element).displayValue(name).leaf(false).build();
	}
}
