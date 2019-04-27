(ns sampler.migration
  (:require
   [crypto.password.scrypt :as scrypt]
   [sampler.queries :as queries]
   [sampler.db :as db]
   [clojure.java.io :as io]))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn migrate! [db]
  (->> "sampler/migrations/init.sql"
       io/resource
       slurp
       (db/exec db))
  (when (= 0 (db/qv db {:select [:%count.*] :from [:client]}))
    (let [secret (rand-str 12)
          client
          {:key "default"
           :secret (scrypt/encrypt secret)}]
      (println (str "Generated client with secret " secret))
      (queries/insert-client! db client))))

