(ns games.controls.core)

(defn ->id
  "Converts a passed id and game-opts to a viable control-id."
  [id game-opts]
  (let [ns (namespace ::x)
        id (str (name id) "-" (name (:name game-opts)))]
    (keyword ns id)))

(comment
  (->id :move-left {:name :my-game}))

;; TODO explore control 'profiles' - premade and byo
;; supporting that might mean editable controls for free

