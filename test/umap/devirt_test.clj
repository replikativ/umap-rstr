(ns umap.devirt-test
  "Gate against type-transport regressions on the LAZY-JIT path for the UMAP-layer
  hot kernels. A deftm's lazy-JIT code is exactly the walker's output; if a binding
  fails to type, consuming ops stay runtime-dispatched (~270ns vs ~4ns), silently
  5-70x slower in hot loops. Correctness tests can't see it, so we assert it
  directly. Companion to raster's devirtualization-test (NN-descent kernels)."
  (:require [clojure.test :refer [deftest testing is]]
            [raster.tooling.inspect :as inspect]
            [raster.compiler.pipeline :as pipeline]
            [umap.layout :as layout]
            [umap.graph :as graph]))

(def ^:private get-walked-body @#'pipeline/get-walked-body)

(defn- dispatch-counts [v dtype]
  (let [wb (get-walked-body v dtype)
        forms (if (vector? wb) wb [wb])]
    (reduce (fn [acc f]
              (merge-with + acc (select-keys (inspect/analyze-devirtualization f)
                                             [:devirtualized :dispatched])))
            {:devirtualized 0 :dispatched 0}
            forms)))

(deftest umap-hot-kernels-fully-devirtualized
  (testing "UMAP hot deftm kernels devirtualize 100% on the lazy-JIT walk path"
    (doseq [[v dtype] [[#'layout/optimize-layout-chunk! :double] ; parametric over emb
                       [#'layout/optimize-layout-chunk! :float]  ; f32: mixed double×float
                       [#'graph/smooth-knn-dist! nil]
                       [#'graph/membership-strengths! nil]]]
      (let [{:keys [devirtualized dispatched]} (dispatch-counts v dtype)]
        (is (zero? dispatched)
            (str v " has " dispatched " undevirtualized dispatch op(s) ("
                 devirtualized " devirtualized) — type-transport regression on the "
                 "lazy-JIT path; check that TC binding types reach the walker"))))))
