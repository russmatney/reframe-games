(ns games.views.components)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Label
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn display-label
  [label]
  [:h3
   {:style
    {:opacity "0.9"}}
   label])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Subhead
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;(defn display-subhead [subhead] subhead)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Widget
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn widget
  "A div with a 'vget-itude"
  ([{:keys [label subhead value style] :as args} & children]
   (let [args (-> args ;; quick and dirty - could clean up
                  (dissoc :label)
                  (dissoc :subhead)
                  (dissoc :value)
                  (dissoc :style))]
     [:div
      (merge
        {:class "nes-container is-dark"
         :style
         (merge
           {:display         "flex"
            :text-align      "center"
            :align-items     "center"
            :flex-direction  "column"
            :justify-content "center"}
           style)}
        args)

      (when label [display-label label])

      (when subhead subhead)

      (when value
        [:h2
         {:style {:margin-top "12px"
                  :opacity    "0.95"}}
         value])
      (when children
        (for [[i c] (map-indexed vector children)] ^{:key i} c))])))
