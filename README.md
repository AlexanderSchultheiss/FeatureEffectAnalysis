# FeatureEffectAnalysis

![Build Status](http://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=KernelHaven_FeatureEffectAnalysis)

An analysis plugin for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

Analysis components for calculating presence condtions and feature effects as proposed by [Nadi, Berger, Kästner, and Czarnecki](https://www.cs.cmu.edu/~ckaestne/pdf/tse15.pdf).

## Usage

Place [`FeatureEffectAnalysis.jar`](https://jenkins.sse.uni-hildesheim.de/view/KernelHaven/job/KernelHaven_FeatureEffectAnalysis/lastSuccessfulBuild/artifact/build/jar/FeatureEffectAnalysis.jar) in the plugins folder of KernelHaven.

The following analysis components can be used as part of a `ConfiguredPipelineAnalysis`:
* `net.ssehub.kernel_haven.fe_analysis.pcs.PcFinder` to calculate presence conditions for variables
* `net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectFinder` to calculate feature effects based on presence conditions for variables
* `net.ssehub.kernel_haven.fe_analysis.fes.FeAggregator` to aggregate feature effects for variables created in NonBooleanPreparation
* `net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectReader` to read feature effects from a file
* `net.ssehub.kernel_haven.fe_analysis.config_relevancy.ConfigRelevancyChecker` to check a given product configuration against feature effects

Alternatively `analysis.class` can be set to one of
* `net.ssehub.kernel_haven.fe_analysis.fes.FeatureEffectAnalysis` to detect feature effect constraints

## Dependencies

This plugin has no additional dependencies other than KernelHaven. However, it optionally utilizes [CnfUtils](https://github.com/KernelHaven/CnfUtils) if it is avaiable.

## License

This plugin is licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).
