/**
 * This function is called by Forge before any minecraft classes are loaded to
 * setup the coremod.
 *
 * @return {object} All the transformers of this coremod.
 */
function initializeCoreMod() {
    print("Initializing Polytone Transformers");
    return {
        "coremod": {
            "target": {
                "type": "METHOD",
                "class": "net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions",
                "methodName": "of",
                "methodDesc": "(Lnet/minecraftforge/fluids/FluidType;)Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;"
            },
            "transformer": function(methodNode) {
                var Opcodes = Java.type('org.objectweb.asm.Opcodes');
                var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
                var JumpInsnNode = Java.type('org.objectweb.asm.tree.JumpInsnNode');
                var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
                var LabelNode = Java.type('org.objectweb.asm.tree.LabelNode');
                var InsnList = Java.type('org.objectweb.asm.tree.InsnList');

                var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');

                var toInject = new InsnList();
                // this must be the first variable of the method which is what I want
                toInject.add(new VarInsnNode(Opcodes.ALOAD, 0));
                // Method call
                toInject.add(ASMAPI.buildMethodCall(
                    "net/mehvahdjukaar/polytone/fluid/forge/FluidPropertiesManagerImpl",
                    "maybeGetWrappedExtension",
                    "(Lnet/minecraftforge/fluids/FluidType;)Lnet/minecraftforge/client/extensions/common/IClientFluidTypeExtensions;",
                    ASMAPI.MethodType.STATIC));

                // Duplicate the result on the stack
                toInject.add(new InsnNode(Opcodes.DUP));
                // Check if the return value is null
                var nullLabel = new LabelNode();
                // If the value is not null, return it
                toInject.add(new JumpInsnNode(Opcodes.IFNULL, nullLabel));
                // load the return value onto the stack
                toInject.add(new InsnNode(Opcodes.ARETURN));
                // Label for the case when the value is null
                toInject.add(nullLabel);
                // Clear stack
                toInject.add(new InsnNode(Opcodes.POP));
                // Inject instructions at the beginning of the method
                methodNode.instructions.insertBefore(methodNode.instructions.getFirst(), toInject);

                return methodNode;
            }
        },
    };



}

/**
 * Utility function to wrap all transformers in transformers that have logging
 *
 * @param {object} transformersObj All the transformers of this coremod.
 * @return {object} The transformersObj with all transformers wrapped.
 */
function wrapWithLogging(transformersObj) {

    var oldPrint = print;
    // Global variable because makeLoggingTransformerFunction is a separate function (thanks to scoping issues)
    currentPrintTransformer = null;
    print = function(msg) {
        if (currentPrintTransformer)
            msg = "[" + currentPrintTransformer + "]: " + msg;
        oldPrint(msg);
    };

    for (var transformerObjName in transformersObj) {
        var transformerObj = transformersObj[transformerObjName];

        var transformer = transformerObj["transformer"];
        if (!transformer)
            continue;

        transformerObj["transformer"] = makeLoggingTransformerFunction(transformerObjName, transformer);
    }
    return transformersObj;
}

/**
 * Utility function for making the wrapper transformer function with logging
 * Not part of {@link #wrapWithLogging) because of scoping issues (Nashhorn
 * doesn't support "let" which would fix the issues)
 *
 * @param {string} transformerObjName The name of the transformer
 * @param {transformer} transformer The transformer function
 * @return {function} A transformer that wraps the old transformer
 */
function makeLoggingTransformerFunction(transformerObjName, transformer) {
    return function(obj) {
        currentPrintTransformer = transformerObjName;
        print("Starting Transform.");
        obj = transformer(obj);
        currentPrintTransformer = null;
        return obj;
    }
}