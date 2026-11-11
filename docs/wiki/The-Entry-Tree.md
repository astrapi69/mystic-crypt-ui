# The Entry Tree

Once you are signed in, the main window shows your database as a tree on the left and a table of
entries on the right. Everything you do with your passwords happens here: you build the tree out
of nodes, you put entries into those nodes, and you move both around as your collection grows.

This page covers the whole surface: nodes, entries, moving, searching, copying credentials, and
the two view modes the window can be in.

## 1. Nodes and entries

There are exactly two kinds of thing in the database:

- A **node** is a place in the tree. It has a name, it can hold other nodes, and it holds a list
  of entries. Nodes are what you see in the tree on the left. A node can also be marked as a
  **leaf**, which means it cannot hold child nodes.
- An **entry** is a single stored secret: a title, a user name, a password, a URL, notes and more
  (see section 6). Entries never appear in the tree. They appear in the table on the right, and
  the table always shows the entries of the node that is currently selected in the tree.

Click a node in the tree and the table reloads with that node's entries. That is the basic rhythm
of the window.

A brand new database is not empty: it is created with one top-level node named `mykeys`, ready to
take your first entries.

## 2. Why the root is not shown

The tree has a root node, but it is deliberately hidden. The root is what the whole database hangs
off: there can only ever be one of it, it cannot be renamed away, moved or deleted, and showing it
would only make every node you created look like it sits one level too deep.

With the root hidden, the nodes you created are the top level, which is what they are meant to be.
Two consequences follow from this and they are enforced by the application:

- The first node of the top level cannot be moved up. A node moved past that position would end up
  next to the root, which would be a second root.
- Since there is no root row to right-click, adding a node to the top level works by right-clicking
  the empty area of the tree, next to or below the nodes (see below).

The handles for opening and closing subtrees are drawn on the top-level rows themselves, so you can
still expand everything without a visible root row.

## 3. Working with nodes

All node operations live in the tree's right-click menu.

### Add a node

- **On the top level**: right-click the empty area of the tree, beside or below the existing nodes.
  A one-item menu appears with `add node...`.
- **Under an existing node**: right-click that node and choose `add node...`. This item is only
  offered on nodes that can hold children, so a node marked as a leaf does not have it.

Either way the **New node** dialog opens. It has a name field ("Enter name for node:") and an
"Is leaf:" checkbox. Confirm with OK, and the new node appears under its parent.

### Rename a node

Right-click the node, choose `Edit node...`. The **Edit node** dialog shows the same two fields as
the new-node dialog, prefilled with the node's current values. Change the name and confirm with OK.

### Duplicate a node

Right-click the node, choose `Duplicate node...`. The **Name for duplicate** dialog opens with the
name of the original plus the suffix `-Copy` already filled in. Confirm and the copy is placed next
to the original, under the same parent.

The copy is a real deep copy: the whole subtree below the node, and every entry inside it, is
copied along with its attachments and its custom properties. Original and copy share nothing, so
renaming or editing one never touches the other.

### Delete a node

Right-click the node, choose `delete`, and confirm the **Confirm deletion** dialog ("Are you sure",
"The delete action is not recoverable"). The node and everything under it, child nodes and entries
alike, go with it. Cancelling the confirmation leaves the node untouched.

### Expand and collapse

`Expand node` and `Collapse node` in the same menu open or close the entire subtree under the
selected node, not just its direct children.

## 4. Moving nodes

### Up and down among the siblings

`Move up` and `Move down` swap the node with the sibling above or below it. The moved node stays
selected afterwards, so you can move it several positions in a row without picking it again.

Moving is only offered where it would actually change something, so the items are shown greyed out
in the cases where they cannot do anything:

- The first node of a level cannot go up (see section 2 for why this matters at the top level).
- The last node of a level cannot go down.

One move is refused even though it looks possible: if two nodes on the same level carry the same
name, the reordering would collapse them into one and lose a node. Instead of silently dropping it,
the application shows **Move not possible** with the message "Two nodes of the same name are on this
level. Rename one of them first."

### Into another node

`Move to node...` opens the **Move node** dialog: a "Move under:" label and a dropdown of every
node the selected one could be moved under. Each candidate is shown with its full path, so two
nodes of the same name can be told apart. Pick one, confirm with OK, and the node moves with its
whole subtree.

The dropdown only offers valid new parents. Left out are:

- **Leaves**, because they cannot hold children.
- **The node itself**.
- **Its current parent**, because moving it there would change nothing.
- **Everything inside its own subtree**, because moving a node under its own child would cut that
  branch out of the tree.

The hidden root is a valid target for a node that does not already hang under it. That is how a
node deeper in the tree is promoted back to the top level.

If nothing is left after that filtering, the dialog does not open at all and you get **Move not
possible** with "There is no other node this one could be moved under." A target that is a leaf or
lies inside the moved subtree is refused with "A node cannot be moved into a leaf or into one of
its own children."

## 5. Working with entries

Select a node first: entry operations always act on the node that is currently selected in the
tree. With nothing selected, a new entry lands on the hidden root instead, where no node in the
tree will show it, so make the selection before you add anything. All entry operations live in the
right-click menu of the entries table.

The table has three columns, `Title`, `Username` and `Url`. They are display only, you cannot type
into a cell.

- **`add...`** opens the **New Crypt Entry** dialog (see section 6) and adds the finished entry to
  the selected node.
- **`edit...`** opens the same form as **Edit Crypt Entry** for the selected entry. A double left
  click on a row does exactly the same thing.
- **`duplicate...`** asks for a new title in the **New title for duplicate** dialog, prefilled with
  the original title plus `-Copy`. The duplicate is a deep copy, with its own attachment and
  property lists.
- **`delete`** removes every selected row after the **Confirm deletion** dialog. This is the one
  entry action that works on more than one row at a time.
- **`select all`** and **`clear selection`** do what they say.

Some items are only active in certain situations, which is why they may look greyed out:

| Menu item | Active when |
|---|---|
| `Copy Username`, `Copy Password`, `edit...`, `duplicate...` | exactly one row is selected |
| `Open url`, `Autotype` | exactly one row is selected and its URL is a well-formed URL |
| `delete`, `clear selection` | at least one row is selected |
| `select all` | the table has at least one row |
| `add...` | always |

Note that a right-click by itself does not select a row. Left-click the row first, then right-click
it, otherwise the actions that need a selection stay disabled.

## 6. Everything an entry holds

The entry dialog has three tabs: **Main**, **Attachments** and **Properties**.

### Main

- **Entry name** - the title, and the first column of the table.
- **Username** - the second column of the table.
- **Password** and **Repeat** - two masked fields. The `***` button next to them toggles between
  masked and plain text. The `Generate` button opens the password generator; accepting a generated
  password fills both the password and the repeat field.
- **Url** - the third column of the table, and the address used by `Open url` and `Autotype`.
- **Notes** - a free-text area.
- **Expires** - a checkbox. While it is unchecked the date picker below it is disabled; check it and
  pick the expiry date.

### Attachments

A table of files stored inside the entry, with the columns `Name`, `Path` and `Checksum`, and three
buttons:

- `Add File` opens a file chooser and takes the chosen file into the entry.
- `Remove` drops the selected row.
- `Save to` writes the selected attachment's content back out to a file you choose.

Only one attachment can be selected at a time, and `Remove` and `Save to` stay disabled until a row
is selected.

### Properties

Free key/value pairs of your own, shown in a table with the columns `Key` and `Value`:

- `Add Property` opens the **New Property Value** dialog with a `Key` and a `Value` field.
- `Edit` opens the same form as **Edit Property Value** for the selected pair.
- `Remove` drops the selected pair.

As with attachments, only one row can be selected at a time.

### Fields you never see in the dialog

An entry also carries a few values that no field edits. They exist so that an entry imported from a
KeePass database can be written back without losing information: the exact expiry timestamp
including time of day and offset (the dialog's expiry date only has day precision), the creation
time, the last access time, the last modification time, and the KeePass icon index. Custom
properties and attachments survive the same round trip, attachments as KeePass binary properties.
See [[import-export-keepass-database]] for the import and export flows themselves.

## 7. Searching

**File > Search...**, or `Ctrl+F`, opens the **Search** dialog. Its prompt says exactly what it
looks at: "Search for a node name or an entry's title, user name, URL or notes:".

- The term is matched case-insensitively, as a substring.
- A node matches if its own name contains the term, or if any of the entries inside it has the term
  in its **title**, **user name**, **URL** or **notes**.
- The first match found while walking the tree wins. The tree is fully expanded, then that node is
  selected and scrolled into view.

Two things worth knowing about the result: search selects the **node**, not the individual entry
row, so after a hit you look at the table on the right to find the entry itself. And the fields it
does not look at are the password, the custom properties and the attachment names.

If nothing matches, a **No match** dialog names the term you searched for. An empty term does
nothing at all, and searching before signing in tells you "Please sign in first."

## 8. Copying credentials

This is the daily use of a password manager, and it is two menu items on a selected entry row:

- `Copy Username` puts the entry's user name on the system clipboard.
- `Copy Password` puts the entry's password on the system clipboard.

Both need exactly one selected row. Both write to the real system clipboard, replacing whatever was
on it.

Two neighbouring items use the entry's URL and are only enabled when that URL is well formed:

- `Open url` opens the address in your standard browser.
- `Autotype` drives a browser to the address and fills in the credentials. It looks for input fields
  with the ids `UserName` and `Password` and then clicks the form's submit button, so it only works
  on login pages that happen to use exactly those ids.

## 9. View modes

The window can present itself in two ways, and the **View > View mode** submenu switches between
them:

- **Switch to panel mode** (`Alt+F7`) shows the tree and the entries table filling the window. This
  is the mode you land in right after signing in.
- **Switch to desktop mode** (`Alt+F6`) replaces that with a desktop pane, the multi-window surface
  the application starts in before you sign in.

The two modes change how you reach your database. In panel mode, **File > Open Database**
(`Ctrl+D`) simply brings the tree panel back to the front. In desktop mode, the same command puts
the tree and its table into a resizable internal window titled "Key database" inside the desktop
pane, so it can sit next to other windows.

## 10. Saving your changes

Every change described on this page - adding, renaming, duplicating, deleting or moving a node,
adding, editing, duplicating or deleting an entry - happens in memory first and marks the database
as modified. Use **File > Save** (`Ctrl+S`) to write it to disk. If you close the application with
unsaved changes, it asks before quitting.

## See also

- [[Getting-Started]]
- [[create-new-mystic-crypt-database]]
- [[open-existing-mystic-crypt-database]]
- [[import-export-keepass-database]]
- [[Secure-your-passwords-with-obfuscation]]
- [[install-a-plugin]]
