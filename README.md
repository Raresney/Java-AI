# Java-AI

A collection of Java-based machine learning classification algorithms with interactive GUI visualization. Each project provides real-time 2D rendering of decision boundaries, dataset loading from CSV/ARFF files, and a synthetic data generator for experimentation.

Built with pure Java AWT — no external libraries required.

## Projects

### [SVM-Simulator](./SVM-Simulator)
Base framework providing the shared GUI architecture, 2D visualization canvas, data I/O, and an abstract `Algorithm` class that the classifiers extend.

### [Median-Classifier](./Median-Classifier)
Binary classifier using the **Median method**. Computes class centroids, derives a linear separating hyperplane from their midpoint, and evaluates accuracy on a test set. Includes per-attribute statistical analysis panels.

### [Perceptron-Classifier](./Perceptron-Classifier)
Binary classifier using the **Perceptron learning algorithm**. Iteratively adjusts weights until convergence, with a configurable learning rate. Supports pause/resume during training so the decision boundary can be inspected mid-iteration.

## How to Run

Each project follows the same structure. From a project's `SVM/` directory:

```bash
# Compile
javac -classpath svm; svm/SVM.java

# Run
java -Xmx1024m -classpath svm; svm.SVM
```

Windows `.BAT` scripts (`compilare.BAT`, `start.BAT`) are also included.

## Data Format

The projects use a modified ARFF format:

```
@relation dataset_name
@attribute attr_1 numeric
@attribute attr_2 numeric
@attribute class {0, 1}
@data
-815.94, 98.27, 0
427.36, 656.61, 1
```

Sample datasets are included in each project's `data/` folder. Additional datasets can be generated with the built-in **InputDataGenerator** tool (Tools menu).

## Author

[Raresney](https://github.com/Raresney)
