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

(def mini-game-defaults
  {:name      :controls-mini-game
   :debug     false
   :no-walls? true})

;; Macro for defn that handles zero arity with defaults/merging?
(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [game-opts (merge mini-game-defaults game-opts)
         debug     (:debug game-opts)
         grid      @(rf/subscribe [::controls.subs/game-grid game-opts])]

     (rf/dispatch [::controls.events/start-game game-opts])
     (grid.views/matrix
       grid
       {:->cell
        (fn cell-component
          [{:keys [moveable? x y] :as cell}]
          ^{:key (str x y)}
          [:div
           {:style
            {:height     (if debug "148px" "48px")
             :width      (if debug "148px" "48px")
             :border     (if moveable? "1px solid white" "1px solid red")
             :background (if moveable? "green" "white")}}
           (if debug (str cell) "")])}))))


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
   [components/widget
    {:style {:width "100%"}
     :label "Controls"}]
   [mini-game]])
