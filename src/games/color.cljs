(ns games.color)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cells
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def green "#92CC41") ;; light green (lime?)
(def red "#FE493C")
(def magenta "#B564D4")
(def light-blue "#6ebff5") ;; sky?
(def blue "#209CEE")
(def yellow "#F7D51D")
(def orange "#E76E55")

(def color->piece-color
  {:green      green
   :red        red
   :blue       blue
   :yellow     yellow
   :light-blue light-blue
   :orange     orange
   :magenta    magenta})

(defn cell->piece-color
  [c]
  (-> c :color (color->piece-color)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Cell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def board-black "#212529")

(defn cell->background
;; [{:keys [x y]}]
[_]
board-black
;; (str "rgba(" (* x 20) ", 100, " (* x 20) ", " (- 1 (/ y 10)) ")")
)
