# Template Manifest Schema

Last updated: 2026-08-15

This document describes the JSON Schema used for template manifests in KawaiiPB.

## Schema file

- [app/src/main/assets/templates/template.schema.json](../../app/src/main/assets/templates/template.schema.json)

## Purpose

Template manifests provide the display name, shared background, and strip-specific overlay mapping used by the flow when a template is selected.

## Schema summary

- Schema dialect: JSON Schema 2020-12
- Required properties: `name`, `background`, `overlays`
- Additional properties: not allowed

## Properties

### `version`

- Type: integer
- Minimum: `1`
- Purpose: compatibility marker for future manifest revisions

### `name`

- Type: string
- Required: yes
- Purpose: human-readable template name shown in the app

### `description`

- Type: string
- Optional
- Purpose: optional human text for tooling or administrators

### `background`

- Type: string
- Required: yes
- Purpose: relative path to the template background image inside the folder

### `overlays`

- Type: object
- Required: yes
- Purpose: mapping of layout/strip-size keys to overlay image file names
- Values: non-empty strings

## Example

```json
{
  "version": 1,
  "name": "Base",
  "background": "background.png",
  "overlays": {
    "2x4": "overlay_2x4.png",
    "2x6-horizontal": "overlay_2x4.png"
  }
}
```

## Current app behavior

- Template folders are discovered from the app assets and from the public Downloads folder.
- If a manifest is present, the app uses it for display name and asset resolution.
- If a manifest is incomplete, the app may fall back to filename-based resolution when possible.

## Important note

The asset contract should remain in sync with the actual template folders and files in `app/src/main/assets/templates/`.
