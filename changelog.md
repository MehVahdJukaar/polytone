fixed model location resolving not being right on fabric
rewrote CIM backing data structure to be much more efficent when adding a bunch of models using a search tree
fixed related issue of CIM not working well when adding multiple components
lowered priority of a mixin to try to dodge a bug from older sodium (seriously tho, just update it, luckily it's been fixed)
 
KNOWN ISSUES:
- CIM models might need a second reload to work
- CIM models defined in item modifiers instead of their own folder will not get loaded, on fabric