# Arhitectura proiectului — Detecție și recunoaștere facială cu SVM

## Scop
Implementare completă în Java (fără biblioteci ML) a unui sistem care:
1. Detectează capetele persoanelor într-o imagine (clasificator SVM cu trăsături HOG, sliding window).
2. Capturează imagini de la webcam pentru fiecare persoană.
3. Antrenează câte un clasificator SVM per persoană (one-vs-rest).
4. Recunoaște persoanele live de la webcam.

Singurele apeluri native permise: OpenCV pentru `VideoCapture` (preluare cadre) și `Imgproc.rectangle` / `Imgproc.putText` (desenare). Restul algoritmilor — implementați manual.

## Structură de directoare
```
Proiect/
├── compilare.BAT            compilează tot src/ în clase
├── start.BAT                lansează proiect.Main
├── ARHITECTURA.md           acest fișier (va deveni PDF)
├── erori.txt, log.txt       output redirectat al BAT-urilor
├── clasificatoare/          modele .ser serializate
│   ├── detector_cap.ser
│   └── recunoastere_<pseudonim>.ser
├── invatare_cap/            dataset pentru detectorul de cap
│   ├── pozitive/            decupaje 64x64 cu fețe
│   └── negative/            decupaje 64x64 fără fețe
├── invatare_fete/           dataset pentru recunoaștere
│   └── <pseudonim>/         imagini 128x128 capturate de la webcam
└── vectori_hog/             vectori HOG salvați (livrabil)
```

## Pachete și clase

### `proiect`
- **Main.java** — punct de intrare; afișează `gui.MainWindow`.

### `proiect.alg` — algoritmii puri (fără dependențe native)
- **Kernel.java** — interfață: `double k(double[] x, double[] y)`, `String name()`.
- **KernelSigmoid.java** — `tanh(γ·xᵀy + r)`. Nucleu cerut explicit la pct. 7.
- **KernelLinear.java**, **KernelRBF.java** — pentru testare comparativă.
- **SVMClassifier.java** — model serializabil: vectori suport, α-uri, bias `b`, kernel; metodă `predict(double[] x) → ±1` și `decision(double[] x) → double`.
- **SMO.java** — Sequential Minimal Optimization (Platt 1998); rezolvă duala SVM cu marjă slabă (parametru `C`); produce un `SVMClassifier`.
- **HOG.java** — extrage vector Histogram of Oriented Gradients dintr-o imagine grayscale. Parametri impliciți: celule 8×8, blocuri 2×2, 9 binuri (0–180°), normalizare L2-Hys.

### `proiect.detect` — detecție „pătrat cap"
- **BoundingBox.java** — `int x, y, latura; double scor`. Metode: `aria()`, `intersectie(BoundingBox)`, `iou(BoundingBox)`.
- **HeadDetector.java** — sliding window pe piramidă de scale; pentru fiecare fereastră 64×64 calculează HOG, întreabă SVM-ul de cap; aplică Non-Maximum Suppression. Implementează cerințele 1 și 2.

### `proiect.io`
- **ImageUtils.java** — conversie BGR→grayscale, scalare biliniară la 128×128, încărcare/salvare PNG (folosim `javax.imageio` din JDK, nu OpenCV, pentru disk I/O).
- **ModelStore.java** — `salveaza(Object, String)` / `incarca(String)` cu `ObjectOutputStream`.
- **DatasetLoader.java** — parcurge un director, returnează listă de `(double[] vectorHOG, int eticheta)`.

### `proiect.webcam` — singurul loc cu OpenCV
- **CameraCapture.java** — wrapper peste `org.opencv.videoio.VideoCapture`. Metode: `open()`, `nextFrame() → int[][] grayscale`, `close()`.
- **FrameDrawer.java** — primește un `Mat` și o listă de `BoundingBox` cu etichete; desenează cu `Imgproc.rectangle` și `Imgproc.putText` (verde).

### `proiect.gui` — interfață Swing
- **MainWindow.java** — `JFrame` cu meniu: „Adnotează cap", „Antrenează detector cap", „Capturează față persoană", „Vizualizează / șterge", „Antrenează recunoaștere", „Test live".
- **AnnotatorPanel.java** — încarcă imagini din Internet salvate de utilizator în `invatare_cap/raw/`; clic+drag pentru a desena pătrate peste fețe; salvează decupaje în `pozitive/` și ferestre random fără suprapunere în `negative/`.
- **CapturePanel.java** — cerința 3: 500 cadre, fiecare trecut prin `HeadDetector.crop128()`, salvat ca `<pseudonim>_yyyyMMdd_HHmmss_SSS.png`.
- **BrowserPanel.java** — cerința 4: grilă de thumbnails per persoană; click stânga = previzualizare, click dreapta = șterge.
- **LiveTestPanel.java** — cerința 8: thread cu ~10 FPS, detectează capete, scalează, HOG, întreabă fiecare clasificator de persoană, scrie pseudonimul deasupra pătratului.

### `proiect.tools`
- **HardNegativeMiner.java** — rulează detectorul pe imagini fără fețe, salvează falsurile pozitive ca negative noi → reantrenare. Îmbunătățește precizia detectorului de cap.

## Fluxuri principale

**Antrenare detector cap (cerință 1):**
`AnnotatorPanel` → `pozitive/`, `negative/` → `DatasetLoader` → `HOG` per imagine → `SMO.antreneaza(X, y, kernel, C)` → `SVMClassifier` → `ModelStore.salveaza("detector_cap.ser")`.

**Capturare persoană (cerință 3):**
`CameraCapture` → cadru → `HeadDetector.detecteaza()` → `argmax(arie)` → decupaj 128×128 → salvare cu timestamp.

**Antrenare recunoaștere (cerință 6):**
Pentru fiecare persoană P: imaginile lui P → +1, ale celorlalți → −1; HOG; `SMO`; salvare `recunoastere_P.ser`.

**Test live (cerință 8):**
Buclă FPS: cadru → detectează capete → desenează verde → pentru fiecare cap, HOG → întreabă fiecare `SVMClassifier` de persoană → dacă +1, pune pseudonimul deasupra.

## Algoritmi cheie (rezumat matematic)

**HOG** (Dalal & Triggs 2005):
1. Gradienți Sobel pe X și Y → magnitudine `m` și unghi `θ ∈ [0,180)`.
2. Imagine împărțită în celule 8×8; fiecare celulă → histogramă cu 9 binuri pe `θ`, ponderată cu `m` și interpolată liniar între binurile vecine.
3. Blocuri 2×2 celule (16×16 px), pas = 8 px → concatenare → normalizare L2-Hys (L2, clip la 0.2, L2 din nou).
4. Vector final = concatenarea tuturor blocurilor.

**SMO** (Platt 1998):
- Variabile: `α_i ∈ [0,C]`, restricție `Σ α_i y_i = 0`.
- Iterativ: alege perechi `(α_i, α_j)` care violează KKT, rezolvă analitic problema redusă în 2 variabile (proiectând pe segmentul L,H), actualizează `b`.
- Convergență când toate KKT sunt satisfăcute în limita `tol`.

**Sigmoid kernel:** `K(x,y) = tanh(γ·xᵀy + r)`. Nu e Mercer pentru orice (γ,r), deci SMO poate să nu conveargă perfect — vom valida empiric.

**NMS:** sortează ferestrele după scor; păstrează prima, elimină celelalte cu IoU > prag (0.3); repetă.
