(ns games.controls.db)

(def key-label->re-pressed-key
  "Maps a 'nice' string to a re-pressed key with keyCode."
  {"enter" {:keyCode 13}
   "space" {:keyCode 32}
   "left"  {:keyCode 37}
   "right" {:keyCode 39}
   "up"    {:keyCode 38}
   "down"  {:keyCode 40}
   "a"     {:keyCode 65}
   "b"     {:keyCode 66}
   "c"     {:keyCode 67}
   "d"     {:keyCode 68}
   "e"     {:keyCode 69}
   "f"     {:keyCode 70}
   "g"     {:keyCode 71}
   "h"     {:keyCode 72}
   "j"     {:keyCode 74}
   "k"     {:keyCode 75}
   "l"     {:keyCode 76}
   "s"     {:keyCode 83}
   "w"     {:keyCode 87}
   "x"     {:keyCode 88}})

(def supported-keys (set (keys key-label->re-pressed-key)))

(def control->label
  "Maps a control to a human label"
  {:move-left  "Move Left"
   :move-right "Move Right"
   :move-down  "Move Down"
   :rotate     "Rotate"
   :hold       "Hold / Swap"
   :pause      "Pause"
   :controls   "Controls"
   :about      "About"
   :game       "Back to Game"})
