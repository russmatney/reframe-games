(ns games.tetris.views.controls
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.tetris.subs :as tetris.subs]
   [games.tetris.events :as tetris.events]
   [games.tetris.views.components :refer [widget display-label]]))

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
     (for [[control keys] controls]
       ^{:key control}
       [widget
        {:style
         {:flex "1"}
         :label (tetris.db/control->label control)
         :children [^{:key (str keys)}
                    [:p (string/join "," keys)]]}])]))

(comment
  (rf/dispatch [::tetris.events/set-view :controls])
  (rf/dispatch [::tetris.events/set-view :game]))
