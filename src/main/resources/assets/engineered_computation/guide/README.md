# Guide pages

Each page of the guides (the four editions and the Palm Pilot) has its own text file here, one folder per language:
`<language>/<page>.txt`, such as `en_us/wrench.txt`. To translate the guides, copy `en_us` to your language's code
(`de_de`, `ja_jp`, ...) and translate the files. You can also ship the folder in a resource pack, under
`assets/engineered_computation/guide/<language>/`. Any page your language doesn't have is shown in English.

## A page's file

```
Assembly Wrench
Assembly Wrench
Drives are multiblocks: build one from casings...

The rest of the text, as it shows in the guide.
```

1. **Line 1** is the page's title.
2. **Line 2** is its line in the guide's contents. Leave it blank to use the title. Pages that aren't listed in the
   contents ignore it.
3. **Everything after that** is the page's text. Line breaks are kept as written, so a blank line starts a new
   paragraph. Don't wrap lines yourself: the guide wraps the text to fit, and moves on to more pages if it needs them.

Save the files as UTF-8.

## Placeholders

Some pages fill in names, written in braces. Keep the braces and the name inside them exactly as they are. You can
move them anywhere in the sentence:

| Page | Placeholders |
|---|---|
| `intro` | `{era}`: the era the guide covers |
| `era1` ... `era5` | `{memory}`: the era's memory. `{part}`: its part |
| `next` | `{next_guide}`: the next guide. `{next_era}`: the era it covers |

## Error codes

In `codes.txt`, each line after the first two is a code, a colon, then what it means: `404: Nothing matches the
search or filter.` The codes' names, such as "404 Not Found", come from the language file.

## What stays in the language file

The guides' buttons, the table of contents' heading, the build pages' captions and the "%s Memory" heading over each
era's cell recipes are still in `lang/<language>.json`, under `guide.engineered_computation.*`.
