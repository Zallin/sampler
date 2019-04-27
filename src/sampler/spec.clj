(ns sampler.spec)

;; add methods here as well
(def params
  {#"/Sample/\$fetch.*"
   {:resourceType string?
    :id string?
    :from string?}
   #"/Sample/\$query.*"
   {:query #(or (map? %) (string? %))
    :from string?}
   #"/Sample/?$"
   {:resourceType string?
    :id string?
    :from string?}
   #"/Connection$"
   {:name string?
    :pghost string?
    :pgport string?
    :pguser string?
    :db string?
    :opts #(if % (map? %) true)}})
