# ToolTiers

ToolTiers is a Minecraft 26.1.2 Fabric port and continuation of the **Tiered** concept, with roots in the community-driven **Tierify** project. This mod adds randomized quality tiers to tools, weapons, and armor with attribute-based progression and customization.

ToolTiers continues the development of tier-based item progression with modern Minecraft version support (26.1.2) and an improved data-driven architecture.

The original mod, **Tiered**, is inspired by [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools).

<img src="resources/legendary_chestplate.png" width="400">

## Features

### Currently Functional

- **Random Item Tiers**: Tools, armor, and weapons receive random quality tiers
- **Tier Names**: Display names for tiers (Common, Uncommon, Rare, Epic, Legendary, Mythic)
- **Attribute Modifiers**: Apply attributes like damage, speed, armor, crit chance, and more
- **Custom Tooltip Styling**: Tier-specific tooltip borders and colors
- **Weapon Support**:
  - Swords
  - Axes
  - Bows
  - Crossbows
  - Spears (Tridents)
  - Maces
  - Shields
  - Utility tools
- **Armor and Tools**: Full support for all vanilla armor and tool categories
- **Data-Driven Customization**: JSON-based item attributes and tier definitions

### In Development

- **Reforging System**: Under development, currently unavailable
- **Additional Item Categories**: Expanding support for more items
- **Enhanced Customization**: Additional configuration options

## 26.1.2 Port

- Updated from older Minecraft versions
- Migrated mappings and API changes (Mojmap)
- Removed outdated dependencies
- Stabilized Fabric runtime
- Modernized item registration and mixins

## Installation

ToolTiers requires:

- [Fabric Loader](https://fabricmc.net/)
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config)
- [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu) (optional)

## Customization

ToolTiers uses a **data-driven system** for all item tier definitions and attributes. This allows for easy customization and extension without code changes.

### System Architecture

- **Internal Namespace**: Uses mod id `tiered` internally for compatibility and data organization
- **Item Attributes**: Stored in `data/tiered/item_attributes/`
- **Tier Definitions**: JSON-based tier templates with attributes, styles, and weights
- **Custom Verifiers**: ToolTiers uses its own item verification system to match items against tier requirements

You can add, modify, and remove tier modifiers by editing the JSON files in `data/tiered/item_attributes/`.

Example modifier:

```json
{
  "id": "tiered:hasteful",
  "verifiers": [
    {
      "tag": "c:pickaxes"
    },
    {
      "tag": "c:shovels"
    },
    {
      "tag": "c:axes"
    }
  ],
  "weight": 10,
  "style": {
    "color": "GREEN"
  },
  "attributes": [
    {
      "type": "generic.dig_speed",
      "modifier": {
        "name": "tiered:hasteful",
        "operation": "ADD_MULTIPLIED_BASE",
        "amount": 0.10
      },
      "optional_equipment_slots": [
        "MAINHAND"
      ]
    }
  ]
}
```

### Attributes

ToolTiers provides and uses custom and vanilla-compatible attributes such as dig speed, crit chance, durability, armor, reach, attack range, movement speed, and more.

Types include: `generic.armor`, `generic.armor_toughness`, `generic.dig_speed`, `tiered:generic.durable`, `generic.max_health`, `generic.movement_speed`, `reach-entity-attributes:reach`, `generic.luck`, `generic.attack_damage`, `tiered:generic.crit_chance`, `reach-entity-attributes:attack_range`, `tiered:generic.range_attack_damage`

### Verifiers

ToolTiers uses a custom verifier system to determine whether items are eligible for specific tier attributes.

**Item ID Example:**

```json
"id": "minecraft:apple"
```

**Tag Example (Convention Tags):**

```json
"tag": "c:helmets"
```

**Fallback Matching:**

ToolTiers includes intelligent fallback matching for common item patterns (e.g., items ending with `_pickaxe`, `_sword`, `_spear`) to ensure comprehensive coverage.


### Weight

Weight determines how common a tier is. Higher weight means a higher chance to be applied.

### NBT

Custom NBT values can be added via `nbtValues`. Supported value types are string, boolean, integer, and double.

```json
"nbtValues": {
  "Damage": 100,
  "key": "value"
}
```

### Tooltip

Custom tooltip borders can be defined via resource packs.

- Border textures go in `assets/tiered/textures/gui`.
- Tooltip json files go in `assets/tiered/tooltips`.
- `background_gradient` can be configured.
- For color alpha format reference, see [this guide](https://gist.github.com/lopspower/03fb1cc0ac9f32ef38f4).
- See default tooltip data in `src/main/resources/assets/tiered/tooltips`.

Example:

```json
{
  "tooltips": [
    {
      "index": 0,
      "start_border_gradient": "FFBABABA",
      "end_border_gradient": "FF565656",
      "texture": "tiered_borders",
      "decider": [
        "set_the_id_here",
        "tiered:common_armor"
      ]
    }
  ]
}
```

### Reforge

**Status:** Under Development - Reforge functionality is currently unavailable and will be enabled in a future release.

Once complete, Reforge will allow players to reset and reassign item tiers.

## Status

**Version:** 1.2.0  
**Minecraft:** 26.1.2  
**Loader:** Fabric  
**Mappings:** Mojmap  
**State:** Work in Progress

### Current State

- ✅ Game launches and loads worlds correctly
- ✅ Core tier system is fully functional
- ✅ Tier assignment and attribute application working
- ✅ All supported items receive appropriate tiers
- ⚠️ Reforge system under development
- 🔄 Additional features and categories planned

## Credits

ToolTiers is built on the work of dedicated community developers:

- **Draylar1** - Original creator of [Tiered](https://github.com/Draylar/tiered), the foundational mod that inspired this project
- **Globox_Z** - Creator of [TieredZ](https://github.com/Globox1997/TieredZ), an early fork continuing Tiered development
- **Ameisin** - Creator of [Tierify_1.21.1](https://github.com/Ameisin/Tierify_1.21.1), a community port that maintained the mod during version transitions
- **nvb-uy** - Community maintainer of Tierify, bridging from TieredZ to modern versions

ToolTiers continues this legacy with **Minecraft 26.1.2 support** and focuses on maintaining a clean, data-driven architecture for future extensibility.

## License

ToolTiers source code in this repository is licensed under MIT.

Original Tiered and TieredZ creators retain authorship credit for their upstream work. ToolTiers continuation, migration, and maintenance contributions are by DeGammaGD.

Non-code assets may be All Rights Reserved where explicitly stated.
