# Guía de plantillas — SetFinder (Fase 2b)

La app compara cada carta recortada con **6 imágenes de referencia** que tú debes crear.

---

## Paso 1: Preparar la carpeta en el proyecto

Copia tus PNG aquí (nombres exactos, minúsculas):

```
app/src/main/assets/templates/
├── forma/
│   ├── forma_oval.png
│   ├── forma_rombo.png
│   └── forma_onda.png
└── relleno/
    ├── relleno_solido.png
    ├── relleno_rayado.png
    └── relleno_vacio.png
```

Tras añadir o cambiar archivos: **Build → Rebuild Project** y vuelve a instalar la app.

---

## Paso 2: Sacar los recortes (la forma más fácil)

1. Abre la app en el móvil.
2. Haz una foto de **varias cartas** con buena luz (como las de tu muestrario).
3. En la sección **"Cartas recortadas"** verás miniaturas numeradas.
4. En el PC, abre la carpeta del proyecto o usa capturas de pantalla.

**Opción A — Desde Android Studio (recomendada)**  
Tras la foto, las miniaturas están en pantalla. Haz captura, recorta en Paint / GIMP / Photoshop cada carta que necesites.

**Opción B — Guardar desde el emulador**  
Si pruebas en emulador, puedes extraer la foto de `cache/fotos/foto_set.jpg` con Device File Explorer.

---

## Paso 3: Elegir qué carta usar para cada plantilla

No hace falta una carta perfecta de cada combinación. Solo necesitas **un ejemplo claro** de cada atributo:

| Archivo | Qué debe mostrar | Ejemplo del muestrario |
|---------|------------------|------------------------|
| `forma_oval.png` | Óvalo bien visible | 1 morado óvalo sólido |
| `forma_rombo.png` | Rombo bien visible | 1 morado rombo sólido |
| `forma_onda.png` | Onda bien visible | 1 verde onda vacía |
| `relleno_solido.png` | Interior completamente relleno | 3 rojo onda sólido |
| `relleno_rayado.png` | Franjas visibles | 2 rojo óvalo rayado |
| `relleno_vacio.png` | Solo contorno, interior blanco | 1 verde onda vacía |

**Importante:** Usa recortes de **tu móvil y tu luz habituales**, no imágenes de internet. Así las plantillas coinciden con tus fotos reales.

---

## Paso 4: Preparar cada PNG

1. **Recorta** solo la carta blanca (como hace la app), sin mesa alrededor.
2. **Tamaño orientativo:** 120–200 px de ancho (la app redimensiona sola).
3. **Formato:** PNG.
4. **Nombre:** exactamente como la tabla (sin espacios, sin mayúsculas).
5. Puede ser **a color** (rojo/verde/morado); la app convierte a silueta binaria al cargar.

Consejos:
- Foto con luz uniforme, sin sombras fuertes.
- Que el símbolo ocupe buena parte del recorte.
- Para `vacio.png`, que se vea claro el contorno fino.

---

## Paso 5: Verificar

1. Coloca los 6 PNG en las carpetas indicadas.
2. `.\gradlew.bat installDebug` (o Run en Android Studio).
3. Al abrir la app:
   - Si **faltan** plantillas → verás un aviso y usará el método antiguo.
   - Si **están las 6** → usará plantillas automáticamente.
4. Repite la misma foto de prueba y compara resultados en la cuadrícula.

---

## Paso 6: Afinar (si algo sigue fallando)

1. Sustituye solo la plantilla que falla (ej. `forma/onda.png`).
2. Rebuild + reinstalar.
3. Prueba de nuevo.

Si una forma y un relleno se confunden, usa plantillas **más típicas** (símbolo centrado, 1 solo símbolo en la carta).

---

## Cómo funciona por dentro (Fase 2b)

```
Recorte de carta
    → detectar color (HSV)
    → máscara del color → aislar el símbolo más grande
    → centrar símbolo en cuadrado 96×96
    → FORMA: plantillas del símbolo + análisis de contorno (defectos, vértices)
    → RELLENO: rasgos de densidad del símbolo, umbrales calibrados con tus 3 PNG
    → contar número (proyección vertical en máscara completa)
    → objeto Carta
```

Clases: `TemplateLibrary`, `CardTemplateMatcher`, `SymbolAnalyzer`, `ShapeAnalyzer`, `FillCalibrator`.

**Importante (Fase 2b):** Las plantillas deben ser **fotos reales** de cartas físicas con tu móvil, no capturas de la sección "Cartas recortadas" de la app. Si usas capturas de pantalla, los umbrales calibrados no coincidirán con las fotos de cámara.

---

## Checklist rápido

- [ ] 6 archivos PNG con nombres exactos
- [ ] En `app/src/main/assets/templates/forma/` y `.../relleno/`
- [ ] Rebuild del proyecto
- [ ] Reinstalar en el móvil
- [ ] Probar con la misma foto que antes
