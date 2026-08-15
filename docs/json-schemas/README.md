# JSON Schema References

Last updated: 2026-08-15

This folder documents the JSON contracts used by KawaiiPB for template and layout assets.

## Current sources

- [Template manifest schema](./template.schema.md)
- [Layout manifest format](./layout-manifest.md)

## Reality of the current project

- `app/src/main/assets/templates/template.schema.json` is the actual JSON Schema used by the template manifest contract.
- `app/src/main/assets/layouts/*.json` files are app-defined layout manifests, not a separate formal schema file.
- These docs are intended to stay aligned with the actual files in `app/src/main/assets/` so that UI behavior and asset contracts remain consistent.

## Important note

The layout and template definitions are part of the runtime kiosk flow, so if a layout or template asset changes, the matching documentation should be reviewed as part of the same change.
