# Normsafio (Paper 1.21.1, Java 21)

Plugin con bloques 3D propios, máquina expendedora, pozo-banco, kart, bosses custom, armaduras de set y Cuchara de la Resurrección.

## Compilar con GitHub
1. Sube **todo el contenido** de este zip a un repositorio (el `pom.xml` debe quedar en la raíz).
2. Crea el archivo `.github/workflows/build.yml` con el contenido del `build.yml` que va aparte
   (en la web: *Add file → Create new file* y escribe `.github/workflows/build.yml` como nombre).
3. En la pestaña **Actions** se compila solo; descarga el `.jar` en *Artifacts → Normsafio-jar*.
4. Mete el `.jar` en `plugins/` del servidor Paper 1.21.1.

## Resource pack
Sube `Normsafio-ResourcePack.zip` a un hosting con enlace directo y pon la URL (y el sha1 si quieres) en `plugins/Normsafio/config.yml` → `resource-pack`.

## Comandos (solo OPs)
`/norsafio give <jugador> <id> [n]` · `items` · `boss <coloso|reina|devorador>` · `reto start [boss] | stop` · `revive <jugador>` · `saldo <jugador> <n>` · `limpiar` · `reload`

## Cómo se usa
- **Bloques 3D**: `/norsafio give <tú> block_vending` (también `block_well`, `block_bonfire`, `block_crystal`, `block_throne`, `block_brazier`). Solo los OPs los colocan; para quitarlos, agáchate y golpea (o rómpelos en creativo).
- **Máquina expendedora**: clic derecho. **Pozo**: clic derecho → depositar/retirar deditas.
- **Reto**: lo inicia un OP; quien muere queda en espectador y aparece en la Cuchara. **Cuchara**: clic derecho → elige al caído.
- **Kart**: clic derecho en el suelo para colocarlo; agáchate + clic para recogerlo.
- **Armaduras**: set Guardián (diamante) y Abismo (netherita) con efectos al llevar las 4 piezas.
