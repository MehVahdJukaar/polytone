/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {

    return {
        "Polytone#wrapFluidTint": {
            "target": {
                "type": "METHOD",
                "class": "net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions",
                "methodName": "of",
                "methodDesc": "(Lnet/minecraftforge/fluids/FluidType;)Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions"
            },
            "transformer": function(methodNode) {
                methodNode.instructions

                var toInject = new InsnList();

                // Make list of instructions to inject
                toInject.add(new VarInsnNode(Opcodes.ALOAD, 0)); // this must be the first variable of the method which is what i want
                toInject.add(new MethodInsnNode(
                    //int opcode
                    INVOKESTATIC,
                    //String owner
                    "net/mehvahdjukaar/polytone/fluid/forge/FluidPropertiesManagerImpl",
                    //String name
                    "maybeGetWrappedExtension",
                    //String descriptor
                    "(Lnet/minecraftforge/fluids/FluidType;)Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions",
                    //boolean isInterface
                    true
                ));
                // Check if the return value is null
                var notNullLabel = new LabelNode();
                toInject.add(new JumpInsnNode(Opcodes.IFNULL, notNullLabel));
                // If the value is not null, return it
                toInject.add(new InsnNode(Opcodes.ARETURN));

                // Label for the case when the value is null
                toInject.add(notNullLabel);

                // Inject instructions
                instructions.insert(instructions[0], toInject);

                return methodNode;
            }
        },
    };
}
