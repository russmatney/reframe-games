(ns games.events
  (:require
   [re-frame.core :as rf]
   [games.events.interceptors :refer [game-db-interceptor]]
   [games.controls.events :as controls.events]
   [games.db :as db]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Init
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::init
  (fn [_]
    {:db db/initial-db
     :dispatch-n
     [[::start-games]
      [::controls.events/init]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Start Games
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; implies games have page knowledge (`pages`)
;; maybe the pages should call out their named-games?
(defn for-page?
  [page {:keys [game-opts] :as _game-db}]
  (contains? (:pages game-opts) page))

(defn games-for-page
  [db page]
  (let [games (-> db :games vals)]
    (filter #(for-page? page %)
            games)))

(defn games-not-for-page
  [db page]
  (let [games (-> db :games vals)]
    (remove #(for-page? page %)
            games)))

(rf/reg-event-fx
  ::start-games
  (fn [{:keys [db]}]
    (let [page        (:current-page db)
          start-games (games-for-page db page)
          stop-games  (games-not-for-page db page)]
      {:db (assoc db :active-games (map :name start-games))
       :dispatch-n
       (concat
         (map (fn [game]
                [(:init-event-name game) (:game-opts game)])
              start-games)
         (map (fn [game]
                [(:stop-event-name game) (:game-opts game)])
              stop-games))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
  ::set-page
  (fn [{:keys [db]} [_ page]]
    {:db       (assoc db :current-page page)
     :dispatch [::start-games]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Per game start, stop, restart, step, pause events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn opts->timer-id
  [opts]
  (keyword (namespace ::x)
           (str (name :timer) "-" (name (:name opts)))))

(defn n->step-id
  [n]
  (keyword n (str (name :step))))

(defn opts-n->timeouts
  [opts n]
  [{:id (n->step-id n)}
   {:id (opts->timer-id opts)}])

(defn reg-game-events
  "Registers general game events:

  - ::init-game
  - ::stop
  - ::restart
  - ::step
  - ::pause
  - ::resume
  - ::toggle-pause
  - ::timer

  NOTE Some of these event names are baked into default db events (init, stop),
  or are otherwise called from controls/ui dispatches (restart, pause, resume).

  Handles a few keys on game-dbs:

  - :time - time passed since start
  - :time-increment - how often the timer should be incremented
  - :step-timeout - the time between game-steps, often decreased as levels
  increase (in falling-block games like tetris/puyo).
  - :paused? - whether or not the game is paused
  - :gameover? - read-only. if the game is over, the ::step event will kill the
  running timers
  "
  ([] (reg-game-events {}))
  ([{:keys [n step-fn]}]
   (let [init-evt         (keyword n :init-game)
         stop-evt         (keyword n :stop)
         restart-evt      (keyword n :restart)
         pause-evt        (keyword n :pause)
         resume-evt       (keyword n :resume)
         toggle-pause-evt (keyword n :toggle-pause)
         timer-evt        (keyword n :timer)]

     (rf/reg-event-fx
       init-evt
       [(game-db-interceptor)]
       (fn [_cofx game-opts]
         {:dispatch-n
          [[::controls.events/register-controls game-opts]
           [(n->step-id n) game-opts]
           [timer-evt game-opts]]}))


     (rf/reg-event-fx
       stop-evt
       [(game-db-interceptor)]
       (fn [_cofx game-opts]
         {:dispatch-n
          [[::controls.events/deregister-controls game-opts]]
          :clear-timeouts (opts-n->timeouts game-opts n)}))

     (rf/reg-event-fx
       restart-evt
       [(game-db-interceptor)]
       (fn [_cofx game-opts]
         {:db (-> db/initial-db :games (get (:name game-opts)))
          :dispatch-n
          [[(n->step-id n) game-opts]
           [timer-evt game-opts]]}))

     (when step-fn
       (rf/reg-event-fx
         (n->step-id n)
         [(game-db-interceptor)]
         (fn [{:keys [db]} game-opts]
           (let [{:keys [step-timeout] :as db}
                 (step-fn db game-opts)]
             (if (:gameover? db)
               {:db db
                :clear-timeouts
                (opts-n->timeouts game-opts n)}
               {:db db
                :timeout
                {:id    (n->step-id n)
                 :event [(n->step-id n) game-opts]
                 :time  step-timeout}})))))

     (rf/reg-event-fx
       pause-evt
       [(game-db-interceptor)]
       (fn [{:keys [db]} game-opts]
         {:db             (assoc db :paused? true)
          :clear-timeouts (opts-n->timeouts game-opts n)}))

     (rf/reg-event-fx
       resume-evt
       [(game-db-interceptor)]
       (fn [{:keys [db]} game-opts]
         (let [updated-db (assoc db :paused? false)]
           {:db updated-db
            :dispatch-n
            [[(n->step-id n) game-opts]
             [timer-evt game-opts]]})))

     (rf/reg-event-fx
       toggle-pause-evt
       [(game-db-interceptor)]
       (fn [{:keys [db]} game-opts]
         (if-not (:gameover? db)
           (if (:paused? db)
             ;; unpause
             {:dispatch [resume-evt game-opts]}
             ;; pause
             {:dispatch [pause-evt game-opts]}))))

     (rf/reg-event-fx
       timer-evt
       [(game-db-interceptor)]
       (fn [{:keys [db]} game-opts]
         (let [{:keys [timer-increment]} db
               timer-increment           (or timer-increment 400)]
           {:db (update db :time #(+ % timer-increment))
            :timeout
            {:id    (opts->timer-id game-opts)
             :event [timer-evt game-opts]
             :time  timer-increment}}))))))
