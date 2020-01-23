(ns toying.db
 (:require
  [toying.tetris :as tetris]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(def initial-db
  {::tetris/db
   {:game-state tetris/initial-game-state}})
