(ns games.views.about
  (:require
   [games.views.components :as components]))

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

(defn page []
  [components/page {}
   [components/widget
    {:style
     {:width  "100%"
      :height "100%"}}
    [about]]])
