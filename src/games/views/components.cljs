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
  ([{:keys [label subhead value class style] :as opts} & children]
   (let [opts (-> opts ;; quick and dirty - could clean up
                  (dissoc :label)
                  (dissoc :subhead)
                  (dissoc :value)
                  (dissoc :class)
                  (dissoc :style))]
     [:div
      (merge
        {:class (str "widget nes-container is-dark " class)
         :style
         (merge
           {:display         "flex"
            :text-align      "center"
            :align-items     "center"
            :flex-direction  "column"
            :justify-content "center"}
           style)}
        opts)

      (when label
        ^{:key (str "display-label-" label)}
        [display-label label])

      (when subhead
        ^{:key (str "subhead-" label)}
        subhead)

      (when value
        ^{:key (str "value-" value)}
        [:h2
         {:style {:margin-top "12px"
                  :opacity    "0.95"}}
         value])
      (when children
        (for [[i c] (map-indexed vector children)] ^{:key i} c))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def background-color "#441086")
;;:background "#5d08c7"
(def background
  (str "linear-gradient(135deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px,
       black 22px, black 24px, transparent 24px, transparent 67px,
       black 67px, black 69px, transparent 69px), 64px"))
(def global-bg {:background       background
                :background-color background-color
                :background-size  "64px 128px"})

(defn page-column [children]
  (when children
    (for [[i c] (map-indexed vector children)]
      ^{:key i} c)))

(defn page-row [{:keys [full-height?]} children]
  [:div
   {:style
    {:display        "flex"
     :flex-wrap      "wrap"
     :flex-direction "row"
     :height         (when full-height? "100%")}}
   (when children
     (for [[i c] (map-indexed vector children)]
       ^{:key i} c))])

(defn page
  ([{:keys [class style empty-bg? direction header] :as opts} & children]
   (let [opts (dissoc opts
                      :style
                      :class
                      :header)]
     [:div
      (merge
        {:class (str "page " class)
         :style
         (merge
           {:height         "100vh"
            :width          "100vw"
            :display        "flex"
            :padding        "24px"
            :flex-direction "column"
            }
           (if empty-bg? {} global-bg)
           style)}
        opts)

      (when header header)

      (when (= :row direction) (page-row opts children))
      (when (or (not direction) (= :column direction)) (page-column children))])))
