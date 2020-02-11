(ns games.controls.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.views.components :as components]
   [games.grid.views :as grid.views]
   [games.controls.events :as controls.events]
   [games.controls.subs :as controls.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page control display
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn display-control [[control {:keys [keys event label]}]]
  ^{:key control}
  [components/widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label}
   ^{:key (str keys)}
   [:p (string/join "," keys)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini [{:keys [controls]}]
  [components/widget
   {:style
    {:padding "0.9rem"
     :flex    "1"}}
   (doall
     (for [ctr controls]
       (let [{:keys [label event keys]}
             @(rf/subscribe [::subs/controls-for ctr])]
         (when (and keys event)
           ^{:key label}
           [:p
            {:style    {:margin-bottom "0.3rem"}
             :on-click #(rf/dispatch event)}
            (str label " (" (first keys) ")")]))))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini-game-cells
  [{:keys [moveable? x y]}]
  ^{:key (str x y)}
  [:div
   {:style
    {:height     "48px"
     :width      "48px"
     :border     (if moveable? "1px solid white" "1px solid black")
     :background (if moveable? "green" "white")}}
   ""])

(def mini-game-defaults
  {:name      :controls-mini-game
   :debug?    false
   :no-walls? true})

;; Macro for defn that handles zero arity with defaults/merging?
(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player.

  Controls mini game is a useful debugger and sandbox -
  Click the anchor to toggle debugging.
  "
  ([] (mini-game {}))
  ([game-opts]
   (let [game-opts (merge mini-game-defaults game-opts)
         grid      @(rf/subscribe [::controls.subs/game-grid game-opts])]

     (rf/dispatch [::controls.events/start-game game-opts])
     (grid.views/matrix
       grid
       {:->cell mini-game-cells}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page Game
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn page-game-cells
  [{:keys [moveable? x y anchor?] :as cell} {:keys [debug?] :as game-opts}]
  ^{:key (str x y)}
  [:div
   {:on-click
    (when anchor?
      #(rf/dispatch [::controls.events/toggle-debug game-opts]))
    :style
    {:height     (if debug? "148px" "48px")
     :width      (if debug? "148px" "48px")
     :border     (if moveable? "1px solid white" "1px solid red")
     :background (cond
                   anchor? "blue"
                   moveable? "green"
                   :else     "white")}}
   (if debug? (str cell) "")])

(def page-game-defaults
  {:name      :controls-page-game
   :debug?    true
   :no-walls? true})

(defn page-game
  "Intended as a full page.
  Useful as a debugger and sandbox, for implementing fancy features.
  Click the anchor cell to toggle `debug`.
  "
  ([] (page-game {}))
  ([game-opts]
   (let [game-opts (merge page-game-defaults game-opts)
         grid      @(rf/subscribe [::controls.subs/game-grid game-opts])
         game-opts @(rf/subscribe [::controls.subs/game-opts game-opts])]

     (rf/dispatch [::controls.events/start-game game-opts])

     (grid.views/matrix grid {:->cell #(page-game-cells % game-opts)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Pages
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn old-page []
  (let [controls @(rf/subscribe [::subs/controls])]
    [components/page {:direction    :row
                      :full-height? true}
     ^{:key "controls!"}
     [components/widget
      {:style {:width "100%"}
       :label "Controls"}]
     (for [control controls]
       ^{:key control}
       (display-control control))]))

(defn page []
  [components/page
   {:direction    :row
    :full-height? true}
   ^{:key "controls-header"}
   [components/widget
    {:style {:width "100%"}
     :label "Controls"}]
   ^{:key "controls-game"}
   [page-game]])
