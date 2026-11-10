# AdvancedDefensiveWalls 2.0 (Paper 1.21.11, Java 21)

Estructuras defensivas modulares con durabilidad, mantenimiento por gabinetes, hologramas y vista previa con partículas.

## Compilar
Requisitos: JDK 21 y Maven 3.8+.

    mvn clean package

El JAR queda en `target/AdvancedDefensiveWalls-2.0.0.jar`.

## Instalar
1. Copia el JAR a `plugins/` de tu servidor Paper 1.21.11 (Java 21) y reinicia.
2. Se crean `config.yml`, `structures.yml` (estructuras, durabilidad, gabinetes y su almacén) y `players.yml` (selección/modo por jugador).
3. Si existía `walls.yml` de la v1 se migra solo (queda como `walls.yml.migrated`). Los muros migrados no tienen gabinete: construye un gabinete cerca (los adopta) o usa `/wall cabinet link`.

## Herramientas
- **Papel**: clic derecho abre el menú de selección (guardado por jugador). Clic izquierdo sobre una estructura la destruye entera (compatibilidad v1).
- **Palo**: SHIFT + clic cambia de modo (Construcción, Eliminación, Mejora, Reparación, Información). Clic derecho normal ejecuta el modo:
  - Construcción: coloca la estructura activa donde apuntas (holograma verde/rojo mientras apuntas con el palo equipado). Cobra materiales (gratis en creativo).
  - Eliminación: elimina la estructura apuntada (devuelve su objeto).
  - Mejora: Madera → Piedra (coste en `costs.upgrade`; por defecto 128 de madera). Conserva ID, vínculo y almacén.
  - Reparación: consume materiales y recupera durabilidad (`costs.repair`).
  - Información: abre la interfaz de la estructura (o el gabinete).
- **Pico**: golpear una estructura resta durabilidad a toda la estructura (`pickaxe.*`). A 0 se destruye.
- **Gabinete**: clic derecho abre la interfaz: zona de depósito (arriba), materiales almacenados, consumo/min, lista de estructuras conectadas y botón para vincular estructuras cercanas sin gabinete.
- **Puerta blindada**: clic derecho (sin herramienta) la abre/cierra.
- **Cofre doble**: cofre doble real de Minecraft.

## Mantenimiento
Techos, escaleras, muros y suelos consumen `0.25` unidades/min del material de su nivel (acumulador fraccionario: se descuenta 1 unidad cada 4 min). Sin materiales o sin gabinete pierden durabilidad (`deterioration.*`), avisan visualmente y a 0 se destruyen. Si un gabinete se destruye, sus materiales caen al suelo y las estructuras quedan sin mantenimiento (no desaparecen); reasígnalas con `/wall cabinet link [force]` mirando otro gabinete.

## Comandos (permiso `defensivewalls.admin`, solo OP por defecto)
- `/wall give <tipo> [cantidad] [jugador]` — tipos: muro_madera, muro_piedra, techo_madera/piedra, suelo_*, escalera_*, escalera_vertical_*, cofre, gabinete_madera/piedra, puerta (también `madera` y `piedra` como en la v1).
- `/wall reload`, `/wall remove`, `/wall info`, `/wall maintenance`, `/wall cabinet [open|link [force]|info]`, `/wall repair [all]`.

Permiso de uso (papel, palo, gabinete, picos): `defensivewalls.use` (todos por defecto).

## Arquitectura
`structures` (modelo), `structures.types` (formas), `structures.builder` (validación y construcción atómica), `structures.manager` (registro), `structures.storage` (YAML seguro y asíncrono), `maintenance`, `durability`, `holograms`, `particles`, `tools`, `gui`, `listeners`, `commands`, `configuration`, `utils`. Para añadir una estructura nueva: valor en `StructureType` + clase en `structures.types` + registro en `ShapeRegistry`.

## Notas
- La comprobación de protecciones lanza `BlockPlaceEvent` de prueba (los plugins de registro pueden verlos). Desactiva `particles.check-protection` si molesta.
- El icono 🛡 del holograma puede no existir en la fuente por defecto del cliente; cámbialo en `holograms.icon`.
