# Catalog of formats and cameras

Source of the `catalog.json` that the app reads from its assets. Edit by hand here; `catalog.json`
is regenerated with the tool in `tools/`.

```
formats.json          sensor formats, in mm, landscape orientation
cameras/<maker>.json  one file per maker, file name = slug(maker)
```

## Rules

- `id` = `slug(maker + " " + model)` at creation (`sony-alpha-7-iii`), then it **never changes**, even
  if the `model` is corrected: it is saved in users' preferences.
- `source`: `iso100` for hand-curated models, `lensfun` for imported ones. The Lensfun import leaves
  curated models alone, except for filling an empty `mount` and adding missing aliases.
- `aliases`: other names of the same camera, usually the EXIF name (`ILCE-7M3`).
- `specs` are optional and unverified (they come from the old 2020 database).
- After every change:

  ```sh
  go run ./tools/cmd/catalog fmt
  go run ./tools/cmd/catalog validate
  go run ./tools/cmd/catalog build -out android/app/src/main/assets/catalog.json
  ```

- To update from Lensfun (cloned next to this repo, never inside it):

  ```sh
  git clone --depth 1 https://github.com/lensfun/lensfun ../lensfun
  go run ./tools/cmd/catalog import-lensfun -lensfun ../lensfun/data/db
  ```

## License

Part of the data derives from the [Lensfun](https://lensfun.github.io) database, distributed under
[CC BY-SA 3.0](LICENSE-lensfun-CC-BY-SA-3.0.txt): the catalog has the same license and the app shows
the attribution. The code's license is separate.
