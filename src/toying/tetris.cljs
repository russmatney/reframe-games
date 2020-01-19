(ns toying.tetris
 (:require
  [re-frame.core :as rf]
  [toying.events :as evts]))

(def grid-height 12)
(def grid-width 5)

(def initial-game-state
  {:grid
   (take grid-height (repeat (take grid-width (repeat {}))))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DB

(def initial-db
  {:game-state initial-game-state})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Events

(rf/reg-event-db
 ::init-db
 (fn [db]
   (assoc db ::tetris initial-db)))

;; this is not quite right, but meh for now
(rf/dispatch-sync [::init-db])

(rf/reg-event-fx
 ::game-tick
 (fn [{:keys [db]}]
   {:db db
    :timeout {:id ::tick
              :event [::game-tick]
              :time 1000}}))

(rf/dispatch-sync [::game-tick])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subs

(rf/reg-sub
 ::tetris-db
 (fn [db]
   (::tetris db)))

(rf/reg-sub
 ::game-state
 :<- [::tetris-db]
 (fn [db]
   (:game-state db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Views

(defn cell [{:keys [has-piece]}]
 ^{:key (str (random-uuid))}
 [:div
  {:style
   {:max-width "30px"
    :max-height "30px"
    :width "30px"
    :height "30px"
    :background (if has-piece "red" "powderblue")
    :border "black solid 1px"}}
  ""])

(defn stage []
  (let [game-state @(rf/subscribe [::game-state])]
   [:div
    (for [row (:grid game-state)]
      ^{:key (str (random-uuid))}
      [:div
       {:style {:display "flex"}}
       (for [cell-state row]
        (cell cell-state))])]))

