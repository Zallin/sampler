(ns sampler.core-test
  (:require
   [sampler.core :as sampler]
   [clojure.test :refer :all]))

(comment

  (sampler/fetch {:resourceType "ClaimResponse" :id "73246cc7-344c-4842-9419-c06e37c0b166"
                  :in {:host "localhost" :port "5461" :user "postgres" :pw "postgres" :db "proto"}})

  (sampler/save {:resourceType "ClaimResponse" :id "73246cc7-344c-4842-9419-c06e37c0b166"
                 :in {:host "localhost" :port "5461" :user "postgres" :pw "postgres" :db "proto"}})


  )

