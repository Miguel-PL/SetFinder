# SET Finder - Notas del Proyecto

## Idea general

Aplicación Android desarrollada en Java para ayudar a detectar SETs válidos en el juego de mesa SET.

El objetivo principal es que el usuario pueda introducir cartas manualmente (o capturarlas con la cámara) y que la aplicación:

1. Detecte si existe algún SET válido.
2. Muestre cuántos SETs hay.
3. Revele cuáles son, resaltándolos en la cuadrícula.

## Reglas del juego SET

Cada carta tiene 4 atributos (número, color, forma, relleno), cada uno con 3 valores posibles.

Un SET válido ocurre cuando, para CADA atributo:
- los 3 valores son iguales
- O
- los 3 valores son distintos

## Estado actual (v1.0)

Completado:

- **UI**: tema Material 3 con modo oscuro, cabecera con degradado, selector 12/15 cartas, cuadrícula de cartas editables, tarjeta de resultado, resaltado de SETs, pantalla de ayuda y splash.
- **Lógica SET**: `Carta` (modelo) y `SetUtils` (`esSet`, `atributoValido`, `buscarSets`).
- **Reconocimiento por foto**: `CardDetector` encuentra y recorta cartas; color y forma se detectan automáticamente con OpenCV (plantillas en `assets/templates/` o heurística).
- **Estado de la partida**: `MainViewModel` conserva cartas, foto y resultados al rotar la pantalla.
- **Splash sin APIs deprecadas**: barra de estado oculta con la API moderna de insets.
- **Tests**: 9 tests unitarios de `SetUtils` en `src/test`.

Pendiente / ideas:

- Reconocimiento automático de cantidad y relleno desde la foto (hoy los define el usuario).
- Decidir licencia para publicar.

## Tecnología elegida

- Android Studio
- Java 11
- XML Views tradicionales (sin Kotlin, sin Compose)
- Material Components 3
- OpenCV 4.9.0

## Historias de desarrollo

- El proyecto inicial se creó con Kotlin + Compose y generó conflictos, por lo que se descartó. El proyecto válido es `SetFinder2` (Java + Views).
- El selector de cantidad pasó de Spinner a `MaterialButtonToggleGroup` (12/15).
- El botón de ayuda se implementó como un `TextView` circular para evitar problemas de recorte del texto en `Button`.

## Referencias

- `README.md`: documento público para el repositorio.
- `PLANTILLAS_GUIA.md`: cómo crear las plantillas PNG de reconocimiento.

## Notas personales

(Usar esta sección para apuntar ideas rápidas, bugs, mejoras o dudas futuras)
