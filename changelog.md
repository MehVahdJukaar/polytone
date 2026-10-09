- big release. many internal rewrite and refactors in rendering department
- added support for native colored/dynamic lights
- this also provides a voxel grid that can carry arbitrary per position flags to core/post shaders
- also includes occlusion data & light data
- added translucent_sun_and_moon to dimension modifiers
restored some legacy expressions compat
- fixed some issues with particle 3d models, like non emissive not rendering, emissive ones having wrong depth test & model particles having no interpolation in pos
- other minor fixes related to new light system like added small padidng on far out lights to prevent subtle edge bleeding into vanilla color
- light volume activation is not properly lazy, only active when needed