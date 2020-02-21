(ns games.tetris.subs
  (:require
   [re-frame.core :as rf]))

(defn game-opts->db
  ([db {:keys [name] :as _game-opts}]
   (-> db :games name))
  ([db {:keys [name] :as _game-opts} k]
   (-> db :games name k)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grids
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
