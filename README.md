# EMIDiscovery

![EMIDiscovery Logo](https://github.com/murphy-slaw/emi-discovery/blob/1.20.1/fabric/src/main/resources/icon_large.png?raw=true)

Prevents EMI from giving away all the secrets in your modpack by hiding items until the player has held one in their 
inventory.

## How it works
- Only items that the player has held appear in the index.
- The craftable/recipe view only shows recipes that can be made entirely from known items.
- Craftable recipes show their output item, but you can't do use or recipe lookups on them until they've been crafted.
- Tag and list ingredients are filtered to hide unknown items in the set.
- Known items can be prepopulated from a JSON file or unlocked via advancement.
- Known items are stored client side, separately for each player and world.
- Works with Reliable EMI (REMI) item groups.
- Blackout mode renders undiscovered items as black silhouettes in recipes instead of hiding the recipe entirely (sorta like BTA's guidebook).

## Configuration

EMI Discovery includes an in-game config screen (via Cloth Config) allowing you to customize:
- Showing craftable items in the index
- Workstation & catalyst requirements
- Recipe/usage lookup permissions for undiscovered items
- Blackout mode/obscured tooltips (`???`)
- Advancement discovery progression

### Pre-populated Items
The prepopulated items file is located at `config/emi_discovery_pre_discovered.json`. The format is a JSON array of strings specifying items by registry ID. Example:
```json
[
    "minecraft:stone",
    "minecraft:iron_ingot"
]
```
The contents are copied to the known items list of any player who doesn't already have one at login.

### Advancement Discovery Rules
You can configure advancements to unlock items, tags, or entire mods by placing JSON files in `config/emi_discovery/advancements/`. Example:
```json
[
  {
    "advancements": [
      "minecraft:story/mine_stone"
    ],
    "items": [
      "minecraft:furnace",
      "minecraft:stone_pickaxe"
    ],
    "tags": [
      "#minecraft:stone_tool_materials"
    ]
  }
]
```

## Acknowledgements
- Inspired by, based on, and reuses code from [Discovered Enough Items](https://www.curseforge.com/minecraft/mc-mods/dei)
- Icon uses [Archaeologist by Jevgenijs Kudinovs](https://thenounproject.com/icon/archaeologist-7507904/) from [The 
  Noun Project](https://thenounproject.com)
