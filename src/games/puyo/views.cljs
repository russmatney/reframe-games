(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.views.components :as components]
   [games.grid.core :as grid]
   [games.grid.views :as grid.views]
   [games.color :as color]
   [games.subs :as subs]))

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
  [grid game-opts]
  (let [spin?         @(rf/subscribe [::subs/game-db game-opts :spin-the-bottle?])
        pieces-played @(rf/subscribe [::subs/game-db game-opts :pieces-played])

        grid
        (cond-> grid
          spin?
          (grid/spin {:reverse-y? (contains? #{1 2 3} (mod pieces-played 6))
                      :reverse-x? (contains? #{2 3 4} (mod pieces-played 6))}))]
    [grid.views/matrix
     grid
     {:cell->style (partial cell->style game-opts)}]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Selectable game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-game
  "Intended as a selectable game (from a list)."
  []
  (let [game-opts {:name :puyo-select-game}
        grid      @(rf/subscribe [::subs/game-grid game-opts])
        game-opts @(rf/subscribe [::subs/game-opts game-opts])]
    [:div
     [matrix grid game-opts]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Debug game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn debug-game
  "Intended as a debug helper."
  []
  (let [game-opts {:name :puyo-debug-game}
        game-opts @(rf/subscribe [::subs/game-opts game-opts])
        grid      @(rf/subscribe [::subs/game-grid game-opts])]

    (components/page
      {:direction    :row
       :full-height? true}
      [grid.views/matrix
       grid
       {:->cell
        (fn [c]
          [:div
           {:style
            (merge
              (cell->style game-opts c)
              {:width  "180px"
               :border "1px solid white"})}
           (str c)])}])))


