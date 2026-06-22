# ToolTiers

ToolTiers is a Minecraft 26.1.2 Fabric port and continuation of the Tiered concept, focused on standalone tier-based item progression.



The original mod, Tiered, is inspired by [Quality Tools](https://www.curseforge.com/minecraft/mc-mods/quality-tools).

<img src="resources/legendary_chestplate.png" width="400">

## Features

- Tier generation for tools and armor
- Randomized item qualities
- Attribute modifiers
- Custom tooltip styling and borders
- Data-driven modifiers
- Reforging system (WiP)

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

ToolTiers is data-driven, which means you can add, modify, and remove modifiers as needed. The base path for modifiers is `data/modid/item_attributes`, and tier modifiers are stored under the Tiered mod id.

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

A verifier defines whether a given tag or item id is valid for a modifier.

Item id example:

```json
"id": "minecraft:apple"
```

Tag example:

```json
"tag": "c:helmets"
```


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



## Status

ToolTiers is currently under active development.

The 26.1.2 port is functional:

- Game launches
- World loading works
- Tier assignment works
- Core systems are being refined

## Credits

- Draylar1 - original Tiered mod
- Globox_Z - TieredZ fork
- nvb-uy - Tierify fork
- Ameisin - Tierify_1.21.1 port

## License

ToolTiers source code in this repository is licensed under MIT.

Original Tiered and TieredZ creators retain authorship credit for their upstream work. ToolTiers continuation, migration, and maintenance contributions are by DeGammaGD.

Non-code assets may be All Rights Reserved where explicitly stated.
