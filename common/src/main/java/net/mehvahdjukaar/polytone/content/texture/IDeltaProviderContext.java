package net.mehvahdjukaar.polytone.content.texture;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public interface IDeltaProviderContext {

    IDeltaProvider polytone$getDeltaProvider();

    void polytone$setDeltaProvider(IDeltaProvider mode);

    int polytone$getTimeCycleDuration();

    void polytone$setTimeCycleDuration(int duration);



}
