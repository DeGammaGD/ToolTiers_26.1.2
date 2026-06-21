# ToolTiers Migration Status: Minecraft 26.1.2 (Fabric)

This repository is currently stable on the 1.21.1 Yarn mapping line.

## Current Blocker

- `minecraft` 26.1.2 exists in Fabric Meta.
- `fabric-loader` has stable 0.19.3.
- `fabric-api` and common ecosystem libs have 26.x-era releases.
- **`yarn` for `26.1.2` is not published yet** (`https://meta.fabricmc.net/v2/versions/yarn/26.1.2` currently returns `[]`).

Because this codebase is written against Yarn-named APIs, a direct compile migration to 26.1.2 cannot be completed until a matching Yarn mapping is available.

## Why This Blocks Code Migration

- This project imports Yarn-named Minecraft classes and methods throughout gameplay, tooltips, mixins, networking, and data loading paths.
- Switching to non-Yarn mappings would require a broad source remap/rewrite and violates the current minimal-risk API-only migration goal.

## Safe Next Step (When Yarn Is Published)

1. Set `minecraft_version=26.1.2` in `gradle.properties`.
2. Set `yarn_mappings=26.1.2+build.X` (published build number).
3. Set `loader_version=0.19.3` (or current stable for 26.1.2).
4. Update Fabric API / Cloth / Mod Menu to the 26.1.x compatible lines.
5. Run:
   - `gradlew.bat clean build --no-daemon`
   - `gradlew.bat runClient --no-daemon`
6. Fix only compile/runtime API breaks introduced by the version jump.

## Validation Checklist (Post-Unblock)

- Game launches.
- Mod appears in Mod Menu.
- Crafted tools and armor get tiers.
- Chest loot gets tiers.
- Mob drops get tiers.
- Tier name + tooltip text render.
- Attribute modifiers apply and display correctly.
- Reforge UI opens.
- Optional integrations remain disabled.

## Rebrand Prep

- Keep internal behavior unchanged for now.
- Add small TODO markers while touching files to prepare `Tierify -> ToolTiers` renaming in a dedicated future pass.
