# Project validation and measured results

Verified on 9 October 2026 using Eclipse Temurin JDK 17 and `--release 17`. All 24 Java source files compile without external Java libraries.

## Verification

`RetentionRadarApp --verify` passes **58 checks**: 14 classical optimizer, 11 model/data-copy, 15 acceleration, 4 grouped data-pipeline, 12 evaluation and 2 cost-threshold checks. [Full transcript](evidence/verification-output.txt).

Tests cover known minima, gradient correctness, actual update counts, optimal starts/final allowed updates, look-ahead evaluation, floating-point momentum, ordinary-iterate convergence bounds, finite nonmonotone losses, numerical divergence, history/copies, CSV quoting/BOM/schema/filtering, split determinism/disjointness, train-only normalization, hand-calculated metrics/tied AUC, and stable large-logit arithmetic.

## Readability refactor verification

All 24 Java files were reviewed and refactored with descriptive English names, consistent formatting and cohesive methods. The driver separates data preparation, training, held-out evaluation, ranking and input handling. The optimizer update remains visible in each algorithm's main loop. Build/run scripts were expanded into explicit steps, and `code-guide.md` explains a suggested reading order.

All 58 checks still pass. The full stored outputs for verification, UCI, synthetic data, both quadratic modes and the cost-menu sequence were compared to the pre-refactor archive and match exactly, excluding the build banner. Numerical results, history output and call counts were preserved. Public optimizer/result and evaluation/comparison APIs were also checked against their prior compiled signatures.

## Reproducible configuration

- UCI 697: selected seven first-semester columns, 4,424 source rows, Enrolled omitted (794), retained Dropout/Graduate rows 3,630.
- Seeded stratified split: 2,904 training / 726 held-out rows, seed 202604.
- Standardization fitted on training rows only; bias remains one.
- Unweighted logistic objective; slope L2 lambda=0.01, intercept unpenalized; start all zeros.
- Conservative global bound B=2.01; common alpha=1/B=0.49751244.
- Ordinary-iterate gradient-norm tolerance 1e-6; maximum updates 50,000; history stride 25.
- Held-out classification threshold 0.5; held-out loss excludes the training penalty.

## Actual optimizer measurements

| Objective | Method | Updates | Objective calls | Gradient calls | Final objective | Stop |
|---|---|---:|---:|---:|---:|---|
| UCI standardized training | GD | 865 | 866 | 866 | 0.358682941 | CONVERGED |
| UCI standardized training | Nesterov | 375 | 749 | 749 | 0.358682941 | CONVERGED |
| Synthetic standardized training | GD | 64 | 65 | 65 | 0.587233792 | CONVERGED |
| Synthetic standardized training | Nesterov | 53 | 105 | 105 | 0.587233792 | CONVERGED |
| Quadratic, curvature 1,000 | GD | 13,809 | 13,810 | 13,810 | 4.99601610e-13 | CONVERGED |
| Quadratic, curvature 1,000 | Nesterov | 3,302 | 6,603 | 6,603 | 3.06677283e-13 | CONVERGED |

Counts are for optimization only, excluding evaluation/contour rendering. Nesterov performs extra ordinary/look-ahead checks, so fewer updates do not automatically mean less total work. The synthetic run actually uses more gradient calls under Nesterov despite fewer updates. These are deterministic examples, not timing benchmarks or evidence of a universal speedup.

The quadratic comparison uses `0.5*(x^2+1000*y^2)`, start (1,1), alpha=0.001, tolerance=1e-6 and cap=100,000. The synthetic comparison uses 600 generated rows, generation/split seed 202604, training 480 / test 120, lambda=0.01, alpha=1/1.01 and the same logistic tolerance/cap as above.

See [UCI transcript](evidence/real-output.txt), [synthetic transcript](evidence/synthetic-output.txt), and [quadratic comparison/contours](evidence/comparison-output.txt).

## Held-out UCI results

Both methods agree to the displayed precision:

| Metric | Value |
|---|---:|
| Test records | 726 |
| Accuracy | 0.88430 |
| Precision | 0.93103 |
| Recall | 0.76056 |
| F1 | 0.83721 |
| ROC AUC | 0.91960 |
| Unpenalized log-loss | 0.31814 |
| Brier score | 0.09443 |
| TP / FP / TN / FN | 216 / 16 / 426 / 68 |

This is one fixed selected-cohort split, not validated deployment performance. AUC uses logit rankings with half credit for ties; unpenalized loss is evaluated using stable arithmetic. No model or threshold was chosen by searching this test set.

## Menu and portability

The recorded menu sequence loaded UCI, trained/compared both methods, then changed illustrative costs from equal costs to missed-positive 9 / unnecessary-outreach 1. The threshold changed from 0.5 to 0.1 and eligible held-out records from 232 to 558 without retraining. [Menu transcript](evidence/cost-menu-output.txt).

The real-data mode also ran with uk/UA JVM locale settings and retained decimal-point formatting and identical displayed results. PowerShell scripts establish the project working directory for relative data paths, use consistent JDK selection and write compiler argument files as UTF-8 without a BOM.

The deliverable includes data attribution/provenance and IntelliJ descriptors. It excludes compiler files and generated build output. A newly packaged copy is checked by extraction/build in a directory with a space using Windows PowerShell 5.1. The initial portability check was an archive extraction test. The IntelliJ desktop UI was not opened during validation. The repository location is [shalltearfrost/RetentionRadar](https://github.com/shalltearfrost/RetentionRadar); it is currently private.

## GitHub clone verification

The initial project was uploaded to `main` in [commit e8f40fa](https://github.com/shalltearfrost/RetentionRadar/commit/e8f40fa0a8c7eb59035a0042ddc712f1ca67b2e2). A separate fresh clone from GitHub contained all 47 intended files. It compiled under Windows PowerShell 5.1 with JDK 17, passed all 58 checks, and ran the bundled real-data mode with the same 3,630 retained rows, optimizer counts and held-out metrics. Generated build output stayed ignored and the clone's working tree remained clean. Subsequent repository-link and verification updates affect documentation only.

## Remaining limits

Academic features are end-of-first-semester observations and target eventual course outcomes. Financial-flag availability requires confirmation for operational use. Excluding Enrolled affects the population. Scores are not established intervention benefits or causal explanations.

Final assignment compliance still needs the original brief/templates, member-authored comments/header, repository-access confirmation and genuine member contribution evidence, final screenshots, report/abstract and slides. The local Java 8 runtime is insufficient; recipients need JDK 17 or later.
