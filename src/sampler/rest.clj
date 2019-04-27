(ns sampler.rest
  (:require 
   [sampler.config :as cfg]
   [sampler.middleware :as mw]
   [sampler.queries :as queries]
   [sampler.core :as sampler]
   [sampler.db :as db]
   [sampler.spec :as spec]
   [sampler.migration :as migration]
   [route-map.core :as route-map]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.stacktrace :as trace]
   [clojure.string :as str]))

(defn fetch-sample
  [{inp :input-db
    {:keys [id] rt :resourceType} :params}]
  (let [arg
        {:resourceType rt
         :id id
         :in inp
         :transform {:Appointment (fn [app] (dissoc app :encounter))}}]
    {:status 200
     :body (->> arg
                sampler/fetch
                :resources)}))

(defn save-sample
  [{db :db
    inp :input-db
    {:keys [resourceType id]} :params}]
  (let [arg {:resourceType resourceType
             :id id
             :transform {:Appointment (fn [app] (dissoc app :encounter))}
             :in inp}]
    {:status 201
     :body (->> (sampler/fetch arg)
                :resources
                (queries/insert-sample! db)
                first
                :id
                (hash-map :sample-id))}))

(defn get-sample
  [{db :db
    {id :id} :params}]
  (let [q {:select [:resources]
           :from [:sample]
           :where [:= :id id]}]
    (if-let [res (->> (db/q db q)
                      first
                      :resources)]
      {:status 200
       :body res}
      {:status 404
       :body {:message (str "Sample with id " id " not found")}})))

(defn query-sample
  [{db :db
    inp :input-db
    {q :query} :params}]
  {:status 201
   :body 
   (->> (db/q inp q)
        (map (fn [{:keys [rt id] :as resp}]
               {:resourceType rt
                :id id
                :in inp
                :transform {:Appointment (fn [app] (dissoc app :encounter))}}))
        (map sampler/fetch)
        (mapcat :resources)
        (queries/insert-sample! db)
        first
        :id
        (hash-map :sample-id))})

(defn create-connection
  [{db :db spec :spec conn :params}]
  {:status 201
   :body
   (->> (select-keys conn (keys spec))
        (queries/insert-connection! db)
        first)})

(defn get-connection [{db :db {id :id} :params}]
  {:status 200
   :body (db/qf db
                {:select [:*]
                 :from [:connection]
                 :where [:= :id id]})})

(def routes
  {:interceptors [#'mw/basic-auth]

   "Connection"
   {:interceptors [#'mw/params!]
    :POST #'create-connection
    [:id] {:GET #'get-connection}}

   "Sample"
   {"$fetch"
    {:interceptors [#'mw/params! #'mw/connection! #'mw/resource!]
     :GET #'fetch-sample}

    "$query"
    {:interceptors [#'mw/params! #'mw/connection!]
     :POST #'query-sample}

    :POST
    {:interceptors [#'mw/params! #'mw/connection! #'mw/resource!]
     :. #'save-sample}

    [:id] {:GET #'get-sample}}})

(defn stack [{:keys [request-method uri] :as req}]
  (let [result (route-map/match [request-method uri] routes)
        mw (mapcat :interceptors (:parents result))]
    (if-let [handler (:match result)]
      (((apply comp mw) handler)
       (update req :params merge (:params result)))
      {:status 404
       :body "Page not found"})))

(defn start! [& args]
  (let [db (db/connect cfg/cfg)
        stack (->> stack
                   (mw/add-db db)
                   mw/format-edn
                   mw/parse-params
                   trace/wrap-stacktrace)]
    (migration/migrate! db)
    (jetty/run-jetty stack {:port 8088 :join? false})))

(defn restart! [srv]
  (.stop srv)
  (start!))

(defn -main [& args]
  (start!))

(comment

  (def server (start!))

  (def server (restart! server))

  (.stop server)
  
  )

