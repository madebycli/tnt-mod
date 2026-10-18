package de.madebycli.tnt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditionSettingsTest {
    @Test void strengthAndRangeAreIndependent() {
        var normal = EditionSettings.DEFAULT;
        var stronger = new EditionSettings(100, 1, 0);
        var wider = new EditionSettings(1, 100, 0);
        assertEquals(normal.explosionPower(), stronger.explosionPower());
        assertEquals(0.01F, stronger.resistanceMultiplier());
        assertEquals(400.0F, wider.explosionPower());
        assertEquals(normal.resistanceMultiplier(), wider.resistanceMultiplier());
    }

    @Test void everySliderCombinationHasBoundedFinitePhysics() {
        for (int strength = 1; strength <= EditionSettings.MAX_STRENGTH; strength++) {
            for (int range = 1; range <= EditionSettings.MAX_RANGE; range++) {
                for (int color = 0; color < EditionSettings.COLORS.size(); color++) {
                    var settings = new EditionSettings(strength, range, color);
                    assertTrue(settings.explosionPower() >= 4 && settings.explosionPower() <= 400);
                    assertTrue(settings.resistanceMultiplier() >= 0.01F && settings.resistanceMultiplier() <= 1);
                    assertTrue(settings.rgb() >= 0 && settings.rgb() <= 0xFFFFFF);
                }
            }
        }
    }

    @Test void corruptOrForgedValuesAreRejected() {
        for (int bad : new int[]{Integer.MIN_VALUE, -1, 0, 101, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new EditionSettings(bad, 1, 0));
        }
        for (int bad : new int[]{Integer.MIN_VALUE, -1, 0, 101, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new EditionSettings(1, bad, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new EditionSettings(1, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new EditionSettings(1, 1, 16));
    }

    @Test void editionsHaveDistinctNamesAndColors() {
        assertEquals(16, EditionSettings.COLORS.stream().map(EditionSettings.Color::id).distinct().count());
        assertEquals(16, EditionSettings.COLORS.stream().map(EditionSettings.Color::rgb).distinct().count());
    }
}
