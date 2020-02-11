(ns games.controls.re-pressed
  (:require [re-frame.core :as rf]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; re-pressed helpers
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
   "r" {:keyCode 82}
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
   "\"" {:keyCode 222}})

(def supported-keys (set (keys key-label->re-pressed-key)))

(defn mk-control-event-name [id]
  (keyword :games.controls id))

(defn controls->id-events-map
  "Builds a map of :id to a list of events"
  [controls]
  (let [controls-by-id (group-by :id controls)]
    (into {}
          (map (fn [[k ctrls]]
                 [k (map :event ctrls)])
               controls-by-id))))

(defn register-dispatchers
  [id-events-map]
  (doall
    (map
      (fn [[id events]]
        (rf/reg-event-fx
          (mk-control-event-name id)
          (fn [_cofx _evt]
            {:dispatch-n (distinct events)})))
      id-events-map)))

(defn controls->rp-event-keys
  "Converts the passed controls-db into a
  re-pressed `[[::event][kbd1][kbd2]]` list.
  TODO join `keys` from same :id events
  "
  [controls]
  (into
    []
    (map
      (fn [{:keys [id event keys]}]
        (into
          []
          (cons
            [(mk-control-event-name id)] ;; using id as event name
            (map (fn [k]
                   (when-not k
                     (print "Alert! Unsupported key passed for event " event))
                   [(key-label->re-pressed-key k)])
                 keys))))
      controls)))

(defn controls->rp-all-keys
  "Converts the passed controls-db into a
  re-pressed `[[kbd1][kbd2]]` list.
  "
  [controls]
  (into []
        (mapcat
          (fn [{:keys [keys]}]
            (map key-label->re-pressed-key keys))
          controls)))
