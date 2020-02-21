(ns games.controls.db)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Global controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def global-controls
  [{:id    ::home-nav
    :label "Home"
    :keys  (set ["m" "x"])
    :event [:games.events/set-page :select]}
   {:id    ::controls-nav
    :label "Controls"
    :keys  (set ["c" "?"])
    :event [:games.events/set-page :controls]}
   {:id    ::about-nav
    :label "About"
    :keys  (set ["b"])
    :event [:games.events/set-page :about]}
   {:id    ::tetris-nav
    :label "Play Tetris"
    :keys  (set ["t"])
    :event [:games.events/set-page :tetris]}
   {:id    ::puyo-nav
    :label "Play Puyo-Puyo"
    :keys  (set ["p"])
    :event [:games.events/set-page :puyo]}
   {:id    ::debug-nav
    :label "Debug view"
    :keys  (set ["d"])
    :event [:games.events/set-page :debug]}])

