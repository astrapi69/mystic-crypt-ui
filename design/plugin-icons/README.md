# The plugin icons

One icon for each internal plugin, in the style of the application icon (`design/app-icon/`): the
same dark tile with its light edge, the same teal (`#35d0bd`), a glyph drawn pixel by pixel on the
16 px grid, on columns and rows 3 to 12. The sheet was put to the maintainer on 2026-11-26 and the
icons were built in on his instruction of 2026-11-28 (#1192). The Wi-Fi QR plugin's icon
was drawn the same way when the plugin came (#1120).

| file | what it is |
|---|---|
| `<plugin>.svg` | the source of the plugin's 16 and 32 px images, on a 16 unit grid |
| `sheet.png` | every icon at 16 and 32 px on a light and a dark menu, next to the plugin's first menu item |

| plugin | motif |
|---|---|
| certificate | a seal with ribbons |
| checksum | a check mark |
| console | a prompt and a cursor |
| conversion | two arrows, one each way |
| file-crypt | the crypt door of draft A, half open |
| kem-demo | an envelope, the capsule a key travels in |
| keygen | a key with a spark |
| keystore | a cabinet with two drawers |
| lethenon | two chain links |
| menu-designer | menu entries of different lengths |
| obfuscation | an asterisk, a character made unreadable |
| password-hash | a hash sign |
| pqc-signature | a pen |
| release-key | a tag with a hole |
| secret-sharing | three shares that belong together |
| wifi-qr | radio waves of a wireless network |

The images the application loads are `plugins/<plugin>/src/main/resources/icon/<plugin>-16.png`
and `-32.png`, square and with an alpha channel. `PluginIcon` reads them through the plugin's class
loader and sets them, as one `BaseMultiResolutionImage`, on the plugin's menu items.
`EveryPluginCarriesItsIconTest` requires both images, this SVG and the call for every directory
under `plugins/`; a plugin with no menu and only an action on an entry gets its icon from the host,
which sets it on that action's item by the plugin's id (`EntryActions`).

## How the images are made

With Chromium through Playwright, at a device scale factor of 1 and a transparent background, the
same way as the application icon. Run from the repository root:

```js
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  for (const file of fs.readdirSync('design/plugin-icons').filter(f => f.endsWith('.svg'))) {
    const plugin = file.replace(/\.svg$/, '');
    const source = fs.readFileSync(path.join('design/plugin-icons', file), 'utf8');
    const dir = path.join('plugins', plugin, 'src/main/resources/icon');
    fs.mkdirSync(dir, { recursive: true });
    for (const size of [16, 32]) {
      const svg = source.replace(/width="\d+" height="\d+"/, `width="${size}" height="${size}"`);
      await page.setViewportSize({ width: size, height: size });
      await page.setContent(`<html><body style="margin:0;background:transparent">${svg}</body></html>`);
      await page.locator('svg').screenshot({ path: path.join(dir, `${plugin}-${size}.png`), omitBackground: true });
    }
  }
  await browser.close();
})();
```

## Two motifs rejected while drawing

- a disc cut into three shares for secret-sharing: at 16 px it read as the radiation trefoil;
- a struck-through eye for obfuscation: at 16 px the stroke and the eye merged into noise.

Draft C's crystal is used nowhere, because it is too close to Obsidian's logo (#815).
