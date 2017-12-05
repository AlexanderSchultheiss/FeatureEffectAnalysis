# FeatureEffectAnalysis

An analysis plug-in for [KernelHaven](https://github.com/KernelHaven/KernelHaven).
This analysis calculates feature effect constraints, as proposed by [Nadi, Berger, Kästner, and Czarnecki](https://www.cs.cmu.edu/~ckaestne/pdf/tse15.pdf).

## Capabilities

* Detects presence conditions for variables (expressions for variable code parts): `net.ssehub.kernel_haven.feature_effects.PcFinder`
* Calculation of feature effect constraints based on detected presence conditions: `net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder`

## Usage

The following analysis elements can be used as part of a `ConfiguredPipelineAnalysis`:
* `net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder` to detect presence conditions
* `net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder` to detect feature effect constraints

Alternatively `analysis.class` can be set to one of
* `net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectAnalysis` to detect feature effect constraints

### Dependencies

This analysis has no additional dependencies other than KernelHaven.

### Configuration

In addition to the default ones, this analysis has the following configuration options in the KernelHaven properties:

| Key | Mandatory | Default | Example | Description |
|-----|-----------|---------|---------|-------------|
| `analysis.relevant_variables` | No | `.*` | `CONFIG_.*` | A Java regular expression to decide which variables are relevant variability variables. |
| `analysis.consider_vm_vars_only` | No | `false` | `true` | Specification whether only variables read by the variability model extractor should be treaded as relevant for the calculation feature effect constraints or also other variables. |
| `analysis.simplify_conditions` | No | `NO_SIMPLIFICATION` | Specification whether presence conditions and/or feature effect results should be simplified if possible, requires the CNF-Utils. Possible values are: `NO_SIMPLIFICATION` = no simplification of results, `PRESENCE_CONDITIONS` = simplification of presence conditions and feature effects, `FEATURE_EFFECTS` = simplification of feature effects only (presence conditions won't be simplified).|

## License

This plug-in is licensed under Apache License 2.0.