(ns tinhat.util
  (:require [clojure.string :as str]))

(defn cap
  [input-string]
  (str/capitalize input-string))