(ns sampler.test-values)

(def pt
  {:resourceType "Patient"
   :id "test-pt"
   :gender "male"
   :birthDate "1980-01-01"
   :name [{:given ["Hulio"] :family "Iglesias"}]})

(def attrs
  [{:resourceType "Attribute",
    :id "Patient.birthDate",
    :path ["birthDate"],
    :type {:id "date", :resourceType "Entity"},
    :resource {:id "Patient", :resourceType "Entity"}}

   {:resourceType "Attribute",
    :id "Patient.id",
    :path ["id"],
    :type {:id "id", :resourceType "Entity"},
    :resource {:id "Patient", :resourceType "Entity"}}

   {:resourceType "Attribute",
    :id "Patient.gender",
    :path ["gender"],
    :type {:id "code", :resourceType "Entity"},
    :resource {:id "Patient", :resourceType "Entity"}}

   {:resourceType "Attribute",
    :id "Patient.name",
    :path ["name"],
    :type {:id "HumanName", :resourceType "Entity"},
    :resource {:id "Patient", :resourceType "Entity"}}])

