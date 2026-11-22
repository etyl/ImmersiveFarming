# Description

Immersive Farming is an addon for Immersive Engineering focusing on extending the farming experience. Players will be able to craft sprinklers or a watering can, to better irrigate their crops and make pesticides to prevent their crops from getting sick and dying. It also adds 2 multiblocks, the composting bin and industrial composter to make compost and speed up crop growth, and a feeding trough to raise baby animals. Immersive Farming also introduces several new mechanics that change vanilla farming and animal husbandry.

# Features

Features are explained in the engineer's manual, but here is an overview:

* Irrigation
  * Soil has a water level: crops don't grow on dry soil, and each growth stage consumes some water
  * Rain and sprinklers water the soil, and unwatered soil slowly dries out
  * Sprinklers! Sprinklers can be crafted in order to irrigate crops (6 block radius for the regular and 12 blocks for the high pressure one)
  * Watering can: right click soil or a plant to fully water it, right click a water source to refill it
  * Soil next to a water block is flooded, and crops on it grow much slower
* Crop growth
  * Bone meal can no longer be used on crops
  * Crops only grow at vanilla speed on watered and fertilized soil
  * Unfertilized and flooded soil both reduce growth speed to 20% (configurable)
  * Crops don't grow in cold biomes
  * Crops need sunlight to grow: they must see the sky, glass is fine (configurable)
* Carts
  * Plow: to till land faster
  * Sowing machine: to sow seeds and fertilize faster
* Fertilization
  * Compost can be made from the Industrial Composter multiblock or the composting bin and boosts crop growth
  * Soil becomes rich after 8 crops have grown to maturity on it, and stays rich: mature crops on rich soil give 30% more loot.
* Farmland
  * Farmland can no longer be trampled by jumping on it
  * Tilling dirt now requires several clicks (but better hoes speed up this process)
  * Farmland can be turned back into dirt with a shovel
  * Trowel: right click soil or a crop to inspect its tilling, water level, fertility and health
* Crop disease
  * Crops can become sick and eventually die
  * Sick crops can contaminate adjacent crops (disabled by default, configurable)
  * Bordeaux mixture (copper dust, gunpowder and a water bottle) cures a crop by hand and protects it until it is fully grown, 8 uses per bottle
  * Using pesticide in the sprinklers will prevent and cure diseases automatically
  * Pesticide is made in the Refinery from plant oil and water
* Raising animals
  * Baby farm animals (cows, mooshrooms, sheep, pigs, chickens, goats and rabbits) need 4 to 8 meals to grow up (configurable)
  * After eating, a baby grows for a while, then stops growing until it eats again
  * Adults must eat to produce (configurable): cows, goats and mooshrooms before each milking (at most every 5 minutes), chickens before each egg, and sheared sheep before their wool grows back
  * Animals eat their breeding food: feed them by hand when they are hungry, or fill a feeding trough
  * Feeding trough: hungry animals within 16 blocks walk to it and eat. It holds 64 items of a single kind of food. Its content takes the color of the food inside
  * The trough is built as a piece of fence: place it in the fence of a pen, fences connect to its ends and animals can't jump over it


# Mod compatibility

* **Immersive Engineering** (required)
  * Pesticide is made in the Refinery, sprinklers and the Industrial Composter connect to fluid pipes, and the composter takes items from conveyors
  * All features are explained in the engineer's manual
* **JEI**: recipes for the Industrial Composter and the composting bin, and information pages for pesticide and the feeding trough
* **Jade**: looking at an animal shows whether it is hungry or fed, how many meals a baby still needs, and when it can be milked again
* **Farmer's Delight**: tilling rich soil gives rich soil, rich soil farmland becomes rich soil, and breaking rich soil gives back Farmer's Delight rich soil. The sowing machine plants tomato and cabbage seeds
* **Crops from other mods**: any plant growing on soil follows the irrigation, fertilization and sunlight rules, and any tool able to till (modded hoes included) makes soil
  * Other mods' farmland can be converted to soil (convertible_farmland config, block ids or tags such as #forge:farmland)
  * The sowing machine plants any item in the #forge:seeds tag, items placing a crop, and the #immersivefarming:sower_seeds tag (Immersive Engineering hemp seeds included)
* **Animals from other mods**
  * Variants of the farm animals added by other mods (a modded cow...) need to be fed too, with their own breeding food (configurable). Modded chickens, cows, goats and sheep need food to produce if they use the vanilla egg laying, milking and wool
  * Other animals can be added with the #immersivefarming:needs_food_to_grow entity tag, or excluded with #immersivefarming:grows_without_food
  * The feeding trough accepts the breeding food of all these animals, plus the #immersivefarming:animal_feed item tag
  * Animals using the newer brain AI (such as goats) go to the trough too
* **Carts**: by default, the plow and sowing machine can be pulled by any animal that wears a saddle and isn't steered with an item (horses, donkeys, mules, camels and their modded equivalents), the list can be changed with the pull_animals config
* **Automation**: the feeding trough can be filled by hoppers, conveyors and item pipes from either side (but not emptied, so a hopper below won't drain it), and the carts' inventories are accessible to item handlers


# More information

This mod is still in its early stage of development so feel free to give any suggestions and please report bugs in the issue tracker !


## Modpack information

No permissions are needed to use the mod, feel free to use it in your modpack !


## Credits

Thanks to Peter for creating the assets !

In order to make the carts, I used chunks of code from AstikorCarts, and adapted it to Immersive Farming.