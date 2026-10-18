package de.madebycli.tnt;

/** Server-side range attached to vanilla primed TNT; vanilla still renders and synchronizes it. */
public interface EditionRangeCarrier {
    int tntEditions$getRange();
    void tntEditions$setRange(int range);
}
