backports:
- added Global Expressions (reusable named expressions, updated on a configurable interval)
- added entity particle emitters (spawn particles from entities via entity_modifiers)
- added particle rate limiting (particles_throttle + auto_particle_rate_limit configs)
- expression improvements: numbers and functions now work directly as conditions (JS-like truthiness), string number literals are accepted, and a bad expression logs instead of crashing
- fixed an issue with colormaps
- added config to turn off the config button