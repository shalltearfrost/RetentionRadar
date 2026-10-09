# Algorithm notes for the project foundation

**Team 4 — Uliana Matviienko, Andrii Moroz**  
**Topic: Gradient descent algorithm**

These notes explain the implemented foundation and provide material to develop in the official report. Report numbering and formatting remain provisional until the instructor's example is available.

## 1. Problem and motivation

Given a differentiable objective `f: R^d -> R`, optimization seeks a point that minimizes its value. Setting all partial derivatives to zero can produce a nonlinear system with no general closed-form solution. Gradient descent instead uses repeated first-order updates. The negative gradient points in the direction of the steepest local decrease in the Euclidean norm.

The implemented application minimizes logistic-regression loss, which measures how well a model fits binary labels. Each update adjusts every model coefficient using the same gradient vector. The application is useful for demonstrating the algorithm because logistic fitting normally requires numerical optimization. The project includes a reproducible synthetic demonstration and an attributed UCI extract. Its real-data task is eventual dropout versus graduation using information available by the end of semester one, rather than week-six prediction. See [Shalizi's logistic-regression notes](https://stat.cmu.edu/~cshalizi/dm/20/lectures/07/lecture-07.html).

## 2. Classical gradient descent

The vector update is

```text
x_(k+1) = x_k - alpha * gradient(f, x_k).
```

For a function with an L-Lipschitz continuous gradient, the descent lemma shows why step size matters:

```text
f(x_(k+1)) <= f(x_k) - alpha * (1 - alpha*L/2) * ||gradient(f,x_k)||^2.
```

Thus `0 < alpha < 2/L` gives a descent inequality when the gradient is nonzero. For the standard convex convergence bound, assume a minimizer exists, `f` is convex and L-smooth, and `0 < alpha <= 1/L`:

```text
f(x_k) - f(x*) <= ||x_0 - x*||^2 / (2*alpha*k), for k >= 1.
```

This is an `O(1/k)` objective-gap bound. It is not an unconditional guarantee for arbitrary functions or steps. The gradient-norm tolerance used by the program and an objective-gap accuracy are different stopping measures. See [Tibshirani, Gradient Descent, slide 16](https://www.stat.cmu.edu/~ryantibs/convexopt/lectures/grad-descent.pdf).

## 3. Pseudocode matching the implementation

```text
GRADIENT-DESCENT(f, x0, alpha, tolerance, maxUpdates)
    validate dimensions and configuration
    x = copy(x0)
    initialValue = f(x)
    updates = 0
    repeat
        value = f(x)
        if x/value is nonfinite or value exceeds the magnitude guard
            return DIVERGED
        g = gradient(f, x)
        if g or its norm is nonfinite
            return DIVERGED
        if norm(g) <= tolerance
            return x, CONVERGED, updates
        if updates == maxUpdates
            return x, MAX_ITERATIONS, updates
        x = x - alpha*g
        updates = updates + 1
```

The implementation evaluates a finite objective once per state and reuses it. It validates configuration and records optional history outside the core pseudocode above. The stopping test comes before each update; an optimal start requires zero updates. It also checks the point produced by the final allowed update. All coordinates use the same `g`. Recomputing the gradient after updating each coordinate would implement a different algorithm.

The divergence magnitude guard is a practical protection copied from the proposal: `value > 10^6 * (abs(initialValue) + 1)`. It is not a convergence theorem and is mainly suitable for the nonnegative losses demonstrated here. Other finite failures can still terminate at the iteration cap. The optimizer deliberately lets genuine objective exceptions remain visible.

## 4. Worked examples and complexity

For `f(x) = x^2`, `gradient = 2x` and `L = 2`. Starting at `x0 = 5`:

| Step alpha | First values | Behavior |
|---|---|---|
| 0.1 | 5, 4, 3.2, 2.56, 2.048 | Contracts by 0.8 |
| 0.9 | 5, -4, 3.2, -2.56 | Oscillates while contracting |
| 1.1 | 5, -6, 7.2, -8.64 | Magnitude grows |

The `--quadratic` example is `f(x,y) = 0.5*(x^2 + 100*y^2)`, with gradient `(x,100*y)`, known minimum `(0,0)`, and `L=100`. Starting at `(1,1)` with `alpha=0.01`, the second coordinate becomes zero after one update, while the first contracts by `0.99`.

The `--compare` example uses `f(x,y)=0.5*(x^2+1000*y^2)`, start `(1,1)`, `L=1000`, `mu=1`, common step `0.001` and gradient tolerance `1e-6`. GD converged in 13,809 updates with 13,810 objective/gradient calls; Nesterov converged in 3,302 updates with 6,603 objective/gradient calls. This is a comparison at a shared safe step, not the best individually tuned fixed step for classical GD. An ASCII contour plot uses actual sampled ordinary iterates, with separate markers for GD and Nesterov. The full transcript is `docs/evidence/comparison-output.txt`; additional evidence is indexed in `validation.md`.

Let `m` be the number of training records, `d` the coefficient dimension including bias, `k` the number of updates, and `s` the history stride. Dense logistic objective and gradient evaluation each take `Theta(m*d)`. Therefore either method's complete run takes `Theta((k+1)*m*d)` including the initial/final checks. The small quadratic costs `Theta(d)` per evaluation. Classical GD evaluates each ordinary state's objective and gradient once. Nesterov additionally evaluates the objective and gradient at a different look-ahead point, giving up to two evaluations per update cycle; it reuses the ordinary evaluation when the points coincide. This changes constants and actual call counts, while preserving the asymptotic order. `ComparisonReport` counts both kinds of evaluations separately for each method; update count alone is not equal computational work.

Optimizer working storage is `Theta(d)` with history off for either method. With history on, stored history is `Theta((k/s + 1)*d)`, including endpoints. The logistic objective separately owns `Theta(m*d)` data storage; this is not part of the optimizer-only space bound. With `h` held-out rows, score evaluation is `Theta(h*d)` and ranking takes `O(h log h)` time with `Theta(h)` ranking storage. AUC computation also sorts held-out scores.

## 5. Application objective

Let each feature vector begin with `x_i0 = 1`, let `y_i` be 0 or 1, and let `p_i = sigmoid(w dot x_i)`:

```text
J(w) = (1/m) * sum_i [softplus(w dot x_i) - y_i*(w dot x_i)]
       + (lambda/2) * sum_(j=1..d-1) w_j^2.

gradient_j J = (1/m) * sum_i (p_i - y_i)*x_ij
               + lambda*w_j for j >= 1.
gradient_0 J = (1/m) * sum_i (p_i - y_i).
```

The bias is not penalized. The cancellation of `sigmoid'(z) = p*(1-p)` in the cross-entropy derivative produces the simple prediction-error term `(p-y)`.

`LogisticObjective` evaluates the loss using stable sign/label branches equivalent to `max(z,0)-y*z+log1p(exp(-abs(z)))`. It does not take `log(0)` and does not form `exp(800)`. The sigmoid likewise uses separate positive and negative branches. All real-valued division uses floating-point arithmetic.

The Hessian is

```text
H(w) = X^T * diag(p_i*(1-p_i)) * X / m
       + lambda * diag(0,1,...,1).
```

It is positive semidefinite, establishing convexity. Since `p*(1-p) <= 1/4`, a conservative smoothness bound is

```text
B = sum_i ||x_i||^2 / (4*m) + lambda.
```

This follows by bounding `||X||_2^2` by `||X||_F^2`. The implementation includes the bias in the sum and rounds the bound upward. It uses `alpha=1/B`; it does not claim an exact largest Hessian eigenvalue, global `mu`, or condition number.

## 6. Implemented improvement: Nesterov acceleration

Nesterov acceleration compares first-order methods on a smooth convex objective. `NesterovGradientDescent.minimize` evaluates the update gradient at an extrapolated point and uses `beta_k = (k-1)/(k+2)` with `0 < alpha <= 1/L`. Java computes `(k-1.0)/(k+2.0)` to avoid integer division. Both optimizers share the interface `minimize(ObjectiveFunction objective, double[] initialPoint, OptimizerConfig config)`.

The conceptual iteration, with `x_k` the ordinary iterate and `y_k` the look-ahead point, is:

```text
NESTEROV-GRADIENT-DESCENT(f, x0, alpha, tolerance, maxUpdates)
    x = copy(x0); y = copy(x0); updates = 0
    repeat
        check finite x and f(x), and the magnitude guard
        g = gradient(f, x)
        check finite g and its norm
        record optional history at ordinary x
        if norm(g) <= tolerance
            return x, CONVERGED, updates
        if updates == maxUpdates
            return x, MAX_ITERATIONS, updates
        check finite y and f(y), and the magnitude guard
        h = g if y equals x, otherwise gradient(f, y)
        check finite h and its norm
        previous = x
        x = y - alpha*h
        updates = updates + 1
        beta = (updates - 1.0)/(updates + 2.0)
        y = x + beta*(x - previous)
```

The Java implementation caches each ordinary objective value and skips duplicate look-ahead evaluations. Stopping, the returned point, objective value, gradient norm and recorded history all refer to ordinary `x`. The look-ahead gradient drives the next update. This keeps the theory's ordinary iterate distinct from the extrapolated point. The final allowed update is checked, and an optimal start returns after zero updates. A temporary objective increase is permitted; only nonfinite arithmetic or the large magnitude guard is treated as divergence.

The theoretical bound for the ordinary iterate is

```text
f(x_k) - f(x*) <= 2*||x_0-x*||^2 / (alpha*(k+1)^2).
```

This gives `O(1/k^2)` for convex L-smooth objectives with an attained minimizer and the required step. At exactly `alpha=1/L`, the numerator can be written `2*L*||x_0-x*||^2`. The bound is about objective gap, not the program's gradient-norm stopping count. It does not assert monotonicity or a universal measured speedup. The strongly convex `O(sqrt(kappa)*log(1/epsilon))` statement belongs to a different, constant-momentum choice requiring a valid `mu`; it is not claimed for the implemented schedule. See [Su, Boyd and Candes, Equation (1) and Theorem 6](https://web.stanford.edu/~boyd/papers/pdf/ode_nest_grad.pdf).

## 7. Application and project risks

| Risk | Effect | Mitigation |
|---|---|---|
| Step too large or nonfinite arithmetic | Training fails | Safe bound for the demo; explicit stop reasons and checks |
| Highly unequal feature scales | Many updates | Use the implemented standardizer fitted only on training rows |
| Wrong feature observation times | Invalid early-warning claim | Confirm the prediction date and exclude unavailable features |
| Single-split results overgeneralized | Unsupported application claims | Report the actual selected population, seed and held-out metrics; further validation remains necessary |
| Missing official templates or rubric | Incomplete submission | Obtain Canvas materials before final formatting |
| Machine-specific paths or missing JDK | Project cannot be reproduced | Relative paths, JDK requirement and extraction/build check |
| AI-generated comments or unexplained code | Conflict with reported assignment rule | Human review and member-authored required comments |
| Scope or contributor availability changes | Work remains unfinished | Prioritized handoff and shared understanding of each part |

## 8. Real-data boundaries

The [UCI dataset](https://archive.ics.uci.edu/dataset/697/predict+students+dropout+and+academic+success) has 4,424 records and first-/second-semester academic variables. The [authors' descriptor](https://www.mdpi.com/2306-5729/7/11/146) describes academic variables as measured at the ends of the respective semesters and the target as the outcome at the end of normal course duration. The implemented application predicts eventual dropout versus graduation using first-semester information; it is not a proven week-six prediction of first-year withdrawal. Financial-status flag availability timestamps still need confirmation before operational use.

The bundled `data/students-first-semester.csv` is a modified, attributed extract under CC BY 4.0. It preserves all 4,424 records and row order, retaining only `Target` and seven features: admission grade, age at enrollment, tuition-fee status, scholarship status, and first-semester curricular units enrolled, approved and grade. Second-semester features, gender and nationality are excluded. Attribution, changes and provenance hashes are in `data/DATA-SOURCE.md` and `data/provenance.json`.

`StudentCsvLoader` accepts the exact semicolon-delimited column names, UTF-8 BOMs and quoted fields, rejects invalid schemas/data, maps `Dropout` to 1 and `Graduate` to 0, and omits `Enrolled`. IDs such as `UCI-row0003` identify original data-row positions. The real run retained 3,630 resolved outcomes after omitting 794 `Enrolled` records. Removing `Enrolled` creates a selected binary cohort; its probabilities cannot automatically be generalized to all currently enrolled students.

`DataSplit.stratified` uses a deterministic class-stratified 80/20 split with seed `202604`, producing 2,904 training and 726 test rows. Both subsets contain both classes; no row is shared and every retained row belongs to one subset. `Standardizer.fit` uses training rows only. The same means/scales transform training and test rows, the bias remains one, and a constant feature uses scale one. The model is fitted only on training rows, with `lambda=0.01`, zero initial weights, `B=2.01`, `alpha=1/B`, tolerance `1e-6` and a cap of 50,000 updates.

## 9. Measured held-out results

The recorded real-data comparison is in `docs/evidence/real-output.txt`:

| Method | Updates | Objective calls | Gradient calls | Final training objective |
|---|---:|---:|---:|---:|
| Classical GD | 865 | 866 | 866 | 0.358682941 |
| Nesterov | 375 | 749 | 749 | 0.358682941 |

Both stopped as `CONVERGED`. Both methods obtained the same following held-out metrics to the displayed precision at fixed threshold `0.5`, without using test rows during training:

| Metric | Recorded value |
|---|---:|
| Accuracy | 0.88430 |
| Precision | 0.93103 |
| Recall | 0.76056 |
| F1 | 0.83721 |
| ROC AUC | 0.91960 |
| Unpenalized log-loss | 0.31814 |
| Brier score | 0.09443 |
| TP / FP / TN / FN | 216 / 16 / 426 / 68 |

Training objective includes the slope penalty; held-out log-loss does not. AUC summarizes score ordering rather than a fixed decision threshold. These single-split metrics do not establish calibration, generalization across institutions, fairness or an intervention's effectiveness. Synthetic demonstration scores are separate from UCI evaluation. The source proposal's measurements were not copied into these results.

## 10. Costs, ranking and verification

Let `c_m` be the illustrative cost of a missed positive outcome and `c_o` the cost of an unnecessary outreach decision. Under the simple binary error-cost model, comparing expected losses gives:

```text
outreach loss = c_o*(1-p)
no-outreach loss = c_m*p
threshold = c_o/(c_o+c_m)
predict positive when p >= threshold
```

At equality, either decision has the same expected loss. The implementation uses `>=`. Menu option 7 changes the two positive finite costs and recomputes this threshold without changing weights or retraining. Increasing `c_m` lowers the threshold. Fixed model scores keep the same ordering, while the number passing the threshold may grow. The default costs are equal, giving threshold `0.5`. The cost interpretation presumes meaningful probabilities; the prototype has not established operational calibration or measured outreach benefits.

The watchlist ranks held-out source-row IDs, shows up to 25 qualifying records and displays two large signed terms `w_j*x_ij`. Inputs here are standardized features; these terms contribute to model log-odds. They are not causal explanations, authenticated student identities or evidence that a specific intervention helps.

The `--verify` mode currently runs 58 checks: 14 generic optimizer, 11 model, 15 acceleration/contour/counter, 4 data-pipeline groups, 12 evaluation and 2 cost-threshold checks. These cover numerical gradients, known minima, stopping behavior, momentum and its objective-gap bound on a known quadratic, CSV failures, split conservation, train-only standardization, metrics and threshold arithmetic. They complement the recorded demos; they do not replace the assignment's requested screenshots or final report. All Java source files currently have no comments, so the team must review the code and add the required comments in its own words before final submission.
