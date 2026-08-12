# Layout Manifest Format

Last updated: 2026-08-12

KawaiiPB layout files define the printable/previewable geometry used by the kiosk flow.

These files are JSON manifests stored in:

- `app/src/main/assets/layouts/`

## Status

This format is app-defined rather than a separate JSON Schema file. The app currently treats these JSON files as structured layout manifests.

## Common Fields

### `version`

- Type: integer
- Purpose: format version for future compatibility.

### `layoutId`

- Type: string
- Purpose: stable identifier for the layout.

### `layoutName`

- Type: string
- Purpose: user-facing layout title.

### `layoutType`

- Type: string
- Purpose: internal type key used by the app when selecting a layout.

### `paper`

- Object describing the rendered print canvas.
- Common fields:
  - `width`
  - `height`
  - `unit`
  - `dpi`
  - `orientation`

### `output`

- Object describing the output strip dimensions.
- Common fields:
  - `doubleStrip`
  - `stripWidth`
  - `stripHeight`

### `backgroundImage`

- Type: string
- Purpose: relative path to the background artwork used for previews and printing.

### `render`

- Object describing how photos are rendered into the layout.
- Common fields:
  - `backgroundColor`
  - `photoFit`
  - `photoShape`
  - `cornerRadius`
  - `clipPhotos`

### `safeArea`

- Object describing margins around the printable area.
- Common fields:
  - `left`
  - `top`
  - `right`
  - `bottom`

### `brandingArea`

- Object describing an optional branding region.
- Common fields:
  - `enabled`
  - `height`

### `slots`

- Array of photo slot definitions.
- Each slot commonly includes:
  - `id`
  - `strip`
  - `x`
  - `y`
  - `width`
  - `height`
  - `rotation`
  - `visible`

## Example

```json
{
  "version": 1,
  "layoutId": "pb_split_vert_4p_grid",
  "layoutName": "2x4 Strip",
  "layoutType": "pb_split_vert_4p_grid",
  "paper": {
    "width": 1200,
    "height": 1800,
    "unit": "px",
    "dpi": 300,
    "orientation": "portrait"
  },
  "output": {
    "doubleStrip": true,
    "stripWidth": 600,
    "stripHeight": 1800
  },
  "backgroundImage": "layouts/pb_split_vert_4p_grid.png"
}
```

## Current App Behavior

- The flow loads layouts from assets for the photo assignment and final preview stages.
- The layout defines the slot geometry used for photo placement and transformations.
- The print composer uses the same layout data to build the final output.
