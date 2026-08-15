# Layout Manifest Format

Last updated: 2026-08-15

KawaiiPB layout files define the printable geometry used by the photo assignment and final print flow.

These files live in:

- `app/src/main/assets/layouts/`

## Status

This is an app-defined layout manifest format, not a separate formal JSON Schema file. The layout JSON is parsed by the app and used to determine slot positions and output geometry.

## Common fields

### `version`

- Type: integer
- Purpose: format version marker for compatibility

### `layoutId`

- Type: string
- Purpose: stable identifier for the layout

### `layoutName`

- Type: string
- Purpose: user-facing layout title

### `layoutType`

- Type: string
- Purpose: app internal type used when selecting a layout

### `paper`

- Object describing the canvas dimensions and orientation
- Typical fields:
  - `width`
  - `height`
  - `unit`
  - `dpi`
  - `orientation`

### `output`

- Object describing the print output geometry
- Typical fields:
  - `doubleStrip`
  - `stripWidth`
  - `stripHeight`

### `backgroundImage`

- Type: string
- Purpose: relative path to the background image used by the layout

### `render`

- Object describing general render settings
- Typical fields:
  - `backgroundColor`
  - `photoFit`
  - `photoShape`
  - `cornerRadius`
  - `clipPhotos`

### `safeArea`

- Object describing margins around the printable area

### `brandingArea`

- Optional object describing an area reserved for branding output

### `slots`

- Array of photo slot definitions
- Typical fields per slot:
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

## Current app behavior

- Layout files are resolved from `app/src/main/assets/layouts/` and used by the flow stage.
- The selected layout defines the slot geometry used throughout assignment and preview.
- The print composer uses the same geometry to build final output.
