# Reading and continuing the code

The source uses English names and a consistent Java layout. Start with the algorithm, then follow how the application supplies data to it.

## Suggested reading order

1. `optimization/ObjectiveFunction.java`: the three operations an optimizer needs.
2. `optimization/OptimizerConfig.java` and `OptimizationResult.java`: inputs, exit reason and recorded history.
3. `optimization/GradientDescent.java`: validation, gradient, stopping check and one simultaneous vector update.
4. `optimization/NesterovGradientDescent.java`: the same ordinary-point checks plus the look-ahead gradient and momentum update.
5. `model/LogisticObjective.java`: stable binary loss, analytic gradient and safe step-size bound.
6. `data/StudentCsvLoader.java`, `DataSplit.java` and `Standardizer.java`: selection of columns/outcomes, train/test separation and training-only feature scaling.
7. `app/RetentionRadarApp.java`: the complete console workflow and how the same optimizer is called in each mode.
8. `app/Evaluation.java`, `ComparisonReport.java` and `Watchlist.java`: measured performance, optimizer work and output.
9. `verify/`: runnable examples of expected behavior and error cases.

Paths above are relative to `src/auk/sdt202/team4/`.

## Names and mathematical notation

| Mathematical concept | Meaning in the implementation |
|---|---|
| f | `objective`: the function supplied to an optimizer |
| x0 | `initialPoint`: copied before optimization |
| x_k | `currentPoint`: the ordinary point returned, stopped on and stored in history |
| y_k | `lookaheadPoint`: Nesterov's point used for the update gradient |
| alpha | Learning rate in `OptimizerConfig` |
| epsilon | Gradient-norm tolerance in `OptimizerConfig` |
| beta | Nesterov momentum coefficient |
| m | Number of data rows |
| d | Number of coefficients, including bias |
| z_i | Logit: the dot product of weights and one feature row |
| lambda | Slope regularization strength; the bias is excluded |

Longer English names in code make these roles visible without requiring a formula beside each line. The actual optimizer update remains visible in its main loop.

## Application workflow

`main` prints the header and delegates to `runRequestedMode`. With no command-line arguments, `runInteractiveMenu` owns the current training session, latest training result and decision costs.

`prepareTrainingSession` splits the raw cohort, fits the standardizer on training features, transforms both subsets, creates the training objective and chooses an optimizer configuration. It does not train on test rows.

`trainClassicalModel` and `trainAcceleratedModel` run a single method. `compareCohortModels` runs both, prints their work counts, evaluates held-out rows and selects a model for the displayed ranking using its optimization stop reason. It does not select a method based on test accuracy.

`readDecisionCosts` reads two positive error costs. Changing those costs changes only the ranking threshold; it leaves the trained coefficients intact.

## How to extend it

Keep data loading, optimizer mathematics and console output separate. An optimizer should accept an objective, point and configuration and return a result. It should not print, load students or decide whom to contact.

For a new feature, use a descriptive method name, give each variable one clear role, keep validation near the input it checks, and make each major step visible. Prefer a simple loop when a nested stream or compound expression would obscure the work. Keep small utility methods together with the class whose job they support.

Do not regroup floating-point sums during a readability-only change: mathematically equivalent expressions can produce different rounded results and iteration counts. Run `--verify` and compare the deterministic demo measurements in `validation.md` after changing the algorithm or data pipeline.

The assignment proposal's reported comment rule still applies. This package supplies explanatory documentation separately; team members must write any required submission comments and header in their own words after reviewing the implementation.
