# Gradient Descent Algorithm — Team 4

**Uliana Matviienko · Andrii Moroz**  
SDT 202: Design and Analysis of Data Structures and Algorithms

Repository: [shalltearfrost/RetentionRadar](https://github.com/shalltearfrost/RetentionRadar). The repository is currently private; access must be arranged for the next contributor and instructor. Confirm any public-access requirement against the official brief.

An English Java 17 educational project implementing classical batch gradient descent and Nesterov acceleration, with a reproducible logistic-regression application on synthetic and UCI student records. Gradient descent is the central topic. The real-data task is eventual dropout versus graduation using information available by the end of the first semester.

Read [HANDOFF.md](HANDOFF.md), [algorithm notes](docs/algorithm-notes.md), [requirements review](docs/requirements-review.md), [measured validation](docs/validation.md), and [data attribution](data/DATA-SOURCE.md).

For the source reading order, mathematical symbols and application workflow, see the [code guide](docs/code-guide.md). The implementation uses descriptive English names, explicit steps and consistent formatting so another contributor can understand and extend it.

## Implemented

- Reusable classical and Nesterov optimizers with a shared function signature, ordinary-iterate stopping, explicit exit reasons, defensive copies and sampled history.
- Stable logistic loss/gradient, unregularized bias and a conservative smoothness bound for selecting the step.
- Bundled attributed UCI extract: seven first-semester features; loader retains 3,630 Dropout/Graduate outcomes from 4,424 source rows.
- Seeded stratified 80/20 split; standardization fitted only on training data.
- Held-out accuracy, precision, recall, F1, AUC, log-loss, Brier score and confusion counts.
- Comparison of updates, objective values, gradient norms and actual objective/gradient calls, with sampled loss output.
- ASCII contours on a known quadratic and projected logistic coefficient trajectories.
- Top-25 held-out record ranking and an illustrative error-cost threshold that changes without retraining.
- English interactive menu and 58 dependency-free verification checks.

This prototype is ready to study and continue. Final report formatting, official templates, human-authored comments/header, presentation screenshots/slides, repository-access confirmation and contribution evidence remain pending.

## Run in IntelliJ

1. Clone the linked repository or extract the archive, then open the project folder.
2. Select JDK 17 as the project/module SDK. Included `.iml` and `.idea` files mark `src` as the source root; adjust the SDK name for your installed JDK.
3. Set the working directory to the project root.
4. Run `auk.sdt202.team4.app.RetentionRadarApp`; no arguments opens the menu.

Program arguments: `--verify`, `--quadratic`, `--compare`, `--synthetic`, or `--real`. You can also use `--real "path/to/another.csv"` with the required schema.

No Maven, Gradle or external Java libraries are needed. Both datasets and checks run offline once a JDK is available.

## Run in PowerShell

With JDK 17 or later selected through `JAVA_HOME` or `PATH`:

```powershell
.\run.ps1 -Verify
.\run.ps1 -Real
.\run.ps1 -Synthetic
.\run.ps1 -Compare
.\run.ps1 -Quadratic
.\run.ps1
```

To choose a JDK explicitly:

```powershell
.\run.ps1 -JavaHome 'C:/path/to/jdk-17' -Real
```

To load a different file:

```powershell
.\run.ps1 -Real -DataPath 'C:/path/to/students.csv'
```

Build only with `.\build.ps1`. Scripts use `--release 17`, resolve the bundled CSV from the project root, and support Windows PowerShell 5.1 and PowerShell 7. Java 8 alone is insufficient. The portable validation JDK is outside the deliverable; recipients select their own JDK.

## Console workflow

The menu starts with the reproducible synthetic cohort. Choose 1 to load the bundled UCI extract, 3/4 to train a method, 5 to compare both and evaluate held-out records, 6 to display scores, 7 to change illustrative costs, 8 for the quadratic comparison, or 9 for verification. Changing cohorts clears the latest fitted model.

The default score threshold is 0.5. Increasing the missed-positive error cost lowers the threshold; model coefficients do not change. Costs are demonstration assumptions, not evidence of intervention effectiveness.

## Results from the recorded run

On the fixed UCI split (training 2,904 / test 726), GD converged after 865 updates and Nesterov after 375. They used 866 and 749 gradient calls respectively. Both produced held-out accuracy 0.88430 and AUC 0.91960 to the displayed precision. Update ratios are not timing speedups; Nesterov also evaluates look-ahead points. See [validation](docs/validation.md) for configuration, confusion counts and limits.

## Layout

```text
src/auk/sdt202/team4/
  app/           Driver, comparison, contours, ranking, evaluation, costs
  optimization/  Shared interface/config/results, classical GD, Nesterov, vector operations
  model/         Stable LogisticObjective
  data/          SyntheticCohort, StudentCsvLoader, DataSplit, Standardizer
  verify/        Optimizer, model, acceleration, data and evaluation checks
data/            Selected UCI CSV, attribution and provenance hashes
docs/            Theory, requirements, measured evidence and source proposal
```

Optimizer working storage is O(d) with history disabled; the data model separately owns O(m*d) storage. See algorithm notes for complete complexity and assumptions.

## Source, authorship and final submission

Starting material: the downloaded `RetentionRadar Project Design.html`, preserved as text in `docs/source/`. It is a design proposal rather than the instructor's original assignment. Topic fit is confirmed; official compliance still requires the actual brief and templates.

This work was prepared with AI assistance. The proposal attributes a no-AI-generated-comments rule to the assignment, so Java files have no generated explanatory comments. Members must understand the code and write any required comments and information header themselves. Record genuine human contributions under the correct names; no contribution history or public repository URL is fabricated here.

## References

- [Tibshirani, Gradient Descent](https://www.stat.cmu.edu/~ryantibs/convexopt/lectures/grad-descent.pdf).
- [Su, Boyd and Candes (2016), Nesterov analysis](https://web.stanford.edu/~boyd/papers/pdf/ode_nest_grad.pdf).
- [UCI dataset #697](https://archive.ics.uci.edu/dataset/697/predict+students+dropout+and+academic+success), [Realinho et al. (2022), descriptor](https://www.mdpi.com/2306-5729/7/11/146), and [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- [Shalizi, Logistic Regression](https://stat.cmu.edu/~cshalizi/dm/20/lectures/07/lecture-07.html).

The included data extract is attributed in `data/DATA-SOURCE.md`. Performance is from one reproducible selected-cohort split; it is not validated institutional deployment performance.
