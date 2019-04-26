(ns sampler.middleware-test
  (:require
   [sampler.rest :as rest]
   [sampler.test-db :as tdb]
   [sampler.test-utils :as utils]
   [cheshire.core :as json]
   [matcho.core :as matcho]
   [sampler.middleware :as mw]
   [clojure.test :refer :all]))

(deftest params-mw
  (tdb/prepare!)

  (matcho/assert
   {:status 400 :body {:message "Invalid input"}}
   (utils/http :get "/Sample/$fetch"
               {:resourceType "Patient"
                :from "test-db"}))

  (matcho/assert
   {:status 400 :body {:message "Invalid input"}}
   (utils/http :post "/Sample/$query"
               {:resourceType "Patient"
                :from "test-db"})))

(deftest connection-mw
  (tdb/prepare!)

  (matcho/assert
   {:status 400 :body {:message #"Wrong connection name"}}
   (utils/http :get "/Sample/$fetch"
               {:resourceType "Patient"
                :id "test-pt"
                :from "untest-db"})))

(deftest resource-mw
  (tdb/prepare!)

  (matcho/assert
   {:status 404
    :body {:message #"does not exist"}}
   (utils/http :get "/Sample/$fetch"
               {:resourceType "Patient"
                :id "untest-pt"
                :from "test-db"})))

(defn basic-req [header]
  (utils/http :get "/Sample/$fetch"
              {:resourceType "Patient"
               :id "untest-pt"
               :from "test-db"}
              {:Authorization header}))

(deftest basic-auth-mw
  (tdb/prepare!)

  (matcho/assert
   {:status 403
    :body {:message #"Access denied"}}
   (basic-req "huyasic"))

  (matcho/assert
   {:status 403}
   (basic-req "Basic 123"))

  (matcho/assert
   {:status 403}
   (basic-req (str "Basic " (utils/encode "notfound:123"))))

  (matcho/assert
   {:status 403}
   (basic-req (str "Basic " (utils/encode "test-client:123"))))

  (matcho/assert
   {:status 404}
   (basic-req (str "Basic " (utils/encode "test-client:1234")))))
