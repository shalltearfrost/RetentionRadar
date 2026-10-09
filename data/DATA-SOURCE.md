# Dataset source and preparation

Source: Realinho, V., Vieira Martins, M., Machado, J., and Baptista, L. (2021). **Predict Students' Dropout and Academic Success** [Dataset]. UCI Machine Learning Repository. DOI: [10.24432/C5MC89](https://doi.org/10.24432/C5MC89).

[Official dataset page](https://archive.ics.uci.edu/dataset/697/predict+students+dropout+and+academic+success). Original archive: [UCI download](https://archive.ics.uci.edu/static/public/697/predict%2Bstudents%2Bdropout%2Band%2Bacademic%2Bsuccess.zip). Downloaded and licence verified on 9 October 2026.

The dataset is licensed under **Creative Commons Attribution 4.0 International (CC BY 4.0)**. [Licence](https://creativecommons.org/licenses/by/4.0/) and [legal text](https://creativecommons.org/licenses/by/4.0/legalcode). Retain this attribution and indicate any further changes when redistributing.

## Changes made for this project

`students-first-semester.csv` is a modified extract of the original `data.csv`: seven selected feature columns plus `Target` are retained, with UTF-8 encoding and a semicolon separator. All original 4,424 records, row order, selected values and target labels are preserved. Every other column, including all second-semester features, was removed from the distributed CSV.

Selected features:

1. Admission grade.
2. Age at enrollment.
3. Tuition fees up to date.
4. Scholarship holder.
5. Curricular units 1st sem (enrolled).
6. Curricular units 1st sem (approved).
7. Curricular units 1st sem (grade).

The Java loader removes `Enrolled` (794 records), maps `Dropout` to 1 (1,421 records) and `Graduate` to 0 (2,209 records), and adds a bias of one. It therefore retains 3,630 resolved-outcome records. These counts must also be printed by the loader in a real run.

`UCI-rowNNNN` identifiers refer to the original data-row position, not a person's name or an authenticated university identifier. No personal names are invented.

The training pipeline uses a seeded stratified 80/20 split. Means and scales are fitted on training rows only. Training and test data are then transformed with those same parameters. The bias remains one.

## Time and population limitations

This educational prototype predicts eventual **dropout versus graduation using first-semester information**. Academic features are measured at the end of semester one; the outcomes refer to the end of normal course duration. See the [authors' dataset descriptor](https://www.mdpi.com/2306-5729/7/11/146). It is not a validated week-six or first-year-withdrawal predictor.

Availability timestamps of financial-status flags must be confirmed before any operational use. Excluding `Enrolled` selects a population with resolved outcomes; the fitted probabilities do not automatically apply to every currently enrolled student. A single held-out split is an educational evaluation and does not establish performance at other institutions, calibration, fairness or intervention effectiveness.

## Provenance

`provenance.json` records SHA-256 hashes of the downloaded original `data.csv` and the distributed extract, the selected columns, row count and target counts. The raw original archive and portable compiler are kept outside the deliverable. The CSV and this attribution allow the demo to run offline once Java is available.
