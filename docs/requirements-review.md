# Review of the downloaded design and conditions

## Topic fit

**The design fits the supplied main topic, “gradient descent algorithm.”** Classical batch gradient descent is the core algorithm. Logistic regression supplies a concrete optimization objective; Nesterov supplies a plausible improvement. The foundation keeps that structure and explicitly calls the optimizer from the main program.

The team is **Team 4: Uliana Matviienko and Andrii Moroz**, as supplied by the user. The proposal assumes four team members in its example division of work. That allocation has not been reused for the two named members.

## What was read

The last downloaded file was `RetentionRadar Project Design.html`; its project content was stored in the accompanying `RetentionRadar Project Design_files/_t.html`. The complete extracted design was read, including sections 00–12 and the closing benchmark note. A text copy is provided in `source/RetentionRadar-design-extracted.txt`.

This is a design proposal containing statements attributed to a brief and rubric. It does not contain the original assignment, assessment rubric, instructor's example report or abstract template. Therefore this review confirms topic fit and tracks the supplied conditions, while complete official compliance remains unverified.

## Conditions attributed to the assignment

| Condition in the proposal | Foundation status / action |
|---|---|
| Main program must call the implemented algorithm function | Done: driver/comparison modes and checks call `GradientDescent.minimize` and `NesterovGradientDescent.minimize` |
| No AI-generated comments; no comment on every line | No generated Java comments supplied; members must write required comments themselves |
| Use the instructor's example report and screenshot titles | Example missing; report numbering is provisional |
| Use the conference abstract template | Template missing; final abstract pending |
| Every member contributes across areas and can answer on any part | Human teamwork/rehearsal pending; no contribution claims made |
| Initial presentation includes project risks | Risk material prepared in algorithm notes; slides pending |
| Week 8 materials 24 hours before presentation | Claimed by proposal; confirm official timing and date |
| Week 15 materials 48 hours before presentation | Claimed by proposal; confirm official timing and date |

## Report and presentation checklist

| Item | Proposal's initial presentation/report | Proposal's final submission | Status here |
|---|---|---|---|
| Title page with course, team, members, date, clickable repository link | Required | Required | Identities and repository URL known; template/submission date pending |
| Introduction / problem | Required | Required | English technical notes drafted |
| Classical algorithm, convergence, complexity | Required | Required | Implemented and documented |
| Canonical pseudocode and worked example | Required | Required | Prepared, with exit semantics matching code |
| Algorithm implementation with comments and screenshots | Not marked | Required | Both optimizers implemented and checked; human comments/screenshots pending |
| Real-world application, objective, derivation and data | Required | Required | Attributed UCI extract, logistic objective, held-out evaluation and synthetic fallback implemented |
| Main program with header, comments, repository link and screenshots | Not marked | Required | English driver and repository URL available; final header/comments/screenshots pending |
| Improved algorithm, pseudocode and comparison/caveats | Not marked | Required | Nesterov implemented; measured comparison, call counters and quadratic ASCII contours supplied |
| Titled screenshots, “Part N.M. Screenshots: ...” | Tentative | Required | Await example and actual captures |
| Effort analysis supported by real commit history | Not marked | Required | Pending; no fabricated history |
| One-page conference abstract | Not marked | Required | Pending template and final results |
| References checked against sources | Required | Required | Focused primary references linked; final bibliography pending |

The initial presentation is described as 5–10 minutes covering the problem, classical algorithm, pseudocode, application and risks. The final presentation is described as about 10 minutes with a live demo and every member speaking. These claims need confirmation against the actual brief.

The proposal recommends an abstract of roughly 400–500 words with title/authors, motivation, method, measured results and conclusion. The instructor's template determines the actual length and format.

## Screenshot list to complete

- IntelliJ project structure and actual public repository showing genuine contributors.
- Imported cohort: row count, base rate and justified step-size information. Do not invent a global logistic `mu` or `kappa` to fill the original display.
- Classical training: updates, final objective and stop reason.
- Nesterov training and comparison with the same objective, initial point, step and tolerance; include objective/gradient calls and the implemented quadratic contours.
- Held-out evaluation at threshold 0.5, with confusion counts and metrics.
- Ranking and an actual threshold change through menu option 7, which does not retrain the model.
- Actual verification output with all checks passing.

Text transcripts supplied in `docs/evidence/` are real run evidence but are not substitutes for the requested screenshots.

## Design choices retained or deferred

The application name, Nesterov selection, plain Java 17, IntelliJ/no-build-framework approach, proposed package layout, project filename, top-25 ranking, scope tiers, ASCII contours and detailed schedule are design choices in the proposal, not independently verified instructor requirements.

Implemented now: project name `SDT202-Team4-GradientDescent`, driver `RetentionRadarApp.java`, plain Java 17, reusable objective interface, classical and Nesterov optimizers, stable logistic objective, synthetic fallback, sampled history, English console output, `Locale.ROOT` numeric formatting, attributed real data and CSV loader, stratified splitting, train-fitted standardization, held-out evaluation, top-25 ranking, cost threshold control, comparison counters and quadratic ASCII contours. The project contains 24 Java source files.

`PowerIteration` was replaced by a conservative global smoothness bound so the selected step has a justified upper bound rather than an eigenvalue estimate that might be too small. The user selected [shalltearfrost/RetentionRadar](https://github.com/shalltearfrost/RetentionRadar), which is currently private. Confirm its visibility/access against the actual brief. Final submission documents remain pending. The eventual report name proposed is `SDT202-project-team4.docx`.

The source recommends fixed-scope work in tiers, independent gradient derivations, cross-review, fresh-clone verification, early submission, and rehearsal. Its default four-person responsibility table and unconfirmed semester calendar need adaptation by the actual team. There is no confirmed deadline in this foundation.

## Corrections to carry forward

1. The UCI data do not establish a mid-first-semester or week-six prediction using semester-end academic features. Confirm timestamps and use an appropriate prediction date. [UCI #697](https://archive.ics.uci.edu/dataset/697/predict+students+dropout+and+academic+success), [dataset descriptor](https://www.mdpi.com/2306-5729/7/11/146).
2. Convexity does not guarantee uniqueness. The unregularized bias also prevents equating slope regularization with a positive global strong-convexity parameter.
3. A power-iteration estimate can underestimate the smoothness constant. The foundation instead uses the certified conservative logistic bound described in `algorithm-notes.md`.
4. The Nesterov objective-gap bound uses `2/alpha`; the `2L` version requires `alpha=1/L` and applies to the ordinary iterate. [Su, Boyd and Candes](https://web.stanford.edu/~boyd/papers/pdf/ode_nest_grad.pdf).
5. The source benchmark counts and speedups have no supplied run artifacts. They were not copied into this foundation's results. The supplied Java implementations were run separately; their actual transcripts and evaluation counts are in `docs/evidence/`. Update-count reductions alone are not runtime speedups, and the shared-step comparison does not individually optimize each method's step size.
6. A Bernoulli synthetic sample and regularization do not exactly recover planted weights. Verify gradients and known minima instead of demanding exact coefficient recovery.
7. Dropping `Enrolled` changes the modeled population. Do not call selected-cohort probabilities validated whole-cohort risk.
8. Naively combining cost-weighted training with the same original cost threshold would count the proposed asymmetry twice. A differently calibrated system needs its own derivation. The current model uses unweighted training; illustrative error costs affect only the implemented decision threshold. Those costs do not establish the benefit of an advisor intervention.
9. Feature contributions explain model log-odds, not causes or demonstrated intervention effectiveness.

## Final-submission decision

The main topic is suitable. This package implements both algorithms and an attributed real-data educational application, with 58 checks and recorded held-out results. The actual brief/templates, repository visibility/access confirmation, human-authored required comments, screenshots, report/abstract/slides, verified bibliography and genuine contribution history remain outstanding. These are required handoff tasks before claiming a completed final submission.

## Implemented real-data evidence

The distributed `data/students-first-semester.csv` retains seven selected features plus `Target`, all 4,424 original records and original row order. `data/DATA-SOURCE.md` supplies CC BY 4.0 attribution and the changes made; `data/provenance.json` records source/extract hashes. The runtime loader omits 794 `Enrolled` outcomes, retaining 3,630 resolved `Dropout`/`Graduate` outcomes. IDs refer to source row positions, not fabricated student names.

For the recorded run, a stratified 80/20 split with seed `202604` produced 2,904 training and 726 held-out rows. Standardization was fitted on training rows only. Both methods used zero starting weights, slope regularization `lambda=0.01`, the conservative bound `B=2.01`, step `alpha=1/B`, gradient tolerance `1e-6` and a cap of 50,000 updates.

| Method | Stop | Updates | Objective calls | Gradient calls |
|---|---|---:|---:|---:|
| Classical GD | CONVERGED | 865 | 866 | 866 |
| Nesterov | CONVERGED | 375 | 749 | 749 |

At the fixed evaluation threshold `0.5`, both methods obtained accuracy `0.88430`, precision `0.93103`, recall `0.76056`, F1 `0.83721` and AUC `0.91960`, with `TP=216`, `FP=16`, `TN=426`, `FN=68`. Unpenalized held-out log-loss was `0.31814` and Brier score `0.09443`, to the printed precision. These are results from one recorded split, not evidence of validated whole-cohort risk, calibration, fairness or intervention effectiveness. See `docs/evidence/real-output.txt`.

The independent quadratic comparison uses `f(x,y)=0.5*(x^2+1000*y^2)`, start `(1,1)`, common step `0.001` and gradient tolerance `1e-6`. GD required 13,809 updates and 13,810 gradient calls; Nesterov required 3,302 updates and 6,603 gradient calls. Sampled ordinary iterates are drawn on ASCII objective contours in `docs/evidence/comparison-output.txt`. These shared-step measurements replace, rather than reproduce, the source proposal's benchmark table.
