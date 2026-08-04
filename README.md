# SetFinder

Detector de SETs para el juego de mesa **SET**. Aplicación Android escrita en **Java** (Views clásicas XML + Material 3).

Introduce las cartas a mano o tómalas en foto y la app te dirá si en esa combinación existe algún SET válido, y cuáles lo forman.

---

## Qué hace

- **Entrada manual**: toca cada carta y define sus 4 atributos (número, color, forma y relleno) con spinners.
- **Entrada por foto (opcional)**: la cámara captura las cartas y la app **detecta color y forma** automáticamente con OpenCV.
- **Cantidad configurable**: 12 o 15 cartas.
- **Detección de SETs**: comprueba si existe al menos un SET y muestra cuáles lo forman, resaltándolos en la cuadrícula.
- **Estado persistente**: la partida (cartas, foto y resultados) no se pierde al rotar la pantalla.
- **Pantalla de ayuda** integrada con las reglas del juego.

## Reglas del juego (resumen)

Cada carta tiene 4 atributos, cada uno con 3 valores posibles:

| Atributo | Valores |
|----------|---------|
| Número | 1, 2, 3 |
| Color | Rojo, Verde, Morado |
| Forma | Óvalo, Rombo, Onda |
| Relleno | Sólido, Rayado, Vacío |

Tres cartas forman un **SET** si, para **cada** atributo, los 3 valores son **todos iguales** o **todos distintos**.

## Capturas

### Introducción manual

<img width="200" height="458" alt="Introduccion manual" src="https://github.com/user-attachments/assets/4ff67ab4-4ebf-46f9-b093-742239d5be87" />

### Editado de carta

<img width="200" height="458" alt="Editar carta" src="https://github.com/user-attachments/assets/ea59541c-c33d-4d41-9fce-135884df230a" />

### Reconocimiento mediante fotografía

<img width="200" height="458" alt="Reconocimiento por foto" src="https://github.com/user-attachments/assets/cb4028fd-3d80-4f4b-a366-9bba0c4ecd16" />

### Cartas detectadas automáticamente

<img width="200" height="458" alt="Cartas detectadas" src="https://github.com/user-attachments/assets/08a1d4d9-ffe4-4316-9eb6-272c537b1ad7" />

### SETs encontrados

<img width="200" height="458" alt="SETs encontrados" src="https://github.com/user-attachments/assets/ad3dbfe5-1d0f-433f-878f-1d490c8fad6d" />

### Ayuda integrada

<img width="200" height="458" alt="Ayuda" src="https://github.com/user-attachments/assets/e4081a95-e9f2-4b07-aa9e-c1b045ecbf3d" />

## Requisitos

- Android 5.0 (API 21) o superior.
- La foto opcional requiere cámara y permiso de cámara en tiempo de ejecución.
- OpenCV 4.9 se empaqueta con la app (no requiere OpenCV Manager).

## Compilar e instalar

Desde Android Studio:

1. Abre la carpeta `SetFinder2`.
2. Espera a que Gradle sincronice las dependencias.
3. Conecta un dispositivo o inicia un emulador y pulsa **Run**.

O por línea de comandos:

```bash
# APK de depuración
.\gradlew.bat :app:assembleDebug

# Instalar en el dispositivo conectado
.\gradlew.bat :app:installDebug

# Tests unitarios de la lógica SET
.\gradlew.bat :app:testDebugUnitTest
```

## Cómo usar

1. Elige 12 o 15 cartas.
2. Toca cada carta y completa sus atributos. (*Opcional*: pulsa **«Tomar foto»** para que la app sugiera color y forma automáticamente; después revisa cantidad y relleno).
3. Pulsa **«Comprobar si hay sets»**.
4. Si hay SETs, pulsa **«Ver sets»** para ver qué cartas los forman.

## Reconocimiento de cartas

El reconocimiento funciona en dos niveles:

1. **Detección**: `CardDetector` encuentra los contornos de las cartas sobre la mesa y las recorta.
2. **Reconocimiento**: si existen las 6 plantillas PNG (`assets/templates/`), se usan (`CardTemplateMatcher`); si no, se usa el reconocedor heurístico (`CardRecognizer`). En ambos casos se detectan **color y forma**; cantidad y relleno los define el usuario.

El pipeline de visión usa OpenCV: conversión a HSV → máscara por color → aislamiento del símbolo → comparación de siluetas (IoU) contra plantillas → análisis de contorno (forma) y de densidad (relleno).

> Si faltan plantillas, la app muestra un aviso al arrancar. Cómo crearlas: [PLANTILLAS_GUIA.md](PLANTILLAS_GUIA.md).

## Estructura del proyecto

```
SetFinder2/
├── app/src/main/java/com/mipel/setfinder/
│   ├── MainActivity.java          # Pantalla principal y flujo de interacción
│   ├── SplashActivity.java        # Pantalla de portada (launcher)
│   ├── AyudaActivity.java         # Pantalla de ayuda / reglas
│   ├── Carta.java                 # Modelo de carta
│   ├── SetUtils.java              # Lógica SET (esSet, buscarSets)
│   ├── MainViewModel.java         # Estado de la partida (sobrevive a la rotación)
│   ├── CardDetector.java          # Detección de cartas en la foto
│   ├── CardRecognizer.java        # Reconocimiento heurístico (color/forma)
│   ├── CardTemplateMatcher.java   # Reconocimiento con plantillas
│   ├── CardColorDetector.java     # Color dominante (HSV)
│   ├── CardNumberDetector.java    # Conteo de símbolos
│   ├── CardImageUtils.java        # Utilidades de imagen (máscaras, siluetas)
│   ├── CardCropper.java           # Recorte de cartas
│   ├── SymbolAnalyzer.java        # Aislamiento y rasgos del símbolo
│   ├── ShapeAnalyzer.java         # Detección de forma
│   ├── FillDetector.java          # Detección de relleno
│   ├── FillCalibrator.java        # Clasificación de relleno por plantillas
│   ├── TemplateLibrary.java       # Carga de plantillas desde assets
│   └── TemplateMatchUtils.java    # Comparación de siluetas (IoU)
└── app/src/main/res/
    ├── layout/                    # activity_main, actividad_ayuda, diálogos...
    ├── values/ y values-night/    # Tema Material 3 claro/oscuro
    └── drawable/                  # Fondos, iconos, logo vectorial
```

## Tests

- `app/src/test/java/.../SetUtilsTest.java`: 9 tests unitarios de `esSet` y `buscarSets` (JUnit 4).

## Tecnologías

- **Lenguaje**: Java 11
- **UI**: Android Views + Material Components 3 (tema claro/oscuro)
- **Visión**: OpenCV Android 4.9.0
- **Build**: Gradle + AGP 8.10

## Licencia

Este proyecto se distribuye bajo la licencia MIT. Consulta el archivo [LICENSE](LICENSE) para más información.
