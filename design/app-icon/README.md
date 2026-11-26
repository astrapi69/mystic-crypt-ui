# The application icon

The key monogram: an M whose right stem carries a key bit, teal on a dark tile. Chosen by the
maintainer on 2026-11-26 from three drafts (#815); its own design, plain geometric shapes, no font,
no icon set, no template.

| file | what it is |
|---|---|
| `mystic-crypt-ui.svg` | the source of the 32, 48 and 256 px images, on a 256 unit grid |
| `mystic-crypt-ui-16.svg` | the 16 px image, drawn pixel by pixel on a 16 unit grid, since a scaled-down M blurs at that size |
| `preview.png` | every size on a light and a dark background, 16 and 32 px also enlarged |

The images the application loads are `src/main/resources/img/app-icon/mystic-crypt-ui-<size>.png`,
in 16, 32, 48 and 256 px, square and with an alpha channel. `ApplicationIcon` reads them and gives
them to every window.

## What changed against draft B

- The M's joins and ends are rounded (`stroke-linejoin="round"`, `stroke-linecap="round"`): Apple's
  guidance warns that sharp corners lose detail at small sizes.
- The key bit is two teeth of 28 units, 16 apart, so that at 32 px it is two steps of three and a
  half pixels with a gap of two.
- The tile has a light edge (`#6f86a8`) and a gradient, so that it stays visible on a dark
  background, where the plain dark tile of the draft disappeared.

## How the images are made

With Chromium through Playwright, at a device scale factor of 1 and a transparent background:

```js
const { chromium } = require('playwright');
const fs = require('fs');
(async () => {
  const big = fs.readFileSync('mystic-crypt-ui.svg', 'utf8');
  const small = fs.readFileSync('mystic-crypt-ui-16.svg', 'utf8');
  const browser = await chromium.launch();
  const page = await browser.newPage({ deviceScaleFactor: 1 });
  for (const size of [16, 32, 48, 256]) {
    const svg = (size === 16 ? small : big)
      .replace(/width="\d+" height="\d+"/, `width="${size}" height="${size}"`);
    await page.setViewportSize({ width: size, height: size });
    await page.setContent(`<html><body style="margin:0;background:transparent">${svg}</body></html>`);
    await page.locator('svg').screenshot({ path: `mystic-crypt-ui-${size}.png`, omitBackground: true });
  }
  await browser.close();
})();
```
