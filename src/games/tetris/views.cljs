(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.grid.views :as grid.views]
   [games.subs :as subs]
   [games.color :as color]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell->style [game-opts {:keys [color] :as c}]
  (merge
    (or (:cell-style game-opts) {})
    (if color
      {:background (color/cell->piece-color c)}
      {:background (color/cell->background c)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix
  "Returns the rows of cells."
  [grid game-opts]
  (grid.views/matrix
    grid
    {:cell->style (partial cell->style game-opts)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Select game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-game
  "Intended as a mini-game to be used when choosing a game to play."
  []
  (let [game-opts {:name :tetris-select-game}
        grid      @(rf/subscribe [::subs/game-grid game-opts])
        game-opts @(rf/subscribe [::subs/game-opts game-opts])]
    [:div
     [matrix grid game-opts]]))

