(ns games.controls.core
  (:require [games.controls.db :as controls.db]))

;; TODO explore control 'profiles' - premade and byo
;; supporting that might mean editable controls for free

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controls->re-pressed helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls->rp-event-keys
  "Converts the passed controls-db into a
  re-pressed `[[::event][kbd1][kbd2]]` list.
  "
  [controls]
  (into
    []
    (map
      (fn [[_ {:keys [event keys]}]]
        (into
          []
          (cons
            event
            (map (fn [k]
                   (when-not k
                     (print "Alert! Unsupported key passed for event " event))
                   [(controls.db/key-label->re-pressed-key k)])
                 keys))))
      controls)))

(defn controls->rp-all-keys
  "Converts the passed controls-db into a
  re-pressed `[[kbd1][kbd2]]` list.
  "
  [controls]
  (into []
        (mapcat
          (fn [[_ {:keys [keys]}]]
            (map controls.db/key-label->re-pressed-key keys))
          controls)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn can-player-move? [db] true)
(defn move-piece [db dir] db)
(defn rotate-piece [db] db)
