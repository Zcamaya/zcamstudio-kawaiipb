# JSON Schema References

Last updated: 2026-08-12

This folder contains markdown source files that document the JSON formats used by KawaiiPB.

## Sources

- [Template Manifest Schema](./template.schema.md)
- [Layout Manifest Format](./layout-manifest.md)

## Notes

- `template.schema.json` validates the template manifest stored in `app/src/main/assets/templates/template.schema.json`.
- Layout JSON files in `app/src/main/assets/layouts/` are app-defined manifests rather than formal JSON Schema files, so they are documented here as a structured format reference.
- The docs are intentionally source-like so app behavior and asset formats stay aligned with the codebase.
