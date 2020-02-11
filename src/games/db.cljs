(ns games.db
  (:require
   [games.tetris.db :as tetris.db]
   [games.puyo.db :as puyo.db]
   [games.controls.db :as controls.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-db
  []
  (merge
    controls.db/db
    {::tetris.db/db {}
     ::puyo.db/db   {}
     :current-page  :controls}))
