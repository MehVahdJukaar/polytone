package net.mehvahdjukaar.polytone.forge;

import cpw.mods.modlauncher.api.ITransformer;
import net.minecraftforge.coremod.api.ASMAPI;
import net.minecraftforge.forgespi.coremod.ICoreModFile;
import net.minecraftforge.forgespi.coremod.ICoreModProvider;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class TestCoremod implements ICoreModProvider {

    @Override
    public void addCoreMod(ICoreModFile file) {

    }

    @Override
    public List<ITransformer<?>> getCoreModTransformers() {
        return null;
    }
}
