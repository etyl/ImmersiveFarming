##### Unreleased

* Raising animals
  * Baby farm animals (the #immersivefarming:needs_food_to_grow entity tag) need 4 to 8 meals to grow up: after eating, a baby grows for a while, then stops growing until it eats again. Adults are not affected
  * Babies eat their breeding food, by hand (only when hungry, instead of the vanilla growth boost) or from a Feeding Trough
  * Adults must eat to produce: cows, goats and mooshrooms before each milking or bowl of stew (which can then only happen every 5 minutes), chickens before each egg, sheared sheep before their wool grows back (grazing grass still works). Hand feeding a hungry adult also makes it breed, as in vanilla
  * Feeding Trough: a 2 block long wooden trough, crafted from planks and wooden slabs, hungry animals within 16 blocks walk to it and eat. It is built as a piece of fence: fences connect to its ends, and animals can't jump over it. Its content takes the color of the food inside. It holds 64 items (anything these animals breed with, plus the #immersivefarming:animal_feed item tag), filled by hand or by hoppers and conveyors
  * Jade shows whether an animal is hungry or fed, how many meals a baby still needs, and when an animal can be milked again
  * Works with animals from other mods: variants of these animals (whose entity class extends one of them) need to be fed too (the #immersivefarming:grows_without_food entity tag excludes animals)
  * Animal Breeder: a 4x3x2 steel multiblock to automatically breed animals. 
* Crop diseases
  * Bordeaux mixture: crafted from copper dust, gunpowder and a water bottle, right click a growing crop to cure it and protect it until it is fully grown (8 uses per bottle)
  * Treated water is renamed to Pesticide, and is now made in the Refinery from plant oil and water (2 mB + 18 mB), instead of copper and sulfur dust in the Mixer
  * The trowel shows whether a crop is protected
* Rich soil
  * Soil becomes rich after 8 crops grown to maturity on it (configurable), and stays rich
  * Mature crops on rich soil give 30% more loot (configurable)
  * Farmer's Delight compatibility: tilling rich soil gives rich soil, rich soil farmland becomes rich soil, and breaking rich soil gives back Farmer's Delight rich soil
* Compost can now be applied by right clicking a crop, to fertilize the soil below it
* Garden Cloche rebalance (configurable)
  * The cloche needs Industrial Fertilizer to work: it no longer runs on water alone, and stops once its current dose runs out
  * Industrial Fertilizer is the only cloche fertilizer (bone meal, slag, saltpeter and sulfur are removed), and no longer speeds up growth
  * Plants take 4 times as long to grow
  * Uses 4 times more energy (32 FE/t)
  * A dose (Industrial Fertilizer + 250 mB of water) lasts 5 minutes: a harvest uses about 0.5 Industrial Fertilizer
  * The cloche's engineer's manual entry moved to the Immersive Farming section, rewritten to match these rules and the config values
* On a server, the engineer's manual and JEI now show the server's config values instead of the player's
* Improved the Composter model (thanks to Peter!)
* New Sowing Machine model


##### Version 1.20.1-0.4.1

* Fixed the wrong particles when breaking or walking on the top part of sprinklers
* Soil can no longer be trampled
* Fixed a gap in the Composting Bin's hitbox when it is open
* Fixed compost interaction
* Only seeds and compost can be added into the sower


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
