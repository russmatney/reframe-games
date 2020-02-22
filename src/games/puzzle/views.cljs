(ns games.puzzle.views
  (:require
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.puzzle.subs :as puzzle.subs]
   [games.grid.views :as grid.views]
   [games.views.components :as components]
   [games.puzzle.events :as puzzle.events]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Select game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-game-cells
  [{:keys [placed? x y]}]
  ^{:key (str x y)}
  [:div
   {:style
    {:height     "48px"
     :width      "48px"
     :border     (if placed? "1px solid white" "1px solid black")
     :background (if placed? "green" "white")}}
   ""])

(defn select-game
  []
  (let [game-opts {:name :puzzle-select-game}
        grid      @(rf/subscribe [::subs/game-grid game-opts])]
    [grid.views/matrix grid
     {:->cell select-game-cells}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Classic Game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pieces
  [game-opts]
  (let [piece-grids @(rf/subscribe [::puzzle.subs/piece-grids game-opts])
        piece-grids (or piece-grids [])]
    [:div
     {:style {:flex "1"}}
     [:h3 "Pieces"]
     (for [[i g] (map-indexed vector piece-grids)]
       ^{:key i}
       [:div {:style {:margin-bottom "8px"}}
        [grid.views/matrix g]])]))

(defn board
  [game-opts]
  (let [grid @(rf/subscribe [::subs/game-grid game-opts])]
    [grid.views/matrix grid game-opts]))

(defn classic-game
  []
  (let [game-opts {:name :puzzle-classic-game}]
    [components/widget
     {:style {:flex-direction "row"}}
     ^{:key "board"}
     [board game-opts]
     ^{:key "pieces"}
     [pieces game-opts]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page
  []
  [components/page
   {:header [components/widget {:label "Puzzle"}]}
   ^{:key "classic"}
   [classic-game]
   ])
