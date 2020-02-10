(ns games.controls.events
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [games.controls.db :as controls.db]))

(rf/reg-event-fx
  ::init
  (fn [db]
    {:dispatch-n
     [[::rp/add-keyboard-event-listener "keydown"]
      [::init-db]
      [::set]]}))

(rf/reg-event-db
  ::init-db
  (fn [db]
    (assoc db :controls-db (controls.db/initial-db))))

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
                     (print
                       "Alert! Unsupported key passed for event " event))
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


;; TODO update to support multiple events from seperate key bindings
;; to support sending keys to multiple keys at once
(rf/reg-event-fx
  ::set
  (fn [{:keys [db]} [_ controls]]
    (let [controls   (merge controls.db/global-controls (or controls {}))
          event-keys (controls->rp-event-keys controls)
          all-keys   (controls->rp-all-keys controls)]
      {:db (assoc db :controls controls)
       :dispatch
       [::rp/set-keydown-rules
        {:event-keys           event-keys
         :prevent-default-keys all-keys}]})))

