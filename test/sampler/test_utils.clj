(ns sampler.test-utils
  (:require
   [clojure.java.io :as io]
   [sampler.rest :as rest]
   [sampler.db :as db]
   [sampler.config :as cfg]
   [sampler.middleware :as mw]
   [clojure.string :as str]))

(def db (db/connect cfg/cfg))

(def handler
  (->> rest/stack
       (mw/add-db db)
       mw/format-edn
       mw/parse-params))

(defn ->qs [params]
  (->> params
       (map (fn [[k v]]
              (str (name k) "=" v)))
       (str/join "&")))

(defn http [& [meth uri params]]
  (let [tmpl
        {:request-method meth
         :uri uri
         ;; this is done to fake strange ring behaviour
         :body (io/input-stream (.getBytes ""))}
        req
        (cond-> tmpl
          (and params (= meth :get)) (assoc :query-string (->qs params))
          (and params (= meth :post)) (assoc :body (-> params pr-str .getBytes io/input-stream)))
        resp (handler req)]
    (cond-> resp
      (:body resp) (update :body read-string))))

