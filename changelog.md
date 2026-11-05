##### Version 1.20.1-0.4.1

* Fixed the wrong particles when breaking or walking on the top part of sprinklers
* Soil can no longer be trampled


##### Version 1.20.1-0.4

* Ported to Minecraft 1.20.1 (Forge 47, Immersive Engineering 10)
* Soil water levels
  * Crops don't grow on dry soil, and each growth stage consumes some water
  * Rain and sprinklers water the soil, and unwatered soil slowly dries out
  * Soil next to a water block is flooded (new texture), and crops on it grow slower (20% speed by default, configurable)
* Watering can
  * Right click soil or a plant to fully water it, right click a water source to refill it
  * Crafted with copper ingots and a bucket
* Trowel
  * Right click soil or a crop to inspect its tilling, water level, fertility and health
* Soil can be turned back into dirt with a shovel
* Unfertilized soil now grows crops much slower (20% speed by default, configurable)
* Crops need sunlight: they must see the sky to grow, glass is fine (configurable)
* Soil rules apply to all plants, including tall plants (pitcher plants, hemp, tomatoes)
* Removed crop rotation growth boost
* Sick crops no longer contaminate adjacent crops by default
* Treated water is now made in the Mixer from water, copper dust and sulfur dust (no more herbicide and Refinery)
* Fixed the Industrial Composter's fluid pipes not showing in the engineer's manual
* Composting Bin: a low-tech 2x2 composter
  * Fill it with wet and dry matter, turn it with a shovel when it opens, and collect compost
* Plow and sowing machine
  * Plow accepts any tool able to till, and finishes partially tilled soil
  * Sowing machine accepts all seeds (#forge:seeds, #immersivefarming:sower_seeds and modded crops)
  * Fixed seeds and compost being consumed twice
  * Respect claim and spawn protection
* Compatibility
  * Dirt tilled by any tool or machine (Create, Tinkers', Mekanism...) becomes soil
  * Farmland that a crop grows on is turned into soil
  * Bone meal is blocked on crops on soil, even when used by machines (Create: Slice & Dice...)
* Engineer's manual reflects the config
* Config file
  * Toggle farmland replacement, sunlight requirement and disease spreading
  * Choose which blocks are turned into soil
  * Change evaporation, flooded growth and unfertilized growth


##### Version 1.18.2-0.3

* Plowing machine
  * Plow dirt much quicker using a plow and horse
* Sowing machine
  * Plant seeds and fertilizer quicker using a sowing machine and horse
* Dead Crop (when crops die they become a Dead Crop block)
* Stems may die after giving a fruit (configurable)
* Config file
  * Change cart speed
  * Change probability of disease contamination
  * Change probability of stem death


##### Version 1.18.2-0.2.3

* Fixed default growth for non crops/stems


##### Version 1.18.2-0.2.2

* Fixed crash when crops have a different maxAge


##### Version 1.18.2-0.2.1

* Growth speed fix
* Crop rotation boosts growth speed
* Refactored soil blockstate values


##### Version 1.18.2-0.2

* Composter multiblock
  * Produces compost from organic matter
  * Compatible with most food mods
* Compost
  * Soil can be fertilized using compost, boosting crop growth speed
  * Fertilized soil has some small vegetation growing (green dots)
* Crops cannot grow in cold weather anymore (cold biomes)
* Changed the sprinkler models
* Engineer's manual integration
* Reduced crop disease chance

##### Version 1.18.2-0.1

* Irrigation
    * Sprinklers! Sprinklers can be crafted in order to irrigate crops (6 block radius for the regular and 12 blocks for the high pressure one)
    * Sprinklers need to be fed with water or treated water in order to function. The water can be inputed and output on the bottom part of the block using a hammer (to configure input or output) and pipes
    * Water tiles only irrigate the adjacent farmland tiles
* Crop disease
    * Crops can become sick and will contaminate adjacent crops and eventually die
    * Using treated water in the sprinklers will prevent and cure diseases
* Farmland
    * Farmland can no longer be trampled by jumping on it
    * Bone meal can no longer be used on crops
    * Tilling dirt now requires several clicks (but better hoes speed up this process)
