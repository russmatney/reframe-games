(ns games.controls.re-pressed
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key Data and maps
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def key-label->re-pressed-key
  "Maps a 'nice' string to a re-pressed key with keyCode."
  {
   "backspace" {:keyCode 8}
   "tab"       {:keyCode 9}
   "enter"     {:keyCode 13}

   "space"    {:keyCode 32}
   "pageup"   {:keyCode 33}
   "pagedown" {:keyCode 34}
   "end"      {:keyCode 35}
   "home"     {:keyCode 36}
   "left"     {:keyCode 37}
   "up"       {:keyCode 38}
   "right"    {:keyCode 39}
   "down"     {:keyCode 40}
   "delete"   {:keyCode 46}

   "a" {:keyCode 65}
   "b" {:keyCode 66}
   "c" {:keyCode 67}
   "d" {:keyCode 68}
   "e" {:keyCode 69}
   "f" {:keyCode 70}
   "g" {:keyCode 71}
   "h" {:keyCode 72}
   "i" {:keyCode 73}
   "j" {:keyCode 74}
   "k" {:keyCode 75}
   "l" {:keyCode 76}
   "m" {:keyCode 77}
   "n" {:keyCode 78}
   "o" {:keyCode 79}
   "p" {:keyCode 80}
   "q" {:keyCode 81}
   ;; "r" {:keyCode 82} ;; TODO reenable - right now this clobbers ctrl+r
   "s" {:keyCode 83}
   "t" {:keyCode 84}
   "u" {:keyCode 85}
   "v" {:keyCode 86}
   "w" {:keyCode 87}
   "x" {:keyCode 88}
   "y" {:keyCode 89}
   "z" {:keyCode 90}

   ";"  {:keyCode 186}
   ":"  {:keyCode 186}
   "+"  {:keyCode 187}
   "="  {:keyCode 187}
   ","  {:keyCode 188}
   "<"  {:keyCode 188}
   "-"  {:keyCode 189}
   "_"  {:keyCode 189}
   "."  {:keyCode 190}
   ">"  {:keyCode 190}
   "?"  {:keyCode 191}
   "/"  {:keyCode 191}
   "`"  {:keyCode 192}
   "~"  {:keyCode 192}
   "["  {:keyCode 219}
   "{"  {:keyCode 219}
   "\\" {:keyCode 220}
   "|"  {:keyCode 220}
   "]"  {:keyCode 221}
   "}"  {:keyCode 221}
   "'"  {:keyCode 222}
   "\"" {:keyCode 222}
   })

(def supported-keys (set (keys key-label->re-pressed-key)))


(defn str-key->event-name [id]
  (keyword :games.controls.key-press id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transforms over key data
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ->rp-event
  "Only supports one key press per event.
  Returns [event[kbd]], assumes passed `:event` is already a vector."
  [{:keys [event kbd]}]
  [event [kbd]])

(def rp-event-keys
  "Converts the supported keys into a re-pressed `[[::event][kbd1][kbd2]]` list."
  (into [] (map
             (fn [[str-key rp-key]]
               (->rp-event {:kbd   rp-key
                            :event [(str-key->event-name str-key)]}))
             key-label->re-pressed-key)))

(def rp-all-keys
  "Converts the passed controls-db into a re-pressed `[kbd1 kbd2]` list."
  (into []
        (vals key-label->re-pressed-key)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key-press listener events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Registers each listener with re-pressed's listener
(rf/reg-event-fx
  ::register-key-listeners
  (fn [_cofx _evt]
    {:dispatch
     [::rp/set-keydown-rules
      {:event-keys           rp-event-keys
       :prevent-default-keys rp-all-keys}]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Key-press handler and registration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn key-press-handler
  "Dispatches registered events when the passed key is pressed.
  Selects events to dispatch using the db's :controls-by-key map.
  "
  [str-key cofx _event]
  (let [controls (get-in cofx [:db :controls-by-key str-key])]
    (println
      (str str-key " pressed, dispatching " (count controls) " events"))
    {:dispatch-n (map :event controls)}))

(defn register-dispatcher
  [[str-key _]]
  (rf/reg-event-fx
    (str-key->event-name str-key)
    (partial key-press-handler str-key)))

(defn register-dispatchers
  "Registers an internal (games) event for every key in `supported-keys`"
  []
  (doall (map register-dispatcher key-label->re-pressed-key)))

;; Registers handler for with re-pressed's events
(rf/reg-event-fx
  ::register-key-dispatchers
  (fn [_cofx _evt]
    (register-dispatchers)
    {}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls->controls by key
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn controls->by-key
  "Converts controls from a list of control maps like
  `{:keys #{k1 k2} :event [evt] :id :control-id}`
  into a map of controls by key, like:
  `{k1 [{:keys #{k1 k2} :event [evt] :id ctrl-id}]
    k2 [{:keys #{k1 k2} :event [evt] :id ctrl-id}]}`

  Multiple controls will be registered for keys that share a control,
  so that events can be dispatched to them simultaneously.

  Supports looking up events to dispatch by keys pressed in above handler.
  Called at control registration time.
  "
  [controls]
  (reduce
    (fn [by-key {:keys [keys] :as control}]
      (reduce
        (fn [by-key str-key]
          (let [controls (get by-key str-key)]
            (assoc by-key str-key (conj controls control))))
        by-key
        keys))
    {}
    controls))
