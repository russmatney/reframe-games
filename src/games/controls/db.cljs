(ns games.controls.db
  (:require
   [games.grid.core :as grid]))

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
   "\"" {:keyCode 222}

   })

(def supported-keys (set (keys key-label->re-pressed-key)))

;; TODO remove, write smarter descriptions
(def control->label
  "Maps a control to a human label"
  {:move-left  "Move Left"
   :move-right "Move Right"
   :move-down  "Move Down"
   :rotate     "Rotate"
   :hold       "Hold / Swap"
   :pause      "Pause"
   :controls   "Controls"
   :about      "About"})

(def global-controls
  {:main     {:label "Main Menu"
              :keys  (set ["m" "x"])
              :event [:games.events/unset-view]}
   :controls {:label "Controls"
              :keys  (set ["c"]) ;; TODO support '?' here and above
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

(def initial-db
  {:grid
   (grid/build-grid
     {:width      5
      :height     5
      :entry-cell {:x 0 :y 0}})})
