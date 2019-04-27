(ns sampler.rest-test
  (:require
   [matcho.core :as matcho]
   [sampler.test-utils :as utils]
   [sampler.db :as db]
   [sampler.test-db :as tdb]
   [sampler.queries :as queries]
   [sampler.rest :as rest]
   [clojure.test :refer :all]))

(deftest $fetch
  (tdb/prepare!)

  (matcho/assert
   {:status 200
    :body
    [{:resourceType "Patient" :id "test-pt"}]}
   (utils/http :get "/Sample/$fetch"
               {:resourceType "Patient"
                :id "test-pt"
                :from "test-db"})))

(deftest $save
  (tdb/prepare!)

  (is (= 0 (db/qv utils/db {:select [:%count.*] :from [:sample]})))

  (def resp
    (utils/http :post "/Sample"
                {:resourceType "Patient"
                 :id "test-pt"
                 :from "test-db"}))

  (matcho/assert
   {:status 201
    :body {:sample-id string?}}
   resp)

  (is (= 1 (db/qv utils/db {:select [:%count.*] :from [:sample]})))

  (matcho/assert
   {:status 200
    :body [{:resourceType "Patient"}]}
   (utils/http :get (str "/Sample/" (get-in resp [:body :sample-id])))))

(deftest *get
  (tdb/prepare!)
  
  (matcho/assert
   {:status 404
    :body {:message #"not-found not found"}}
   (utils/http :get "/Sample/not-found")))

(deftest $query
  (tdb/prepare!)

  (def resp
    (utils/http :post "/Sample/$query"
                {:query {:select [["Patient" :rt] :id] :from [:patient]
                         :where [:= :id "test-pt"]}
                 :from "test-db"}))

  (matcho/assert
   {:status 201
    :body
    {:sample-id string?}}
   resp)

  (matcho/assert
   {:status 200
    :body
    ^:matcho/strict
    [{:resourceType "Patient" :id "test-pt"}]}
   (utils/http :get (str "/Sample/" (get-in resp [:body :sample-id])))))

(deftest post-connection
  (tdb/prepare!)

  (def conn
    {:name "new-db"
     :pghost "localhost"
     :pgport "6005"
     :pguser "postgres"
     :db "my-db"
     :opts {:sslmode "disable"}})

  (def resp (utils/http :post "/Connection" conn))

  (matcho/assert
   {:status 201
    :body {:id not-empty}}
   resp)

  (matcho/assert
   {:status 200
    :body conn}
   (utils/http :get (str "/Connection/" (get-in resp [:body :id])))))
