(ns toying.db
 (:require
  [toying.tetris.db :as tetris.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(def initial-db
  {::tetris.db/db tetris.db/initial-db})
