(ns sampler.config
  (:require
   [tools.config :as cfg]))

(def spec
  {:pghost {:required true}
   :pgport {:required true}
   :pguser {:required true}
   :pw {:required true}
   :db {:required true}})

(def cfg (cfg/*get spec))
