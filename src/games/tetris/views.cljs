(ns games.tetris.views
  (:require
   [re-frame.core :as rf]
   [games.tetris.subs :as tetris.subs]
   [games.views.util :as util]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell
  "Renders a cell as a div."
  ([c] (cell c {}))
  ([{:keys [falling occupied style] :as c} opts]
   (let [debug (:debug opts)
         width (if debug "80px" "20px")
         height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :border "#484848 solid 1px"}
       style)}
     (if debug
      (str c)
      "")])))

(defn preview-cell
  "Renders a cell as a div."
  [{:keys [preview style] :as c} opts]
  (let [debug (:debug opts)
        width (if debug "80px" "20px")
        height (if debug "120px" "20px")]
    ^{:key (str (random-uuid))}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :background (when preview "coral")
        :border (if preview
                  "black solid 1px"
                  ;;"gray solid 1px"
                  "transparent solid 1px")}
       style)}
     (if debug
      (str c)
      "")]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn matrix []
  (let [grid-data @(rf/subscribe [::tetris.subs/game-grid])]
    [:div
     {:class "nes-container is-dark"
      :style
      {:display "flex"
       :flex "1"
       :flex-direction "column"
       :justify-content "center"
       :align-items "center"}}
       ;;:border "1px solid red"}}
     (for [row grid-data]
        ^{:key (str (random-uuid))}
        [:div
         {:style
          {:display "flex"}}
           ;;:transform "rotateX(0deg) rotateY(0deg) rotateZ(0deg)"}}
         (for [cell-state row]
          (cell cell-state))])]))

(defn piece-preview [grid-data]
   [:div
    (for [row grid-data]
      ^{:key (str (random-uuid))}
      [:div
       {:style {:display "flex"}}
       (for [cell-state row]
        (preview-cell cell-state {:debug false}))])])

(defn display-label
  [{:keys [label]}]
  [:h3
   {:style
    {:opacity "0.9"
     :margin-bottom "12px"}}
   label])

(defn piece-preview-list
  ([] (piece-preview-list {}))
  ([{:keys [label piece-grids] :as metric}]
   [:div
    {:class "nes-container is-dark"
     :style
     {:text-align "center"}}
    [display-label metric]
    (for [g piece-grids]
      ^{:key (str (random-uuid))}
      [:div
       {:style
        {:display "flex"
         :justify-content "center"
         :margin-bottom "12px"}}
         ;;:border "1px solid red"}}
       (piece-preview g)])]))

(defn piece-previews []
  (let [preview-grids @(rf/subscribe [::tetris.subs/preview-grids])]
    (piece-preview-list {:label "Queue"
                         :piece-grids preview-grids})))

(defn allowed-pieces []
  (let [allowed-piece-grids @(rf/subscribe [::tetris.subs/allowed-piece-grids])]
    (piece-preview-list {:label "All pieces"
                         :piece-grids allowed-piece-grids})))

(defn held-piece []
  (let [held-grid @(rf/subscribe [::tetris.subs/held-grid])]
    (piece-preview-list {:label "Hold"
                         :piece-grids [held-grid]})))

(defn metric [{:keys [label value] :as metric}]
  [:div
   {:class "nes-container is-dark"
    :style
    {:display "flex"
     :flex "1"
     :text-align "center"
     :flex-direction "column"
     :justify-content "center"}}

   [display-label metric]
   [:h2
    {:style {:opacity "0.95"}}
    value]])

(defn score-panel []
  (let [score @(rf/subscribe [::tetris.subs/score])
        t @(rf/subscribe [::tetris.subs/time])
        level @(rf/subscribe [::tetris.subs/level])
        paused? @(rf/subscribe [::tetris.subs/paused?])]
    [:div.left-panel
     {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"}}
     [metric {:label "Score" :value score}]
     [metric {:label (if paused? "Time (Paused)" "Time")
              :value (str (util/with-precision 1 (/ t 1000)) "s")}]
     [metric {:label "Level" :value level}]]))

(defn piece-panel []
  [:div
    {:style
      {:display "flex"
       :flex "1"
       :flex-direction "column"}}
    [piece-previews]
    [held-piece]])
    ;;[allowed-pieces]]))

(def background-color "#441086")
     ;;:background "#5d08c7"

(defn page []
  [:div
   {:style
    {:height "100vh"
     :display "flex"
     :background
     (str "linear-gradient(135deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px),
     linear-gradient(225deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px)0 64px")
     :background-color background-color
     :background-size "64px 128px"
     :padding "24px"}}
   [score-panel]
   [matrix]
   [piece-panel]])
