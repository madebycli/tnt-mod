# TNT Editions — Fabric 26.2

Eine Minecraft-Java-Mod für **Minecraft 26.2**, **Fabric Loader 0.19.5+**, **Fabric API 0.161.0+26.2** und **Java 25**. Einzelspieler und Fabric-Server werden unterstützt; die Mod und Fabric API müssen auf Client und Server installiert sein.

**Fertige Mod herunterladen:** [fertig.jar](https://github.com/madebycli/tnt-mod/raw/refs/heads/main/fertig.jar). Falls der Direktdownload eine Anmeldung benötigt, die [Datei auf GitHub öffnen](https://github.com/madebycli/tnt-mod/blob/main/fertig.jar) und „Download raw“ wählen.

## Spielen

1. Fabric für **26.2** installieren.
2. Die heruntergeladene `fertig.jar` (oder die selbst gebaute `tnt-editions-1.0.0.jar` aus `build/libs/`) und die passende **Fabric API für 26.2** in den `mods`-Ordner legen. Die `-sources.jar` wird zum Spielen nicht benötigt.
3. Den **TNT-Konfigurator** herstellen oder im Kreativ-Reiter **TNT Editions** nehmen:

   ```text
   Eisen      Kompass   Eisen
   Redstone   Diamant   Redstone
              Eisen
   ```

   Mit Cheats: `/give @s tnt_editions:tnt_tuner`

4. Den Konfigurator in die Hand nehmen und rechtsklicken. **Sprengkraft 1–8×** und **Reichweite 1–4×** mit den Slidern einstellen. „Zufällige Farbe“ wählt eine andere der 16 Editionen; die Vorschau zeigt die Auswahl.
5. „1 Edition-TNT herstellen“ wandelt **genau ein normales TNT** aus dem Inventar in deine Edition um. Im Kreativmodus wird kein TNT verbraucht. Bei vollem Inventar wird die Ausgabe vor dir fallen gelassen.
6. Das Edition-TNT platzieren und mit Feuerzeug, Feuerkugel, Redstone oder einem brennenden Projektil zünden. Kettenreaktionen funktionieren ebenfalls. Werte und Farbe bleiben beim Platzieren, Abbauen, Speichern und Zünden erhalten. Der Konfigurator merkt sich die zuletzt hergestellte Einstellung.

**Normales TNT wird nicht verändert.** Bereits hergestellte Editionen behalten ihre Werte, auch wenn du danach die Slider änderst. Gleichfarbige Editionen mit verschiedenen Werten sind separate Item-Stacks; ihre Werte stehen im Tooltip.

## Was die Regler ändern

- **Sprengkraft:** reduziert den Explosionswiderstand zerstörbarer Blöcke und erhöht Schaden sowie Rückstoß. Bedrock, Barrieren und andere unzerstörbare Blöcke bleiben geschützt.
- **Reichweite:** multipliziert den Vanilla-Explosionswert von 4 auf bis zu 16. Der tatsächlich zerstörte Bereich hängt wie bei Vanilla von Material, Abschirmung und Zufall ab; die Anzeige ist ein Multiplikator, keine garantierte Blockanzahl.
- Die Vanilla-Gamerule `minecraft:tnt_explodes` bleibt wirksam. Mit hohen Werten entstehen große Explosionen; zum Ausprobieren eine Testwelt verwenden.

Menü und TNT verwenden ausschließlich Minecraft-GUI- und Block-Rendering, ohne direkte OpenGL-Aufrufe oder einen eigenen Grafik-Backend. Die Mod benötigt keine zusätzliche Vulkan-Bibliothek. Die Verfügbarkeit von Vulkan hängt vom Minecraft-Renderer, Betriebssystem und Treiber ab; die Mod erzwingt keinen Backend-Wechsel.

## Entwickeln

Auf **`main`** und im vorhandenen Checkout arbeiten. Keinen zusätzlichen Branch oder Worktree anlegen.

Mit einem JDK **25** auf `JAVA_HOME`:

```sh
./gradlew build
./gradlew runClient
```

`build` führt JUnit-Tests und echte Minecraft-Server-GameTests aus. Einzelne Prüfungen:

```sh
./gradlew test
./gradlew runGameTest
```

Der zusätzliche Client-Test öffnet das Menü über die echte Netzwerkverbindung, bedient beide Slider, stellt eine Edition her und prüft den TNT-Verbrauch. Er braucht eine grafische Sitzung:

```sh
./gradlew runClientGameTest
```

Ein Screenshot des Menüs wird dabei unter `build/run/clientGameTest/screenshots/` gespeichert.

Für Tests startet eine isolierte GameTest-Welt unter `build/run/gameTest`; es wird kein öffentlich erreichbarer Spielserver benötigt. Testergebnisse stehen in `build/test-results/`, der GameTest-Log in `build/run/gameTest/logs/latest.log`.

Die Original-Pixeltexturen und JSON-Ressourcen lassen sich mit `python3 tools/generate_assets.py` erneut erzeugen. Alle zum Spielen nötigen Ressourcen sind bereits eingecheckt; Python ist zum Bauen der Mod nicht erforderlich.
