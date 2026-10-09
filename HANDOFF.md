# Handoff to the next contributor

**Team 4: Uliana Matviienko and Andrii Moroz**  
**Topic: Gradient descent algorithm**  
Updated on 9 October 2026. All project-facing text is in English.

Repository: [shalltearfrost/RetentionRadar](https://github.com/shalltearfrost/RetentionRadar). It is currently private. Arrange access for the next contributor; confirm the instructor's repository visibility requirement before final submission.

## Start here

1. Select JDK 17 in IntelliJ and set the working directory to the project root.
2. Run `RetentionRadarApp --verify`: 58 checks should pass.
3. Run `--compare` to see GD/Nesterov on a known quadratic, then `--real` for the complete UCI pipeline.
4. Read `docs/algorithm-notes.md`, `docs/requirements-review.md`, `docs/validation.md` and `data/DATA-SOURCE.md`.
   Use `docs/code-guide.md` to follow the source in a suggested reading order.
5. Use the source proposal in `docs/source/` as background together with the corrections in the review.

## Completed and checked

Classical GD and Nesterov share `minimize(ObjectiveFunction,double[],OptimizerConfig)`. Both return and trace ordinary iterates, assess gradient-norm convergence before an update and after the final allowed update, and report actual completed update counts. The source proposal's look-ahead return semantics were deliberately corrected for consistency with the theorem.

Nesterov uses a look-ahead gradient with floating-point momentum. Extra evaluations check ordinary-iterate convergence and look-ahead numerical state; comparison tables count them. Both algorithms allow finite loss increases rather than treating every increase as divergence.

The stable logistic objective, seeded fictional cohort, strict CSV loader, stratified split, train-only standardizer, held-out evaluation, ranking, cost threshold and contours are implemented. The dataset extract preserves all 4,424 source records and selected values; the loader removes 794 Enrolled outcomes, leaving 3,630 resolved records.

On the recorded UCI split, train=2,904 and test=726. GD took 865 updates / 866 gradient calls; Nesterov took 375 / 749. Held-out confusion counts at 0.5 are TP=216, FP=16, TN=426, FN=68 for both methods. These are this implementation's actual measurements, not the proposal's benchmarks.

## Remaining work, in priority order

1. **Official materials.** Obtain the original assignment/rubric, report example, abstract template and confirmed submission/presentation dates from Canvas. Confirm the exact team membership, programming and comment rules, and whether any mandatory feature differs from this proposal.
2. **Human review.** Every member studies the implementation and can explain all sections. Members write required comments and the driver information header in their own words. This AI-assisted package does not claim authorship by either student or supply AI-written comments for submission.
3. **Repository access and contribution evidence.** Use the linked repository, retain `.iml`, `modules.xml` and `misc.xml`, exclude `build/`, and have contributors make genuine commits under their own names. Insert this repository URL in the report/header and verify logged-out public access if the brief requires it. The initial AI-assisted project upload is not evidence that either student independently authored every section. Do not fabricate prior work or commit history.
4. **Report and abstract.** Transfer the English technical notes and measured evidence into `SDT202-project-team4.docx` with the instructor's formatting. Add the application timeline/population limitations, actual screenshots, verified references and genuine effort analysis. Write the one-page conference abstract only after confirming the template.
5. **Presentation.** Prepare the initial/final slides as required. Each member rehearses the whole algorithm and explains both the useful Nesterov result and its caveats. The menu supports a short offline live demo.
6. **Further evaluation if required.** Use additional validation splits or cross-validation on training/development data before any model/threshold tuning; preserve an untouched test set. Compare preprocessing explicitly if showing conditioning effects. Do not tune to the current held-out results and present them as a fresh test.
7. **Optional presentation polish.** Capture genuine titled screenshots and, if required, export plots from recorded trace data. ASCII contour/path output is already present. The logistic view projects real full-vector iterates; its contour slice fixes the other weights at the final reference point.

The code is a functional educational prototype. Missing official templates, human comments/header, repository-access confirmation, genuine member contribution evidence, final report/abstract/slides and screenshots still prevent claiming a complete final submission.

## Interfaces and semantics

```java
public interface ObjectiveFunction {
    int dimension();
    double value(double[] weights);
    double[] gradient(double[] weights);
}
public static OptimizationResult minimize(
    ObjectiveFunction objective, double[] initialPoint, OptimizerConfig configuration);
```

Config: `learningRate`, `tolerance`, `maxIterations`, `historyStride`; stride 0 disables history. Result: `point`, `value`, `gradientNorm`, `iterations`, `stopReason`, `trace`. Results/trace points own copies. Iterations count updates; comparison call counts measure objective/gradient evaluations during optimization only, excluding evaluation and plotting.

`StudentCsvLoader.load(Path)` returns `LoadedCohort(cohort,sourceRows,omittedEnrolled)`. `DataSplit.stratified` returns training/test cohorts. `Standardizer.fit` receives training features only. `Evaluation.evaluate` returns metrics without the training penalty; undefined metrics are shown as N/A.

The common `SyntheticCohort.Cohort` container is reused for real and transformed data. Its planted-weight metadata is meaningful only for the raw synthetic generator; real/transformed cohorts use zero placeholders and never present them as true model weights.

## Mathematical and data boundaries

- Convexity alone does not imply uniqueness; an unregularized intercept does not give a positive global strong-convexity constant merely from slope regularization.
- Logistic steps use the conservative bound `B = sum ||x_i||^2/(4m) + lambda`. A power-iteration estimate alone is not a certified upper bound.
- Convex Nesterov has an O(1/k^2) objective-gap bound for ordinary iterates with alpha<=1/L. The `2L` constant requires alpha=1/L; the general bound uses `2/alpha`. The supplied schedule is not the constant-momentum strongly convex variant.
- The gradient-norm stop used by the program differs from an objective-gap benchmark. Do not compare these counts to the proposal's relative-gap counts as if they used the same stopping rule.
- UCI academic features are end-of-semester measurements and the target is the eventual course outcome. Availability timestamps of financial flags need confirmation before any operational prediction.
- Excluding Enrolled selects a resolved-outcome population. One held-out split does not establish generalization to all enrolled students or other institutions.
- Feature terms explain fitted log-odds, not causes. Error-cost thresholds do not demonstrate that a call prevents dropout. Removing protected attributes does not establish fairness.
- Synthetic Bernoulli sampling and regularization do not exactly recover planted coefficients.

## Quick demo

Open the menu, load UCI with 1, compare with 5, then change costs with 7: missed-positive 9, unnecessary-outreach 1. The threshold falls from 0.5 to 0.1 and eligible held-out records increase from 232 to 558 without retraining. These costs are illustrative. Choose 8 for the quadratic paths, and 9 for all checks. Full transcripts are in `docs/evidence/`.
