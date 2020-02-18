(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :as components]
   [games.views.util :as util]
   [games.grid.views :as grid.views]
   [games.controls.views :as controls.views]
   [games.subs :as subs]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
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
        grid      @(rf/subscribe [::tetris.subs/game-grid game-opts])
        game-opts @(rf/subscribe [::tetris.subs/game-opts game-opts])]
    [:div
     [matrix grid game-opts]]))

