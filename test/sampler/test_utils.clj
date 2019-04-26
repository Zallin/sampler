(ns sampler.test-utils
  (:require
   [clojure.data.codec.base64 :as b64]
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

(defn ->ring-headers [headers]
  (->> headers
       (map (fn [[k v]] [(str/lower-case (name k)) (str v)]))
       (into {})))

(defn encode [s]
  (String. (b64/encode (.getBytes s))))

(defn http [& [meth uri params headers]]
  (let [tmpl
        {:request-method meth
         :uri uri
         ;; this is done to fake strange ring behaviour
         :body (io/input-stream (.getBytes ""))}
        req
        (cond-> tmpl
          (and params (= meth :get)) (assoc :query-string (->qs params))
          (and params (= meth :post)) (assoc :body (-> params pr-str .getBytes io/input-stream))
          (nil? headers) (assoc :headers {"authorization" (str "Basic " (encode "test-client:1234"))})
          headers (assoc :headers (->ring-headers headers)))
        resp (handler req)]
    (cond-> resp
      (:body resp) (update :body read-string))))
