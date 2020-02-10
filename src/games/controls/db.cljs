(ns games.controls.db
  (:require
   [games.grid.core :as grid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; controls->re-pressed helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; TODO move to re-pressed namespace?
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def global-controls
  {:main     {:label "Main Menu"
              :keys  (set ["m" "x"])
              :event [:games.events/unset-view]}
   :controls {:label "Controls"
              :keys  (set ["c" "?"])
              :event [:games.events/set-view :controls]}
   :about    {:label "About"
              :keys  (set ["b"])
              :event [:games.events/set-view :about]}
   :tetris   {:label "Play Tetris"
              :keys  (set ["t"])
              :event [:games.events/set-view :tetris]}
   :puyo     {:label "Play Puyo-Puyo"
              :keys  (set ["p"])
              :event [:games.events/set-view :puyo]}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls Game DB
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def db-defaults
  {:game-grid {:entry-cell      {:x 0 :y 0}
               :height          5
               :width           5
               :phantom-columns 4
               :phantom-rows    4}})

(defn controls-game-controls
  "heh."
  [game-opts]
  {:add-piece  {:label "Add Piece"
                :keys  (set ["space"])
                :event [:games.controls.events/add-piece game-opts]}
   :move-left  {:label "Move Left"
                :keys  (set ["left" "h" "a"])
                :event [:games.controls.events/move-piece game-opts :left]}
   :move-down  {:label "Move Down"
                :keys  (set ["down" "j" "s"])
                :event [:games.controls.events/move-piece game-opts :down]}
   :move-right {:label "Move Right"
                :keys  (set ["right" "l" "d"])
                :event [:games.controls.events/move-piece game-opts :right]}
   :move-up    {:label "Move Up"
                :keys  (set ["up" "j" "s"])
                :event [:games.controls.events/move-piece game-opts :up]}
   :rotate     {:label "Rotate"
                :keys  (set ["up" "k" "w"])
                :event [:games.controls.events/rotate-piece game-opts]}})

(defn initial-db
  ([] (initial-db {}))
  ([game-opts]
   (let [{:keys [name game-grid] :as game-opts}
         (merge db-defaults game-opts)]
     {:name      name
      :game-opts game-opts
      :game-grid (grid/build-grid game-grid)
      :controls  (controls-game-controls game-opts)})))
