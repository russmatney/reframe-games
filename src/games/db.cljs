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
  (let [game-db-merge-keys [:games]
        game-dbs           [puyo.db/db
                            tetris.db/db
                            controls.db/db]
        initial-db
        { ;; NOTE also the initial page
         :current-page :select
         ;; initial controls
         :controls     controls.db/global-controls}]

    ;; merge game-dbs keys
    ;; NOTE this is NOT a deep merge - matching keys will overwrite
    (reduce
      (fn [db merge-key]
        (let [merged-map
              (merge (into {} (map merge-key game-dbs)))]
          (assoc db merge-key merged-map)))
      initial-db game-db-merge-keys)))


(comment
  (keys (:games (initial-db))))
