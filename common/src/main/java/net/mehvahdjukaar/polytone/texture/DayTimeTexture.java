package net.mehvahdjukaar.polytone.texture;

public interface DayTimeTexture {

    boolean polytone$usesDayTime();

    void polytone$setUsesDayTime(boolean usesWorldTime);

    int polytone$getDayDuration();

    void polytone$setDayDuration(int duration);
}
