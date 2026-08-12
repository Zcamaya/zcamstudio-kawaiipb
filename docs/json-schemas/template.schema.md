# Template Manifest Schema

Last updated: 2026-08-12

This document describes the JSON Schema used for template manifests in KawaiiPB.

## Schema File

- [`app/src/main/assets/templates/template.schema.json`](../../app/src/main/assets/templates/template.schema.json)

## Purpose

Template manifests describe a reusable background plus strip-size-specific overlay images for a template folder.

The app reads these manifests when loading template options from:

- `app/src/main/assets/templates/`
- `Downloads/KawaiiPB/Templates/`

## Schema Summary

- Schema dialect: JSON Schema 2020-12
- Required properties: `name`, `background`, `overlays`
- Additional properties: not allowed

## Properties

### `version`

- Type: integer
- Minimum: `1`
- Default: `1`
- Purpose: optional format version marker for future compatibility.

### `name`

- Type: string
- Required: yes
- Purpose: human-readable name shown in the UI.

### `description`

- Type: string
- Required: no
- Purpose: longer helper text for admins or tooling.

### `background`

- Type: string
- Required: yes
- Purpose: relative path to the shared background image inside the template folder.

### `overlays`

- Type: object
- Required: yes
- Purpose: mapping of strip-size keys to overlay image file names.
- Values: non-empty strings

## Overlay Keys

The app currently recognizes overlay keys such as:

- `2x4`
- `2x3`
- `2x2`
- `2x1-stack`
- `3x1-left`
- `3x1-right`
- `2x6-horizontal`
- `4x1-banner`

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

## Current App Behavior

- The template tab lists template folders discovered in the template asset directory and in public Downloads.
- If a manifest is present, the app uses it to resolve display name, background, and overlay paths.
- If a manifest is missing fields, the app falls back to filename-based lookup when possible.
