(ns games.db
  (:require
   [games.tetris.db :as tetris.db]
   [games.puyo.db :as puyo.db]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-db
  []
  {::tetris.db/db {}
   ::puyo.db/db   {}
   :controls      {}
   :current-page  nil
   :default-page  :controls})
