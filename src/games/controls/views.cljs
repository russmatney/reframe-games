(ns games.controls.views
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.subs :as subs]
   [games.views.components :refer [widget]]
   [games.grid.views :as grid.views]
   [games.grid.core :as grid]
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


(defn control-in-cell [[control {:keys [keys event label]}]]
  ^{:key control}
  [widget
   {:on-click #(rf/dispatch event)
    :style
    {:flex "1 0 25%"}
    :label    label}
   ^{:key (str keys)}
   [:p (string/join "," keys)]])

(defn relative [{x0 :x y0 :y} {:keys [x y] :as cell}]
  (-> cell
      (assoc :x (+ x0 x))
      (assoc :y (+ y0 y))))

(defn control->ec->cells [ctrl]
  (let [[control {:keys [label keys event]}] ctrl]
    (fn [ec]
      (println "ctrl->ec->cells: " ctrl)
      (println control)
      (seq (map (comp
                  #(assoc % :control ctrl)
                  #(relative ec %))
                (case control
                  :move-left [{:y -1} {:x -1} {} {:x 1}]
                  :main      [{:y -1} {:x 2} {} {:x 1}]
                  [{}]))))))

(defn controls->shapes [controls]
  (let [res (seq  (map control->ec->cells controls))]
    (print res)
    res))


(defn controls-add-pieces
  [grid controls]
  (let [ec->cell-fns (controls->shapes controls)]
    (reduce
      (fn [grid cell-fn]
        (grid/add-cells grid {:make-cells cell-fn}))
      grid
      ec->cell-fns)))

(def mini-game-defaults
  {:name      :default
   :game-grid {:entry-cell   {:x 0 :y 0}
               :height       5
               :phantom-rows 5
               :width        5}})

(defn mini-game
  "Intended as a div.
  Starts itself.

  Establishes sane defaults for a mini-player."
  ([] (mini-game {}))
  ([game-opts]
   (let [{:keys [cell-style]} (merge mini-game-defaults game-opts)
         controls             @(rf/subscribe [::subs/controls])
         grid                 @(rf/subscribe [::controls.subs/grid])
         grid                 (controls-add-pieces grid controls)]
     (println "mini-game grid")
     (println grid)
     (grid.views/matrix
       grid
       {:cell-comp
        (fn controls-cell-comp
          [{:keys [control] :as c}]
          (println "cell-comp arg - cell? " c)
          (let [[ctrl {:keys [label]}] control]
            ;; (println "cell-comp")
            ;; (println label)
            [:div
             {:style {:background "green"}}
             [:h3 ctrl]
             [:p label]]))

        :cell->style
        (fn [c]
          (merge
            (or cell-style {})
            (if (:style c)
              (:style c)
              {:background "#FEFEFE"})))}))))
