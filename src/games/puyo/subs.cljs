(ns games.puyo.subs
  (:require
   [re-frame.core :as rf]
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]))

(defn ->puyo-db
  ([db n]
   (-> db ::puyo.db/db n))
  ([db n k]
   (-> db ::puyo.db/db n k)
   ))

(rf/reg-sub
  ::puyo-db
  (fn [db evt]
    ;; is there a multi-arity subscription helper?
    (case (count evt)
      2 (let [[_ k] evt] (-> db ::puyo.db/db :default k))
      3 (let [[_ n k] evt] (-> db ::puyo.db/db n k)))))

(rf/reg-sub
  ::current-view
  (fn [db [_ n]]
    (or (->puyo-db db n :current-view)
        :game)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Grids
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::game-grid
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :game-grid)
        (grid/only-positive-rows))))

(rf/reg-sub
  ::preview-grids
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :preview-grids))))

(rf/reg-sub
  ::held-grid
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :held-grid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Logic
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::paused?
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :paused?))))

(rf/reg-sub
  ::gameover?
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :gameover?))))

(rf/reg-sub
  ::any-held?
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :held-shape-fn))))

(rf/reg-sub
  ::score
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :score))))

(rf/reg-sub
  ::time
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :time))))

(rf/reg-sub
  ::level
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :level))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Controls
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
  ::controls
  (fn [db [_ n]]
    (-> db
        (->puyo-db n :controls))))

(rf/reg-sub
  ::keys-for
  (fn [db [_ n for]]
    (-> db
        (->puyo-db n :controls)
        for
        :keys)))

(rf/reg-sub
  ::event-for
  (fn [db [_ n for]]
    (-> db
        (->puyo-db n :controls)
        for
        :event)))
