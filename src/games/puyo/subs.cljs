(ns games.puyo.subs
  (:require
   [re-frame.core :as rf]
   [games.puyo.db :as puyo.db]
   [games.grid.core :as grid]))

(rf/reg-sub
  ::puyo-db
  (fn [db evt]
    (case (count evt)
      1 (-> db ::puyo.db/db :default)
      2 (let [[_e n] evt] (-> db ::puyo.db/db (get n)))
      3 (let [[_e n k] evt] (-> db ::puyo.db/db (get n) (get k))))))


(defn game-opts->db
  ([db {:keys [name] :as _game-opts}]
   (-> db ::puyo.db/db name))
  ([db {:keys [name] :as _game-opts} k]
   (-> db ::puyo.db/db name k)))

;; TODO get this over the finish line
(defn ->subs [subs]
  (for [[sub-name db-key] subs]
    (let [db-key (if (map? db-key)
                   (:db-key db-key)
                   db-key)
          after  (when (map? db-key)
                   (:after db-key))]

      (rf/reg-sub
        sub-name
        (fn [db [_ game-opts]]
          (cond-> (game-opts->db db game-opts db-key)
            after (after)))))))

;; (->subs
;;   {::game-grid     {:db-key :game-grid
;;                     :after  grid/only-positive-rows}
;;    ::preview-grids :preview-grid
;;    ::held-grids    :held-grid})

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
