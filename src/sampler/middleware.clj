(ns sampler.middleware
  (:require
   [clojure.data.codec.base64 :as b64]
   [crypto.password.scrypt :as scrypt]
   [clojure.string :as str]
   [sampler.config :as cfg]
   [sampler.db :as db]
   [ring.util.codec :as codec]
   [clojure.walk :as walk]
   [matcho.core :as matcho]
   [sampler.spec :as spec]))

(defn resource! [handler]
  (fn [{inp :input-db {:keys [resourceType id]} :params :as req}]
    (let [exists?
          {:select [true]
           :from [(db/*table-name resourceType)]
           :where [:= :id id]}]
      (if (db/qv inp exists?)
        (handler req)
        {:status 404
         :body {:message (str "resource " resourceType "/" id " does not exist")}}))))

(defn connection! [handler]
  (fn [{:keys [params body] db :db :as req}]
    (let [nm (:from params)
          q
          {:select [:*]
           :from [:connection]
           :where [:= :name nm]}]
      (if-let [connection (db/qf db q)]
        (let [pw (get cfg/cfg (keyword nm))] 
          ;; TODO move this validation to config
          (assert pw (str "password is not provided for " nm))
          (handler (assoc req :input-db (->> {:pw pw} (merge connection) db/connect))))
        {:status 400
         :body {:message (str "Wrong connection name provided " nm)}}))))

(defn params! [handler]
  (fn [req]
    (if-let [query-spec
             (->> spec/params
                  (filter (fn [[k v]] (re-find k (:uri req))))
                  (map (fn [[k v]] v))
                  first)]
      (if (matcho/valid? query-spec (:params req))
        (handler (assoc req :spec query-spec))
        {:status 400
         :body {:message "Invalid input"
                :spec (pr-str query-spec)}})
      (handler req))))

(defn add-db [db handler]
  (fn [req]
    (handler (assoc req :db db))))

(defn format-edn [handler]
  (fn [req]
    (let [body* (when-let [b* (not-empty (slurp (:body req)))]
                  (read-string b*))
          req* (-> req
                   (update :params merge body*)
                   (assoc :body body*))
          response (handler req*)]
      (cond-> response
        (:body response) (update :body pr-str)))))

(defn parse-params [handler]
  (fn [req]
    (if-let [qs (:query-string req)]
      (let [params 
            (-> qs
                codec/form-decode
                walk/keywordize-keys)]
        (handler (update req :params merge params)))
      (handler req))))

(defn decode [s]
  (String. (b64/decode (.getBytes s))))

(def denied {:status 403 :body {:message "Access denied"}})

(defn basic-auth [handler]
  (fn [{db :db hs :headers :as req}]
    (if-let [encoded (and (get hs "authorization")
                          (->> (get hs "authorization") (re-find #"^Basic (.*)$") last))]
      (let [[key secret] (str/split (decode encoded) #":" 2)]
        (if (and key secret)
          (if-let [client (first (db/q db {:select [:*] :from [:client] :where [:= :key key]}))]
            (if (scrypt/check secret (:secret client))
              (handler req)
              denied)
            denied)
          denied))
      denied)))
