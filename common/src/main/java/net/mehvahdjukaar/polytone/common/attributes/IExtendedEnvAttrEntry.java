package net.mehvahdjukaar.polytone.common.attributes;

import java.util.function.Supplier;

public interface IExtendedEnvAttrEntry<Value> {

    void polytone$setArgumentSupplier( Supplier<Value> supplier) ;

    Supplier<Value> polytone$getArgumentSupplier( );

    boolean polytone$shouldBlend();

    void polytone$setShouldBlend(boolean shouldBlend);
}
