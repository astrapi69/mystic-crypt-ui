# The plugin icons

One icon for each internal plugin, in the style of the application icon (`design/app-icon/`): the
same dark tile with its light edge, the same teal (`#35d0bd`), the same 16 unit grid with the glyph
on columns and rows 3 to 12. Drawn pixel by pixel for 16 px; the 32 px image is the same drawing at
twice the size. Approved by the maintainer on 2026-11-28 as a whole (#1192); their own design, plain
rectangles, no font, no icon set, no template.

| file | motif |
|---|---|
| `certificate-plugin.svg` | seal with ribbons |
| `checksum-plugin.svg` | check mark |
| `console-plugin.svg` | prompt and cursor |
| `conversion-plugin.svg` | two arrows, one each way |
| `file-crypt-plugin.svg` | the crypt door of the application icon's draft A, half open |
| `kem-demo-plugin.svg` | envelope, the capsule a key travels in |
| `keygen-plugin.svg` | key with a spark |
| `keystore-plugin.svg` | cabinet with two drawers |
| `lethenon-plugin.svg` | two chain links |
| `menu-designer-plugin.svg` | menu entries of different lengths |
| `obfuscation-plugin.svg` | asterisk, a character made unreadable |
| `password-hash-plugin.svg` | hash sign |
| `pqc-signature-plugin.svg` | pen nib |
| `release-key-plugin.svg` | tag with a hole |
| `secret-sharing-plugin.svg` | three shares that belong together |
| `wifi-qr-plugin.svg` | radio waves of a wireless network (#1120; drawn after the approved sheet, in its style) |
| `sheet.png` | the sheet the maintainer approved: every icon at 16 and 32 px, enlarged, on light and dark |

Each plugin carries its two images in its own jar, next to its menu contribution class:
`plugins/<plugin>/src/main/resources/<the contribution's package>/icon-16.png` and `icon-32.png`,
square and with an alpha channel. The contribution sets them on every menu item it returns, as one
`BaseMultiResolutionImage`, so a scaled screen draws the 32 px image
(`docs/wiki/how-to-create-a-plugin.md`, "The plugin's icon").

Two tests keep it that way for every directory under `plugins/`:
- `EveryPluginCarriesItsIconTest`, in the everyday build: both images, square and with a
  transparent corner; this SVG; and the contribution reading both images next to itself.
- `EveryPluginShowsAnIconOfItsOwnUiTest`, end to end: every clickable item of every plugin submenu
  carries the 16 px icon with its 32 px variant, read from the built zip, and no two plugins show
  the same picture.

A plugin with no menu and only an action on an entry (the Wi-Fi QR plugin, #1120) carries the two
images next to its `EntryActionContribution` instead, returns the icon from `getIcon()`, and the host
sets it on the action's item in the entry's context menu; `WifiQrPluginUiTest` checks it there.

## How the images are made

With Chromium through Playwright, at a device scale factor of 1 and a transparent background, from
this directory into `png/`:

```js
const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  fs.mkdirSync('png', { recursive: true });
  const names = fs.readdirSync('.').filter(f => f.endsWith('.svg')).map(f => f.slice(0, -4));
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  for (const name of names) {
    const svg = fs.readFileSync(`${name}.svg`, 'utf8');
    for (const size of [16, 32]) {
      const sized = svg.replace(/width="[^"]+" height="[^"]+"/, `width="${size}" height="${size}"`);
      await page.setViewportSize({ width: size, height: size });
      await page.setContent(`<html><body style="margin:0;background:transparent">${sized}</body></html>`);
      await page.locator('svg').screenshot({ path: `png/${name}-${size}.png`, omitBackground: true });
    }
  }
  await browser.close();
})();
```

`png/<plugin>-16.png` becomes the plugin's `icon-16.png`, `png/<plugin>-32.png` its `icon-32.png`.

## Two motifs rejected while drawing

- A disc cut into three shares, for secret-sharing: at 16 px it read as the radiation trefoil.
- A struck-through eye, for obfuscation: at 16 px the stroke and the eye merged into noise.

Draft C's crystal from the application icon's drafts is used nowhere, because it is too close to
Obsidian's logo (#815).
