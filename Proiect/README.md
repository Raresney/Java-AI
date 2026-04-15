# Proiect — SVM Face Detection and Recognition

A complete face detection and recognition system built from scratch in Java,
without any ML libraries. OpenCV is used **only** for webcam frame grabbing
and drawing rectangles over images.

## What it does

1. **Detects the head** (square bounding box around the face) in an image using
   an SVM trained on HOG features, with sliding window over a scale pyramid +
   Non-Maximum Suppression.
2. **Picks the largest-area face** and crops it to 128×128.
3. **Captures webcam images** for each person (500 per person).
4. **GUI editor** to browse and delete bad captures.
5. **HOG** implemented from scratch (Dalal & Triggs 2005): gradients, 8×8
   cells, 2×2 blocks, 9 bins, L2-Hys normalization.
6. **Per-person recognition classifier** (one-vs-rest), serializable.
7. **SMO** (Sequential Minimal Optimization, Platt 1998) with Sigmoid kernel.
   Linear and RBF kernels are also provided for comparison.
8. **Live test** at ~10 FPS: detects heads, draws green squares, writes the
   recognized pseudonym above each.

## Layout

```
Proiect/
├── src/proiect/
│   ├── alg/        HOG, Kernel (Sigmoid/Linear/RBF), SVMClassifier, SMO
│   ├── detect/     BoundingBox, HeadDetector (sliding window + pyramid + NMS)
│   ├── io/         ImageUtils, ModelStore, DatasetLoader
│   ├── webcam/     CameraCapture, FrameDrawer  (the ONLY OpenCV calls)
│   ├── gui/        MainWindow + 4 panels (Capture, Browser, Annotator, LiveTest)
│   └── tools/      PregateSetCap, CurataImagini, Antrenare
├── invatare_cap/   detector dataset (pozitive/ + negative/ at 64×64)
├── invatare_fete/  recognition dataset (<person>/ with 128×128 PNGs)
├── clasificatoare/ serialized .ser models
├── vectori_hog/    saved HOG vectors (for delivery)
├── compilare.BAT   compiles everything under src/
└── start.BAT       launches proiect.Main
```

## Build and run

Requires:
- JDK 17 (Eclipse Adoptium recommended)
- OpenCV 4.10.0 with Java bindings

JDK and OpenCV paths are configured in `compilare.BAT` and `start.BAT` — adjust
them to your local installation.

```bat
compilare.BAT       :: errors go to erori.txt
start.BAT           :: launches the GUI
```

## Usage flow

1. **Train the head detector** (one-time, using the ORL bootstrap dataset):
   ```
   java proiect.tools.PregateSetCap
   ```
   Downloads the ORL Database of Faces (400 faces), generates 400 synthetic
   negatives (noise, gradients, stripes, checkerboards, blobs), trains a
   Linear SVM. Output: `clasificatoare/detector_cap.ser`.

2. **Webcam capture**: `Captura` tab → pseudonym → Start → 500 images.

3. **Automatic cleanup** (optional):
   ```
   java proiect.tools.CurataImagini <pseudonym>
   ```
   Deletes images with low variance (uniform wall) or that the current
   detector no longer recognizes as faces.

4. **Manual cleanup**: `Imagini` tab → right-click → Delete on bad thumbnails.

5. **Train recognition**: `Antrenare → Antreneaza recunoastere` menu for each
   person.

6. **Live test**: `Test live` tab.

## Implemented algorithms

| Component | File | Requirement |
|---|---|---|
| HOG | [src/proiect/alg/HOG.java](src/proiect/alg/HOG.java) | 5 |
| Soft-margin SVM | [src/proiect/alg/SVMClassifier.java](src/proiect/alg/SVMClassifier.java) | 1, 6 |
| SMO + Sigmoid | [src/proiect/alg/SMO.java](src/proiect/alg/SMO.java), [KernelSigmoid.java](src/proiect/alg/KernelSigmoid.java) | 7 |
| Sliding window + pyramid + NMS | [src/proiect/detect/HeadDetector.java](src/proiect/detect/HeadDetector.java) | 1, 2 |
| 500-image capture | [src/proiect/gui/CapturePanel.java](src/proiect/gui/CapturePanel.java) | 3 |
| Browser + delete | [src/proiect/gui/BrowserPanel.java](src/proiect/gui/BrowserPanel.java) | 4 |
| Model serialization | [src/proiect/io/ModelStore.java](src/proiect/io/ModelStore.java) | 6 |
| Live test | [src/proiect/gui/LiveTestPanel.java](src/proiect/gui/LiveTestPanel.java) | 8 |

## Performance notes

The detector uses a **Linear kernel** with a precomputed weight vector
`w = Σ αᵢ·yᵢ·SVᵢ`. Instead of evaluating `K(SVᵢ, x)` against hundreds of
support vectors per window, the decision becomes a single dot product:
`f(x) = w·x + b`. Hundreds of times faster — essential for the 10 FPS live
test.

The Sigmoid kernel is still used for per-person recognition classifiers
(requirement 7).

## Author

Bighiu Rareș (group 534)
