(ns games.tetris.subs
  (:require
   [re-frame.core :as rf]
   [games.tetris.db :as tetris.db]
   [games.grid.core :as grid]))


(defn game-opts->db
  ([db {:keys [name] :as _game-opts}]
   (-> db ::tetris.db/db name))
  ([db {:keys [name] :as _game-opts} k]
   (-> db ::tetris.db/db name k)))

(rf/reg-sub
  ::tetris-db
  (fn [db evt]
    (case (count evt)
      1 (-> db ::tetris.db/db :default)
      2 (let [[_e n] evt] (-> db ::tetris.db/db (get n))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grids
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(rf/reg-sub
::game-grid
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :game-grid)
      (grid/only-positive-rows))))

(rf/reg-sub
::preview-grids
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :preview-grids))))

(rf/reg-sub
::held-grid
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :held-grid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
::paused?
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :paused?))))

(rf/reg-sub
::gameover?
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :gameover?))))

(rf/reg-sub
::any-held?
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :held-shape-fn))))

(rf/reg-sub
::score
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :score))))

(rf/reg-sub
::time
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :time))))

(rf/reg-sub
::level
(fn [db [_ game-opts]]
  (-> db
      (game-opts->db game-opts :level))))
