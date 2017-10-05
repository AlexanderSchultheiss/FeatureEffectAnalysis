# FeatureEffectAnalysis

An analysis plug-in for [KernelHaven](https://github.com/KernelHaven/KernelHaven).
This analysis calculates feature effect constraints, as proposed by [Nadi, Berger, Kästner, and Czarnecki](https://www.cs.cmu.edu/~ckaestne/pdf/tse15.pdf).

## Capabilities

* Detects presence conditions for variables (expressions for variable code parts): `net.ssehub.kernel_haven.feature_effects.PcFinder`
* Calculation of feature effect constraints based on detected presence conditions: `net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder`

## Usage

To use this analysis, set `analysis.class` to one of
* `net.ssehub.kernel_haven.feature_effects.PcFinder` to detect presence conditions
* `net.ssehub.kernel_haven.feature_effects.FeatureEffectFinder` to detect feature effect constraints
in the KernelHaven properties.

### Dependencies

This analysis has no additional dependencies other than KernelHaven.

### Configuration

In addition to the default ones, this analysis has the following configuration options in the KernelHaven properties:

| Key | Mandatory | Default | Example | Description |
|-----|-----------|---------|---------|-------------|
| `analysis.relevant_variables` | No | `.*` | `CONFIG_.*` | A Java regular expression to decide which variables are relevant variability variables. |
| `analysis.consider_vm_vars_only` | No | `false` | `true` | Specification whether only variables read by the variability model extractor should be treaded as relevant for the calculation feature effect constraints or also other variables. |

## License

This plug-in is licensed under Apache License 2.0.