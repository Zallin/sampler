(ns sampler.migration
  (:require
   [sampler.db :as db]))

(defn migrate! [db]
  (db/exec db (slurp "src/sampler/migrations/init.sql")))
