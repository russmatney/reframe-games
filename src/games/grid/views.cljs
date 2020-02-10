(ns games.grid.views
  (:require
   [games.views.components :refer [widget]]))

(def border-color "#484848")

(defn cell
  [{c     :cell
    style :style
    debug :debug :as opts}]
  (let [{:keys [cell-comp]} (:game-opts opts)
        {:keys [x y]}       c
        style               (or style {})
        width               (if debug "260px" "40px")
        height              (if debug "120px" "40px")]
    ^{:key (str x y)}
    [:div
     {:style
      (merge
        {:max-width  width
         :max-height height
         :width      width
         :height     height
         :border     (str border-color " solid 1px")}
        style)}
     (if debug (str c) "")

     ^{:key (str x y)}
     (when cell-comp
       (cell-comp c))]))



(defn matrix
  "Displays the game matrix itself, using the passed grid-db and cell->style
  functions.

  `cell->style` merges the resulting map with the `cell` component's styles.

  `cell-comp` attaches a passed component as a child to the above cell component.

  Passing `->cell` allows you to write your own cell component, ignoring
  `cell->style` and `cell-comp`.
  "
  ([grid-db] [matrix grid-db {}])

  ([grid-db {:keys [cell->style ->cell] :as game-opts}]
   (let [grid (:grid grid-db)]
     [:div.matrix
      {:style
       {:display         "flex"
        :text-align      "center"
        :align-items     "center"
        :flex-direction  "column"
        :justify-content "center"
        :flex            "1"}}
      (for [[i row] (map-indexed vector grid)]
        ^{:key (str i)}
        [:div
         {:style
          {:display "flex"}}
         (for [{:keys [x y] :as c} row]
           (if ->cell
             ^{:key (str x y)}
             (->cell c)
             ^{:key (str x y)}
             [cell
              {:game-opts game-opts
               :cell      c
               :style     (if cell->style (cell->style c) {})}]))])])))

(defn piece-list
  "Displays a centered list of pieces below a label."
  ([] (piece-list {}))
  ([{:keys [label style piece-grids cell->style]}]
   [widget
    {:label label
     :style {:flex "1"}}
    [:div
     {:style
      (merge
        {:display         "flex"
         :flex-direction  "column"
         :justify-content "space-around"
         :width           "100%"}
        style)}
     (for [[i g] (map-indexed vector piece-grids)]
       ^{:key (str i)}
       [:div
        {:style
         {:display       "flex"
          :margin-bottom "12px"}}
        [matrix g {:cell->style cell->style}]])]]))
