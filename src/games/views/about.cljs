(ns games.views.about
  (:require
   [games.views.components :refer [widget]]))

(defn about
  []
  [:div
   {:style
    {:text-align "left"
     :font-size  "14px"
     :padding    "3.5rem"}}
   [:h3
    {:style
     {:margin-bottom "2rem"}}
    "About"]
   [:p
    "The tetris clone was initially created for itch.io's "
    [:a {:href "https://itch.io/jam/finally-finish-something-2020"}
     "Finally Finish Something 2020"]
    " Game Jam. Puyo-puyo was added for "
    "itch.io's "
    [:a {:href "https://itch.io/jam/my-first-game-jam-winter-2020"}
     "My First Game Jam - Winter 2020"]
    "."]
   [:p
    "The code is open source and available on github at "
    [:a {:href "https://github.com/russmatney/reframe-games"}
     " russmatney/reframe-games"]
    "."]
   [:p "The engine was built using ClojureScript, Reagent, and Re-Frame."]
   [:p
    "The NES-style graphics come from "
    [:a {:href "https://github.com/nostalgic-css/NES.css"} "NES.css"]
    "."]
   [:p "Thanks for playing!"]
   [:p "Press m to return to the main menu."]])

;; TODO dry this up
(def background-color "#441086")
;;:background "#5d08c7"

(defn page []
  [:div
   {:style
    {:height           "100vh"
     :width            "100vw"
     :display          "flex"
     :background
     (str "linear-gradient(135deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px),
       linear-gradient(225deg, " background-color " 21px, black 22px, black 24px, transparent 24px, transparent 67px, black 67px, black 69px, transparent 69px)0 64px")
     :background-color background-color
     :background-size  "64px 128px"
     :padding          "24px"}}
   [widget
    {:style
     {:width "100%"}}
    ^{:key "child"}
    [about]]])
