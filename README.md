# Machinometry

Factory instrumentation for modded Minecraft. Machinometry provides dedicated physical instruments instead of a universal all-in-one meter.

## Initial instrument set

- Item Flow Meter — exact committed item throughput and lifetime total.
- Fluid Flow Meter — exact committed fluid throughput and lifetime total.
- Energy Flow Meter — exact committed energy throughput and lifetime total.
- Inventory Gauge — current item count and capacity of the block behind the gauge.
- Tank Gauge — current fluid amount and capacity of the block behind the gauge.
- Energy Gauge — current stored energy and capacity of the block behind the gauge.

Flow meters are directional inline proxies: the front face is the output side and the opposite face is the input side. Gauges are state probes and inspect the adjacent block behind their display face. Throughput is measured from transfers that actually commit; it is not inferred from storage deltas.

## Supported targets

| Minecraft | Fabric | Forge | NeoForge | Java |
| --- | --- | --- | --- | --- |
| 1.20.1 | yes | yes | — | 17 |
| 1.21.1 | yes | — | yes | 21 |
| 26.1.2 | yes | — | yes | 25 |

The three Minecraft bands are intentionally isolated. 26.1.2 uses Minecraft's unobfuscated distribution and the post-26 NeoForge transfer API rather than carrying compatibility shims through older targets.

## Development

Each platform target is an independent Gradle build under `platforms/`. Shared loader-agnostic measurement logic lives under `shared/core`; common assets live under `shared/resources`.

On Windows, `scripts/build-all.ps1` builds the complete matrix using the appropriate JDK for each target.
