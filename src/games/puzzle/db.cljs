(ns games.puzzle.db
  (:require
   [games.puzzle.controls :as puzzle.controls]
   [games.grid.core :as grid]
   [games.tetris.shapes :as tetris.shapes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pieces
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initial-pieces
  "Tetrominos for now."
  []
  (map :cells tetris.shapes/tetrominos))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; game db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def defaults {})

(defn game-db
  [game-opts]
  (let [{:keys [name game-grid] :as game-opts}
        (merge defaults game-opts)]
    {:name            name
     :game-opts       game-opts
     :init-event-name :games.puzzle.events/init-game
     :stop-event-name :games.puzzle.events/stop

     ;; game
     :game-grid
     (grid/build-grid
       (merge {:height 10 :width 6} game-grid))

     ;; puzzle-pieces
     :pieces (initial-pieces)

     ;; timer
     :time 0

     ;; controls
     :controls (puzzle.controls/initial game-opts)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; games dbs
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def classic-game-db
  (->
    {:name      :puzzle-classic-game
     :pages     #{:puzzle}
     :game-grid {:height 10 :width 6}}
    (game-db)))

(def select-game-db
  (-> {:name      :puzzle-select-game
       :pages     #{:select}
       :game-grid {:height 5 :width 5}}
      (game-db)))

(def game-dbs
  [select-game-db
   classic-game-db])

(def game-dbs-map
  (->> game-dbs
       (map (fn [game] [(-> game :name) game]))
       (into {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; db
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db
  {:games game-dbs-map})
