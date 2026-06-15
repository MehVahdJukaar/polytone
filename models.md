# Extra Features & Tweaks

This section contains some extra misc customization features which expand on existing vanilla concepts.

As of now these include

- Custom HUD Heart Textures
- Custom Leash Texture
- BlockState Model offset
- Separate Transform Model on Fabric
- Translucency support for gui textures
- Conditional Pack Overlays

## Custom HUD Heart Textures

This is a feautre that allows you to customize health bar by adding empty heart texture that change depending on your current heart style.

To do so you can add a texture at the following path

`assets/minecraft/textures/gui/sprites/polytone/heart/container_" + name + "_full.png`
and
`assets/minecraft/textures/gui/sprites/polytone/heart/container_" + name + "_half.png`

name can be one of the following: `poisoned`, `withered`, `absorbing`, `frozen`

You can also add ones for blinking hearths or hardcoded ones such as `container_withered_full_blinking` or `container_withered_hardcode_half_blinking`


## Custom Leash Texture

You can now change the texture used by leash. Simply put any texture in `assets/minecraft/textures/entity/lead.png`. Texture can be any size but default one is 1x24 pixels.


## Improved BlockState and Block Models

Polytone allows to use `z` rotation in blockstate variants aswell as unrestricts their vaues to be any angle.
The same is true for Block Models cubes which also can have any angle instead of just multiple of 22.5 degrees. They can also use the `x` `y` `z` rotation format instead of the axis one allowing for arbitrary rotations.

Additionally, Polytone adds the ability to change the model offset in your `blockstate` model files. These only work for the `variants` type.

The name of the fields are `xoffset`, `yoffset` and `zoffset`.

Here's an example blockstate file:


```json
{
  "variants": {
    "": {
      "model": "minecraft:block/poppy",
      "x": 90,
      "z": 13.1,
      "xoffset": 0.2,
      "zoffset": -0.2
    }
  }
}
```

## Conditional Pack Overlays

**1.21.11+**

Polytone adds a new field `polytone_condition` to your pack overlays. This is similar to the neoforge `neoforge:conditions` field which you can already use to make your overlays not load under certain conditions.

The polytone field is special as it is shared across loaders and also supports intricate logic throuhg the use of expressions.
It suports 2 possible syntaxes, as a Json Object or as a Scripting Expression string entry.
Note that the `required_configs` and `require_mods` fields are exactly the same you can use in the json files conditions described on the main page. As a reminder that's also a way to selectively disable polytone files, catch here is that these overlay conditions let you disable much more than that.

Here are examples for both formats; all fields are optional:


```json
{
  "polytone_condition": {
    "override": true,
    "require_config": "my_namespace:my_config",
    "require_mods": {
      "quark",
      "supplementaries",
      {
        "mod": "amendments",
        "version": "1.21-1.0.0"
      },
      {
        "mod": "jei",
        "version": "[1.19-1.0.0, 1.20-2.0.0)"
      }
    }

  }
}
```

The `override` field determines if the result of the minecraft version ranges should be overwritten or not.

The `require_config` field references a [Polytone Config](https://github.com/MehVahdJukaar/polytone/wiki/Polytone-Configs) by ID. These are special configs that can be defined and used in the mod config screen.

Or with an expression (shown directly inside a pack overlay).

```json
{
  "overlays": {
    "entries": [
      {
        "directory": "my_overlay_folder",
        "polytone_condition": "dateYear() == 2026 && condition('my_pack:my_condition') && !modLoaded('randomium')"
      }
    ]
  }
}
```


## Separate Transforms Model on Fabric

With Polytone version 1.17.12 you can use Forge's 'separate_transforms' method in resource packs on Fabric and Quilt.

On Forge/Neoforge you are able to easily specify an in-hand model and a GUI model for your item/any item, this method does not exist on Fabric/Quilt. This is where Polytone comes into play, Polytone can read and use the model files for Forge and implement the custom models on Fabric/Quilt.

### What files do I need?

To start you will need 3 files in ``assets/yourmodid/models/item``:

### GUI Model

First you need the GUI file to specify what sprite to use. This file needs to have a different name than the original model file of the item you are trying to overwrite, you can just append ``_gui`` to the filename. In this case we are using Supplementaries' ``flute_gui.json``.

```json
{
  "parent": "item/generated",
  "textures": {
    "layer0": "supplementaries:item/flute"
  }
}
```
### In-Hand Model

The second file is the in-hand model. This file also needs a different name, just append ``_in_hand`` to the filename. This file is called ``flute_in_hand.json``.
```json
{
	"textures": {
		"1": "supplementaries:item/flute_model",
			    "particles": "supplementaries:item/flute",
			    "particle": "supplementaries:item/flute"
	},
	"elements": [
		{
			"from": [7, 0, 7],
			"to": [9, 14, 9],
			"rotation": {"angle": 0, "axis": "y", "origin": [8, 8.5, 8]},
			"faces": {
				"north": {"uv": [2, 2, 4, 16], "texture": "#1"},
				"east": {"uv": [0, 2, 2, 16], "texture": "#1"},
				"south": {"uv": [6, 2, 8, 16], "texture": "#1"},
				"west": {"uv": [4, 2, 6, 16], "texture": "#1"},
				"up": {"uv": [2, 0, 4, 2], "texture": "#1"},
				"down": {"uv": [4, 0, 6, 2], "texture": "#1"}
			}
		}
	],
	"gui_light": "front",
	"display": {
		"thirdperson_righthand": {
			"translation": [0, -1, 0]
		},
		"thirdperson_lefthand": {
			"translation": [0, -1, 0]
		},
		"firstperson_lefthand": {
			"rotation": [0, 90, 0]
		},
		"ground": {
			"rotation": [90, 0, 0]
		},
		"gui": {
			"rotation": [-67.5, 0, 45],
			"scale": [1.5, 1.5, 1.5]
		},
		"head": {
			"rotation": [90, 0, 90],
			"translation": [5.5, 0, 0],
			"scale": [1.6, 1.6, 1.6]
		},
		"fixed": {
			"translation": [0, 0, -1.5],
			"scale": [1.5, 1.5, 1.5]
		}
	}
}
```
### Separate Transforms Model

And lastly you will need the 'separate_transforms' model, this model has to be named the exact filename of the model you are trying to overwrite. This file is logically called ``flute.json`` then.

NOTE: on NeoForte the loader id is `neoforge:separate_transforms` instead to match whats there.

```json
{
  "loader": "forge:separate_transforms",
  "base": {
    "parent": "supplementaries:item/flute_in_hand"
  },
  "perspectives": {
    "gui": {
      "parent": "supplementaries:item/flute_gui"
    },
    "ground": {
      "parent": "supplementaries:item/flute_gui"
    },
    "fixed": {
      "parent": "supplementaries:item/flute_gui"
    }
  },
  "textures": {
    "layer0": "supplementaries:item/flute"
  },
  "gui_light": "front",
  "display": {
    "thirdperson_righthand": {
      "translation": [
        0,
        -1,
        0
      ],
      "scale": [
        1,
        1,
        1
      ]
    },
      "translation": [
        0,
        -1,
        0
      ],
      "scale": [
        1,
        1,
        1
      ]
    },
    "ground": {
      "rotation": [
        0,
        0,
        0
      ],
      "translation": [
        0,
        2,
        0
      ],
      "scale": [
        0.5,
        0.5,
        0.5
      ]
    },
    "head": {
      "rotation": [
        90,
        0,
        90
      ],
      "translation": [
        5.5,
        0,
        0
      ],
      "scale": [
        1.6,
        1.6,
        1.6
      ]
    },
    "firstperson_lefthand": {
      "rotation": [
        0,
        90,
        0
     ]
  }
}
```

### Options

You have multiple options in your 'separate_transforms' for which model is displayed when and how.
***

``` 
"loader": "forge:separate_transforms"
```
This line needs to be added to the top of each 'separate_transforms' file to have them working.
```json
"perspectives": {
  "gui": {
      "parent": "yourmodid:models/item_gui" // This has to point to your 'item_gui.json'
  },
  "ground": {
      "parent": "yourmodid:models/item_gui" // This can point to either of your models, you normally want to use 'item_gui.json'
  },
  "fixed":  {
      "parent": "yourmodid:models/item_gui" // This option decides which model to use in item frames, you also want to use 'item_gui.json'
  }
}
```

### Textures

You will need to specify the texture paths of the texture for the GUI model in your 'separate_transforms' model:

```json
"textures": {
    "layer0": "supplementaries:item/flute"
}
```

### Translations and Display

For your item model to display correctly, you will need to define translations and GUI lighting:

```json
"gui_light": "front", // defines the lighting in the GUI
"display": {} // here you can specify the translation of the 3D model, you can just paste the translations from your 'item_in_hand.json' model
```

