# Siricondos palette reference

Extracted from https://dev.siricondos.com/ (`:root` + key UI).

## CSS variables (site)

```css
:root {
  --paper: #f7f5ef;
  --ink: #252b27;
  --muted: #717369;
  --accent: #b29c78;
  --font-display: "Cormorant Garamond", Georgia, "Times New Roman", serif;
  --font-body: "DM Sans", Arial, sans-serif;
}
```

Body on site: `color: #1a1a1a; background-color: #faf8f4; font-family: DM Sans, …`

## Supporting greens & warms

| Hex                               | Role on site                                          |
|-----------------------------------|-------------------------------------------------------|
| `#293a2e`                         | AI FAB / forest CTA                                   |
| `#41523e`                         | Forest hover                                          |
| `#202a23`                         | Footer dark                                           |
| `#27342c`                         | Hero search submit                                    |
| `#3d4c3f`                         | Submit hover                                          |
| `#293f34`                         | Related forest                                        |
| `#667b62`                         | Soft green text                                       |
| `#65705e`                         | Neighbourhood link grey-green                         |
| `#74766d`                         | Italic title emphasis                                 |
| `#eceee4` / `#e7eee2` / `#f8f9f4` | Soft sage surfaces                                    |
| `#d5c5ab`                         | Sand / warm border                                    |
| `#faf3e4` / `#f8eedb`             | Warm paper variants                                   |
| `#e9e9e0`                         | Photo placeholder                                     |
| `#00A651`                         | M-Pesa green (payments only — avoid as primary brand) |

## Typography behaviours

- `.eyebrow`: 10px, tracking `0.2em`, uppercase, weight 500
- `.editorial-title`: Cormorant, tracking `-0.035em`, clamp ~38–62px
- `.brand-wordmark`: DM Sans, tracking `0.23em`, 16px, weight 500
- `.property-price`: Cormorant ~32px for large figures

## Suggested finance-app `:root`

```css
:root {
  --paper: #f7f5ef;
  --ink: #252b27;
  --muted: #717369;
  --accent: #b29c78;
  --forest: #293a2e;
  --forest-hover: #41523e;
  --forest-deep: #202a23;
  --surface: #ffffff;
  --sage: #eceee4;
  --sand: #d5c5ab;
  --income: #293f34;
  --expense: #8c4a3a;
  --font-display: "Cormorant Garamond", Georgia, serif;
  --font-body: "DM Sans", Arial, sans-serif;
  --font-mono: "DM Mono", ui-monospace, monospace;
}
```
