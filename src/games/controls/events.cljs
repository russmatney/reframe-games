(ns games.controls.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [games.controls.db :as controls.db]))

(rf/reg-event-fx
  ::init
  (fn [_] {:dispatch [::rp/add-keyboard-event-listener "keydown"]}))

(defn controls->rp-event-keys
  "Converts the passed controls-db into a
  re-pressed `[[::event][kbd1][kbd2]]` list.
  "
  [controls-db]
  (into []
        (map
          (fn [[_ {:keys [event keys]}]]
            (into []
                  (cons
                    event
                    (map (fn [k]
                           [(controls.db/key-label->re-pressed-key k)])
                         keys))))
          controls-db)))

(defn controls->rp-all-keys
  "Converts the passed controls-db into a
  re-pressed `[[kbd1][kbd2]]` list.
  "
  [controls-db]
  (into []
        (mapcat
          (fn [[_ {:keys [keys]}]]
            (map controls.db/key-label->re-pressed-key keys))
          controls-db)))

(rf/reg-event-fx
  ::set
  (fn [_ [_ controls-db]]
    (let [event-keys (controls->rp-event-keys controls-db)
          all-keys   (controls->rp-all-keys controls-db)]
      {:dispatch
       [::rp/set-keydown-rules
        {:event-keys           event-keys
         :prevent-default-keys all-keys}]})))

