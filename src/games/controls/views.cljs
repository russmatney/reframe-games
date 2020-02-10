(ns games.controls.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.views.components :refer [widget]]
   [games.grid.views :as grid.views]
   [games.grid.core :as grid]
   [games.controls.core :as controls]
   [games.controls.events :as controls.events]
   [games.controls.subs :as controls.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page control display
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn display-control [[control {:keys [keys event label]}]]
  ^{:key control}
  [widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label}
   ^{:key (str keys)}
   [:p (string/join "," keys)]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry this up
(def background-color "#441086")
(def background
  (str "linear-gradient(135deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px), 64px"))


;; TODO create page component
(defn page []
  (let [controls @(rf/subscribe [::subs/controls])]
    [:div
     {:style
      {:height           "100vh"
       :width            "100vw"
       :display          "flex"
       :background       background
       :background-color background-color
       :background-size  "64px 128px"
       :padding          "24px"}}
     [:div
      {:style
       {:display        "flex"
        :flex-wrap      "wrap"
        :flex-direction "row"}}
      [widget
       {:style {:width "100%"}
        :label "Controls"}]
      (for [control controls]
        (display-control control))]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls-mini
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn mini [{:keys [controls]}]
  [widget
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
  {:name :controls-mini-game})

;; Macro for defn that handles zero arity with defaults/merging?
(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [game-opts (merge mini-game-defaults game-opts)
         debug     true
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
            {:height     "148px"
             :width      "148px"
             :border     (if moveable? "1px solid white" "1px solid red")
             :background (if moveable? "green" "white")}}
           (if debug (str cell) "")])}))))
