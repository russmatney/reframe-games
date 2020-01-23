(ns games.db
 (:require
  [games.tetris.db :as tetris.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(def initial-db
  {::tetris.db/db tetris.db/initial-db})
