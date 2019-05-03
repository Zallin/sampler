(ns sampler.test-db
  (:require
   [crypto.password.scrypt :as scrypt]
   [cheshire.core :as json]
   [sampler.migration :as migration]
   [sampler.db :as db]
   [sampler.queries :as queries]
   [sampler.spec :as spec]
   [sampler.config :as cfg]
   [sampler.test-values :as values]
   [clojure.java.io :as io]))

(defn ->table-row [resource]
  {:id (get resource :id)
   :txid 0
   :status "created"
   :resource (json/generate-string (dissoc resource :id :resourceType))})

(defn prepare-data! [test-db]
  (let [attr-q
        {:insert-into (db/raw (db/table-name "attribute"))
         :values 
         (reduce (fn [acc attr]
                   (conj acc (->table-row attr)))
                 []
                 values/attrs)
         :returning [:*]}
        pt-q 
        {:insert-into (db/raw (db/table-name "patient"))
         :values [(->table-row values/pt)]
         :returning [:*]}]
    (db/truncate test-db :attribute)
    (db/q test-db attr-q)

    (db/truncate test-db :patient)
    (db/q test-db pt-q)))

(defn prepare! []
  (let [sampler-db (db/connect cfg/cfg)
        conn {:name "test-db"
              :pghost "localhost"
              :pgport "6004"
              :pguser "postgres"
              :db "postgres"}
        test-db (db/connect (assoc conn :pw (:test-db cfg/cfg)))
        client {:key "test-client"
                :secret (scrypt/encrypt "1234")}]
    (db/truncate sampler-db :connection)
    (migration/migrate! sampler-db)
    (queries/insert-connection! sampler-db conn)
    (db/truncate sampler-db :sample)
    (db/truncate sampler-db :client)
    (queries/insert-client! sampler-db client)

    (db/exec test-db (slurp (io/resource "sampler/test_migration.sql")))
    (prepare-data! test-db)))

