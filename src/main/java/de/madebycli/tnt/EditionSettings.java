package de.madebycli.tnt;

import java.util.List;

/** Limits shared by sliders, server validation, item data and explosions. */
public record EditionSettings(int strength, int range, int color) {
    public static final int MAX_STRENGTH = 8;
    public static final int MAX_RANGE = 4;
    public record Color(String id, int rgb) {}
    public static final List<Color> COLORS = List.of(
            new Color("ruby", 0xEB4D63), new Color("amber", 0xF59B42),
            new Color("solar", 0xEEC84A), new Color("lime", 0xA6D74E),
            new Color("emerald", 0x36B778), new Color("mint", 0x66D9B1),
            new Color("turquoise", 0x35C5C9), new Color("ocean", 0x4099D9),
            new Color("cobalt", 0x6176E8), new Color("violet", 0x9C70E8),
            new Color("orchid", 0xCA6BE0), new Color("pink", 0xED77B0),
            new Color("copper", 0xBF8055), new Color("silver", 0xB4C5D3),
            new Color("obsidian", 0x636580), new Color("ivory", 0xE4DFC8));
    public static final EditionSettings DEFAULT = new EditionSettings(1, 1, 0);

    public EditionSettings {
        if (!isValid(strength, range, color)) {
            throw new IllegalArgumentException("Invalid TNT edition settings");
        }
    }

    public static boolean isValid(int strength, int range, int color) {
        return strength >= 1 && strength <= MAX_STRENGTH
                && range >= 1 && range <= MAX_RANGE
                && color >= 0 && color < COLORS.size();
    }

    public float explosionPower() { return 4.0F * range; }
    public float resistanceMultiplier() { return 1.0F / strength; }
    public int rgb() { return COLORS.get(color).rgb(); }
    public String colorId() { return COLORS.get(color).id(); }
}
