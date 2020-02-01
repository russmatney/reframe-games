(ns games.tetris.views.controls
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.components :refer [widget display-label]]))

(defn display-control [[control keys]]
   ^{:key control}
   [widget
    {:on-click #(rf/dispatch (tetris.events/control->event control))
     :style
     {:flex "1 0 25%"}
     :label (tetris.db/control->label control)
     :children [^{:key (str keys)}
                [:p
                 (string/join "," keys)]]}])

(defn view []
  (let [controls @(rf/subscribe [::tetris.subs/controls])]
    [:div
     {:style
      {:display "flex"
       :flex-wrap "wrap"
       :flex-direction "row"
       :width "100%"
       :padding "24px"}}
     [widget
      {:style
       {:width "100%"}
       :label "Controls"}]
     (for [c-k controls]
       (display-control c-k))]))

(comment
  (rf/dispatch [::tetris.events/set-view :controls])
  (rf/dispatch [::tetris.events/set-view :game]))
