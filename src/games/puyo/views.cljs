(ns games.puyo.views
  (:require
   [re-frame.core :as rf]
   [games.puyo.subs :as puyo.subs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cell->style
  [{:keys [falling color] :as c}]
  (let [background (case color
                     ;; TODO extract colors into db
                     :green "rgb(146,204,65)"
                     :red "#FE493C" ;;"#B564D4"
                     :blue "#209CEE" ;;#6ebff5
                     :yellow "rgb(247,213,29)"
                     "#323232")]
    {:background background}))

;; TODO dry up cell view?
(defn cell
  ([c] (cell c {}))
  ([{:keys [falling occupied x y] :as c} opts]
   (let [debug (:debug opts)
         debug true
         width (if debug "260px" "20px")
         height (if debug "120px" "20px")]
    ^{:key (str x y)}
    [:div
     {:style
      (merge
       {:max-width width
        :max-height height
        :width width
        :height height
        :border "#484848 solid 1px"}
       (cell->style c))}
     (if debug
      (str c)
      "")])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO dry up matrix view?
(defn matrix
  []
  (let [grid-data @(rf/subscribe [::puyo.subs/game-grid])]
    [:div
     (for [[i row] (map-indexed vector grid-data)]
       ^{:key (str i)}
       [:div
        {:style
         {:display "flex"}}
        (for [cell-state (seq row)]
          (cell cell-state))])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main page component
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")

(defn page []
  [:div
   {:style
    {:height "100vh"
     :width "100vw"
     :display "flex"
     :background-color background-color
     :background-size "64px 128px"
     :padding "24px"}}
   [matrix]])
