(ns sampler.core
  (:require
   [sampler.db :as db]))

(defn resolve-attribute [resource path]
  (if (empty? path)
    [resource]
    (let [node (get resource (first path))]
      (if (vector? node)
        (mapcat #(resolve-attribute % (rest path)) node)
        (resolve-attribute node (rest path))))))

(defn build-graph [{rt :resourceType id :id tr :transform in :in :as ctx}]
  (println "building for " rt id)
  (let [attrs
        (db/qr in {:select [:*]
                   :from [:attribute]
                   :where [:and
                           [:= (db/h>> [:resource :id]) (name rt)]
                           [:= (db/h>> [:type :id]) "Reference"]]})
        tr-fn (get tr (keyword rt))
        resource (cond-> (db/find in {:resourceType rt :id id})
                   tr-fn tr-fn)
        links
        (->> attrs
             (map :path)
             (map (fn [p] (map keyword p)))
             (mapcat #(resolve-attribute resource %))
             (filter identity)
             (filter #(contains? % :id))
             (map #(select-keys % [:resourceType :id]))
             (map (fn [l] (build-graph (merge ctx l)))))]
    {:resourceType rt :id id :links links}))

(defn order [{rt :resourceType id :id links :links}]
  (if (empty? links)
    [{:resourceType rt :id id}]
    (let [links*
          (->> links
               (mapcat order)
               vec)]
      (conj links* {:resourceType rt :id id}))))

(defn fetch [{tr :transform :as arg}]
  (let [result
        (->> (build-graph arg)
             order
             distinct
             (map #(db/find (:in arg) %))
             (map (fn [resource]
                    (if-let [tr-fn (get tr (keyword (:resourceType resource)))]
                      (tr-fn resource)
                      resource))))]
    (assoc arg :resources result)))
