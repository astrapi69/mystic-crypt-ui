# How to create a plugin

mystic-crypt-ui plugins are built on [pf4j](https://github.com/pf4j/pf4j). This page covers
what a minimal plugin needs; for a complete, buildable starting point see
[`examples/hello-plugin`](https://github.com/astrapi69/mystic-crypt-ui/tree/develop/examples/hello-plugin)
in the repository - copy it as a template rather than starting from scratch.

# Overview

A plugin needs three things:

1. A main class extending `org.pf4j.Plugin`.
2. One or more classes annotated `@org.pf4j.Extension`, implementing
   `io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution`.
3. A `plugin.properties` descriptor and a `.zip` in the exact layout pf4j expects.

# The extension point

`PluginMenuContribution` is currently the one extension point the app exposes. It lets a plugin
contribute one or more `JMenuItem`s to the app's "Plugins" menu:

```java
package com.example.helloplugin;

import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.pf4j.Extension;
import io.github.astrapi69.mystic.crypt.plugin.api.PluginMenuContribution;

@Extension
public class HelloMenuContribution implements PluginMenuContribution
{
    @Override
    public List<JMenuItem> getMenuItems()
    {
        JMenuItem item = new JMenuItem("Hello from Plugin");
        item.addActionListener(e -> JOptionPane.showMessageDialog(null, "Hello!"));
        return List.of(item);
    }
}
```

A menu item's `ActionListener` can do anything a normal Swing action can. In particular, to open
a real panel the same way every built-in feature of the app does:

```java
MysticCryptApplicationFrame instance = MysticCryptApplicationFrame.getInstance();
if (!FrameMode.DESKTOP_PANE.equals(instance.getFrameMode()))
{
    instance.switchToDesktopPane();
}
JInternalFrame internalFrame = JComponentFactory.newInternalFrame("My Panel", true, true, true, true);
JInternalFrameExtensions.addInternalFrameToMainFrame(myPanelComponent, internalFrame, instance);
```

You don't need to bundle the app's own dependencies (crypt-api, crypt-data, swing-\*, ...) in
your plugin - pf4j's plugin-first classloading strategy falls back to the host application's
classpath for anything your plugin doesn't ship itself.

# The plugin class

```java
package com.example.helloplugin;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

public class HelloPlugin extends Plugin
{
    public HelloPlugin(PluginWrapper wrapper) { super(wrapper); }

    @Override
    public void start() { /* optional startup logic */ }

    @Override
    public void stop() { /* optional cleanup logic */ }
}
```

The `(PluginWrapper wrapper)` constructor is required - it's how pf4j instantiates your plugin
class.

# plugin.properties

Placed at the root of the plugin zip:

```properties
plugin.id=hello-plugin
plugin.class=com.example.helloplugin.HelloPlugin
plugin.version=1.0.0
plugin.provider=Example Author
plugin.description=Minimal example plugin
```

# Building the zip

pf4j expects a specific, non-jar layout: `plugin.properties` at the root, plus a `classes/`
directory holding your compiled `.class` files (this is also where the annotation processor
writes the extension index it needs to find your `@Extension` classes at runtime). A Gradle
`Zip` task assembling this is included in
[`examples/hello-plugin/build.gradle`](https://github.com/astrapi69/mystic-crypt-ui/blob/develop/examples/hello-plugin/build.gradle) -
copy that project and run:

```bash
./gradlew publishToMavenLocal      # from the main mystic-crypt-ui repo, once
cd examples/hello-plugin
../../gradlew pluginZip
```

The zip is produced at `examples/hello-plugin/build/plugin-dist/hello-plugin-1.0.0.zip`. See
[Install a plugin](install-a-plugin) for how to load it.

# Security

Plugins run with the full privileges of the host application - there is no sandboxing, and no
code signing in the current version. Document this clearly for your own plugin's users too.
