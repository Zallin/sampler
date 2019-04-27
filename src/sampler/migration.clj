(ns sampler.migration
  (:require
   [sampler.db :as db]
   [clojure.java.io :as io]))

(defn migrate! [db]
  (->> "sampler/migrations/init.sql"
       io/resource
       slurp
       (db/exec db)))
