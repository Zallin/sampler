(ns sampler.queries
  (:require
   [sampler.db :as db]
   [cheshire.core :as json]))

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn insert-sample! [db resources]
  (->> {:insert-into (db/*table-name "sample")
        :values [{:id (uuid)
                  :resources (json/generate-string resources)}]
        :returning [:*]}
       db/fmt
       (db/q db)))

(defn insert-connection! [db v]
  (->> {:insert-into (db/*table-name "connection")
        :values [(merge {:id (uuid)} v)]
        :upsert {:on-conflict [:name]
                 :do-nothing {}}
        :returning [:*]}
       db/fmt
       (db/q db)))
