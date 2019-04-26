(ns sampler.db
  (:require
   [cheshire.core :as json]
   [honeysql.core :as hsql]
   [clojure.string :as str]
   [clj-pg.pool :as pool]
   [clj-pg.honey :as pg]))

;; TODO make all the db funs look nice
;; and get rid of extra deps

(defn table-name [resource-type]
  (str "\"" (str/replace (str/lower-case (name resource-type)) #"\." "__")  "\""))

(defn *table-name [rt]
  (hsql/raw (table-name rt)))

(defn ->resource [{id :id st :status rt :resource_type ts :ts  txid :txid resource :resource :as row}]
  (when (and row (or resource {}))
    (merge resource
           (dissoc row :ts :id :status :txid :resource_type :resource)
           {:id id :resourceType rt
            :meta {:lastUpdated (.toString ts)
                   :versionId (str txid)
                   :tag [{:system "https://aidbox.app" :code st}]}})))

(def qv pg/query-value)
(def q pg/query)
(def qf pg/query-first)
(def raw hsql/raw)
(def fmt hsql/format)
(def exec pg/execute)

(defn truncate [db table]
  (exec db (str "DELETE FROM " (table-name table))))

(defn qr [& args]
  (->> (apply q args)
       (map ->resource)))

(defn qfr [& args]
  (when-let [res (first (apply q args))]
    (->resource res)))

(defn sn [x] (if (keyword? x) (name x) (str x)))

(defn h>>
  ([path]
   (h>> :resource path))
  ([col path]
   (assert col)
   (-> (str (name col) "#>>" "'{" (str/join "," (mapv sn path)) "}'")
       hsql/raw)))

(defn find [db {rt :resourceType id :id}]
  (->> {:select [:*]
        :from [(*table-name rt)]
        :where [:= :id id]}
       (pg/query-first db)
       ->resource))

(defonce ds (atom {}))

(defn connect [cfg]
  (let [{h :pghost p :pgport d :db u :pguser pw :pw :as cfg} cfg
        ds-id (str h ":" p)]
    (if-let [d (get @ds ds-id)]
      d
      (let [uri
            (clojure.core/format "jdbc:postgresql://%s:%s/%s?user=%s&password=%s&stringtype=unspecified&sslmode=%s"
                                 h p d u pw (get cfg :sslmode "disable"))
            pool-cfg
            {:idle-timeout       1000
             :minimum-idle       0
             :connection-timeout 15000
             :maximum-pool-size  2
             :connection-init-sql "select 1"
             :data-source.url uri}]
        (first (vals (swap! ds assoc ds-id {:datasource (pool/create-pool pool-cfg)})))))))

