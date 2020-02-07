(ns games.puyo.views.about
  (:require
   [games.views.components :refer [widget]]))

(defn about
  []
  [:div
   {:style
    {:padding "5.5rem"}}
   [:h3
    {:style
     {:margin-bottom "2rem"}}
    "About"]
   [:p
    "This game was created for itch.io's "
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
   [:p "Press g to return to the game."]])

(defn view []
  [widget
   {:style
    {:width "100%"}}
   ^{:key "child"}
   [about]])