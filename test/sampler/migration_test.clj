(ns sampler.migration-test
  (:require
   [sampler.config :as cfg]
   [sampler.db :as db]
   [sampler.migration :as migration]
   [clojure.test :refer :all]))

(deftest client-inserted
  (def db (db/connect cfg/cfg))

  (db/truncate db :client)

  (is (= (->> {:select [:%count.*] :from [:client]}
              (db/qv db))
         0))

  (migration/migrate! db)

  (is (= (->> {:select [:%count.*] :from [:client]}
              (db/qv db))
         1))

  (migration/migrate! db)

  (is (= (->> {:select [:%count.*] :from [:client]}
              (db/qv db))
         1)))

