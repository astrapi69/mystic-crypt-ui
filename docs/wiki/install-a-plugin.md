# Install a plugin

mystic-crypt-ui can be extended with plugins - `.zip` files that get picked up automatically on
startup. No installer, no restart-and-configure step beyond copying the file.

## Where plugins go

The plugins directory is created automatically the first time the app runs, nested under the
application's own configuration directory:

- Linux: `~/.config/mystic-crypt-ui/plugins`
- the exact location follows wherever your OS/user puts application config for
  `mystic-crypt-ui`

## Installing a plugin

1. Get the plugin's `.zip` file from its author (see
   [`examples/hello-plugin`](https://github.com/astrapi69/mystic-crypt-ui/tree/develop/examples/hello-plugin)
   in the repository for a working example you can build yourself).
2. Copy the `.zip` into the plugins directory above. You don't need to unzip it - the app does
   that itself.
3. (Re)start mystic-crypt-ui.

If the plugin loaded successfully, a **Plugins** menu appears in the menu bar with whatever
items the plugin contributed. If the menu doesn't appear, the plugin either failed to load or
didn't contribute any menu items - nothing else in the app is affected either way.

## A note on trust

Plugins run with the full privileges of the application process - there is no sandboxing.
Only install plugins from sources you trust.

## Writing your own plugin

See [How to create a plugin](how-to-create-a-plugin).
