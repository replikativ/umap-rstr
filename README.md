# umap-rstr

UMAP (Uniform Manifold Approximation and Projection) for Clojure, built on
[raster](../raster) — typed multiple dispatch with devirtualizing bytecode
compilation. The `.rstr` suffix marks the raster substrate (à la Julia's `.jl`).

A faithful port of `umap-learn` (Python+numba is the gold standard): cosine/
euclidean kNN → fuzzy simplicial set → spectral/random init → negative-sampled
SGD layout. The numeric kernels are `deftm` functions that JIT-compile to
primitive-speed JVM bytecode; the orchestration is plain Clojure.

## Status

Validated against `umap-learn` on MNIST-70k and Fashion-MNIST-70k (cosine):

| dataset | trust (rstr) | trust (umap-learn) | fit (rstr) | fit (umap-learn) |
|---------|--------------|--------------------|------------|------------------|
| MNIST-70k   | 0.951 | 0.950 | 83 s  | 109 s |
| Fashion-70k | 0.901 | 0.906 | 67 s  | 181 s |

Trustworthiness matches; wall-clock is faster (incl. cold JVM/JIT). See
`dev/` for the reproducible comparison harness.

## Usage

```clojure
(require '[umap.rstr :as umap])

;; X: flat row-major double[] (or float[]) of n*dim
(def result (umap/fit X n dim :k 15 :metric :cosine :init :auto :seed 42))
(:emb result)   ;; => double[n*2] embedding
```

Options: `:k` (neighbors, 15), `:out-dim` (2), `:n-epochs` (auto 500/200),
`:neg-rate` (5.0), `:gamma` (1.0), `:init` (`:auto`/`:spectral`/`:random`),
`:metric` (`:cosine`/`:euclidean`), `:seed` (42).

## Namespaces

- `umap.rstr` — public `fit` orchestrator
- `umap.rstr.layout` — negative-sampled SGD (the hot kernel)
- `umap.rstr.graph` — fuzzy simplicial set (smooth-knn-dist, membership, symmetrize)
- `umap.rstr.spectral` — spectral init (matrix-free Lanczos, disconnected-graph handling)

kNN / RP-trees / Tausworthe RNG live in raster (`raster.knn`,
`raster.spatial.*`, `raster.tausworthe`) since they're shared with clustering.

## Requirements

Runs on the Valhalla JDK (raster's `deftm` kernels use preview features). See
raster's README for JDK setup and run with the `:valhalla` alias.

```bash
clojure -M:valhalla:test          # run tests
```

## License

Same as raster.
